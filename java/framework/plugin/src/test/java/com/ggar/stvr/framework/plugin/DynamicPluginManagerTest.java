package com.ggar.stvr.framework.plugin;

import com.ggar.stvr.framework.plugin.event.PluginEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.test.StepVerifier;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DynamicPluginManagerTest {

    @TempDir
    Path tempDir;

    private DynamicPluginManager manager;
    private PluginContext context;

    public interface TestService {
        String ping();
    }

    @BeforeEach
    void setUp() {
        context = PluginContext.of(Map.of("test.property", "hello-world"));
        manager = new DynamicPluginManager(context);
    }

    @Test
    void shouldLoadPluginFromJarAndDiscoverExtensions() throws Exception {
        Path jarPath = createSamplePluginJar("sample-plugin", "SamplePluginImpl", "1.0.0");

        StepVerifier.create(manager.events())
                .then(() -> {
                    StvrPlugin plugin = manager.loadPlugin(jarPath);
                    assertThat(plugin.getId()).isEqualTo("sample-plugin");
                    assertThat(plugin.getVersion()).isEqualTo("1.0.0");
                })
                .assertNext(event -> {
                    assertThat(event).isInstanceOf(PluginEvent.PluginLoadedEvent.class);
                    assertThat(event.pluginId()).isEqualTo("sample-plugin");
                })
                .thenCancel()
                .verify();

        assertThat(manager.getLoadedPlugins()).hasSize(1);
        assertThat(manager.getPlugin("sample-plugin")).isPresent();

        List<TestService> services = manager.getExtensions(TestService.class);
        assertThat(services).hasSize(1);
        assertThat(services.get(0).ping()).isEqualTo("pong from sample-plugin");
    }

    @Test
    void shouldUnloadPluginSuccessfully() throws Exception {
        Path jarPath = createSamplePluginJar("sample-plugin", "SamplePluginImpl", "1.0.0");
        manager.loadPlugin(jarPath);
        assertThat(manager.getLoadedPlugins()).hasSize(1);

        StepVerifier.create(manager.events())
                .then(() -> {
                    boolean unloaded = manager.unloadPlugin("sample-plugin");
                    assertThat(unloaded).isTrue();
                })
                .assertNext(event -> {
                    assertThat(event).isInstanceOf(PluginEvent.PluginUnloadedEvent.class);
                    assertThat(event.pluginId()).isEqualTo("sample-plugin");
                })
                .thenCancel()
                .verify();

        assertThat(manager.getLoadedPlugins()).isEmpty();
        assertThat(manager.getPlugin("sample-plugin")).isEmpty();
        assertThat(manager.getExtensions(TestService.class)).isEmpty();

        // Unloading non-existent plugin returns false
        assertThat(manager.unloadPlugin("sample-plugin")).isFalse();
    }

    @Test
    void shouldReloadPluginSuccessfully() throws Exception {
        Path jarPath = createSamplePluginJar("sample-plugin", "SamplePluginImpl", "1.0.0");
        manager.loadPlugin(jarPath);

        StepVerifier.create(manager.events())
                .then(() -> {
                    StvrPlugin reloaded = manager.reloadPlugin("sample-plugin");
                    assertThat(reloaded).isNotNull();
                    assertThat(reloaded.getId()).isEqualTo("sample-plugin");
                })
                .assertNext(event -> {
                    assertThat(event).isInstanceOf(PluginEvent.PluginUnloadedEvent.class);
                    assertThat(event.pluginId()).isEqualTo("sample-plugin");
                })
                .assertNext(event -> {
                    assertThat(event).isInstanceOf(PluginEvent.PluginLoadedEvent.class);
                    assertThat(event.pluginId()).isEqualTo("sample-plugin");
                })
                .assertNext(event -> {
                    assertThat(event).isInstanceOf(PluginEvent.PluginReloadedEvent.class);
                    assertThat(event.pluginId()).isEqualTo("sample-plugin");
                })
                .thenCancel()
                .verify();
    }

    @Test
    void shouldLoadPluginsFromDirectory() throws Exception {
        createSamplePluginJar("plugin-a", "PluginAImpl", "1.0.0");
        createSamplePluginJar("plugin-b", "PluginBImpl", "2.0.0");

        List<StvrPlugin> plugins = manager.loadPluginsFromDirectory(tempDir);
        assertThat(plugins).hasSize(2);

        List<TestService> services = manager.getExtensions(TestService.class);
        assertThat(services).hasSize(2);
    }

    @Test
    void shouldThrowExceptionWhenJarHasNoServiceLoaderDescriptor() throws Exception {
        Path invalidJar = tempDir.resolve("empty.jar");
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(invalidJar.toFile()))) {
            jos.putNextEntry(new JarEntry("test.txt"));
            jos.write("hello".getBytes());
            jos.closeEntry();
        }

        assertThatThrownBy(() -> manager.loadPlugin(invalidJar))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("No StvrPlugin implementation found");
    }

    @Test
    void shouldThrowExceptionWhenDuplicatePluginLoaded() throws Exception {
        Path jarPath = createSamplePluginJar("dup-plugin", "DupPluginImpl", "1.0.0");
        manager.loadPlugin(jarPath);

        assertThatThrownBy(() -> manager.loadPlugin(jarPath))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("already loaded");
    }

    @Test
    void shouldThrowExceptionWhenJarDoesNotExist() {
        Path missing = tempDir.resolve("non-existent.jar");
        assertThatThrownBy(() -> manager.loadPlugin(missing))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("does not exist");
    }

    private Path createSamplePluginJar(String pluginId, String className, String version) throws Exception {
        String fullClassName = "test.plugin." + className;
        String sourceCode = """
                package test.plugin;
                
                import com.ggar.stvr.framework.plugin.StvrPlugin;
                import com.ggar.stvr.framework.plugin.PluginContext;
                import com.ggar.stvr.framework.plugin.DynamicPluginManagerTest.TestService;
                import java.util.List;
                
                public class %s implements StvrPlugin, TestService {
                    private boolean initialized = false;
                    private boolean destroyed = false;
                
                    @Override
                    public String getId() { return "%s"; }
                
                    @Override
                    public String getVersion() { return "%s"; }
                
                    @Override
                    public void initialize(PluginContext context) {
                        this.initialized = true;
                    }
                
                    @Override
                    public void destroy() {
                        this.destroyed = true;
                    }
                
                    @Override
                    public String ping() {
                        return "pong from %s";
                    }
                
                    @Override
                    @SuppressWarnings("unchecked")
                    public <T> List<T> getExtensions(Class<T> extensionPoint) {
                        if (extensionPoint.isInstance(this)) {
                            return (List<T>) List.of(this);
                        }
                        return List.of();
                    }
                }
                """.formatted(className, pluginId, version, pluginId);

        Path srcFile = tempDir.resolve(className + ".java");
        Files.writeString(srcFile, sourceCode);

        Path classesDir = tempDir.resolve("classes_" + className);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        ByteArrayOutputStream errStream = new ByteArrayOutputStream();
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        String pluginClasspath = StvrPlugin.class.getProtectionDomain().getCodeSource().getLocation().getPath()
                + System.getProperty("path.separator")
                + DynamicPluginManagerTest.class.getProtectionDomain().getCodeSource().getLocation().getPath();

        int compileResult = compiler.run(null, outStream, errStream,
                "-cp", pluginClasspath,
                "-d", classesDir.toString(),
                srcFile.toString());

        assertThat(compileResult)
                .withFailMessage("Compilation failed:\n%s\n%s", outStream.toString(), errStream.toString())
                .isZero();

        Path jarFile = tempDir.resolve(pluginId + ".jar");
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile.toFile()))) {
            // Write META-INF/services/com.ggar.stvr.framework.plugin.StvrPlugin
            jos.putNextEntry(new JarEntry("META-INF/services/com.ggar.stvr.framework.plugin.StvrPlugin"));
            jos.write((fullClassName + "\n").getBytes());
            jos.closeEntry();

            // Write compiled classes
            try (var stream = Files.walk(classesDir)) {
                for (Path p : (Iterable<Path>) stream::iterator) {
                    if (Files.isRegularFile(p)) {
                        String relPath = classesDir.relativize(p).toString().replace("\\", "/");
                        jos.putNextEntry(new JarEntry(relPath));
                        jos.write(Files.readAllBytes(p));
                        jos.closeEntry();
                    }
                }
            }
        }

        return jarFile;
    }
}
