package com.ggar.stvr.framework.plugin;

import com.ggar.stvr.framework.plugin.event.PluginEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Default dynamic plugin manager implementation.
 * Manages loading, isolated ClassLoaders, lifecycle hooks, and extension discovery.
 */
public class DynamicPluginManager implements PluginManager {

    private static final Logger log = LoggerFactory.getLogger(DynamicPluginManager.class);

    private final PluginContext defaultContext;
    private final Map<String, PluginHolder> plugins = new ConcurrentHashMap<>();
    private final Sinks.Many<PluginEvent> eventSink = Sinks.many().multicast().directBestEffort();

    public DynamicPluginManager() {
        this(PluginContext.empty());
    }

    public DynamicPluginManager(PluginContext defaultContext) {
        this.defaultContext = Objects.requireNonNull(defaultContext, "defaultContext cannot be null");
    }

    @Override
    public StvrPlugin loadPlugin(Path jarPath) {
        Objects.requireNonNull(jarPath, "jarPath cannot be null");
        Path normalizedPath = jarPath.toAbsolutePath().normalize();

        if (!Files.exists(normalizedPath)) {
            throw new PluginException("Plugin JAR file does not exist: " + normalizedPath);
        }
        if (!Files.isRegularFile(normalizedPath)) {
            throw new PluginException("Plugin path is not a file: " + normalizedPath);
        }

        PluginClassLoader classLoader = null;
        try {
            classLoader = new PluginClassLoader(normalizedPath, getClass().getClassLoader());
            ServiceLoader<StvrPlugin> serviceLoader = ServiceLoader.load(StvrPlugin.class, classLoader);
            Iterator<StvrPlugin> iterator = serviceLoader.iterator();

            if (!iterator.hasNext()) {
                classLoader.close();
                throw new PluginException("No StvrPlugin implementation found via ServiceLoader in JAR: " + normalizedPath);
            }

            StvrPlugin plugin = iterator.next();
            String id = Objects.requireNonNull(plugin.getId(), "Plugin ID cannot be null");

            if (plugins.containsKey(id)) {
                classLoader.close();
                throw new PluginException("Plugin with ID '" + id + "' is already loaded");
            }

            log.info("Initializing plugin '{}' v{} from {}", plugin.getId(), plugin.getVersion(), normalizedPath.getFileName());
            plugin.initialize(defaultContext);

            PluginHolder holder = new PluginHolder(plugin, classLoader, normalizedPath);
            plugins.put(id, holder);

            eventSink.tryEmitNext(new PluginEvent.PluginLoadedEvent(plugin));
            return plugin;

        } catch (PluginException e) {
            throw e;
        } catch (Exception e) {
            if (classLoader != null) {
                try {
                    classLoader.close();
                } catch (IOException ignored) {
                }
            }
            throw new PluginException("Failed to load plugin from JAR: " + normalizedPath, e);
        }
    }

    @Override
    public boolean unloadPlugin(String pluginId) {
        Objects.requireNonNull(pluginId, "pluginId cannot be null");
        PluginHolder holder = plugins.remove(pluginId);
        if (holder == null) {
            log.warn("Attempted to unload unknown plugin: {}", pluginId);
            return false;
        }

        log.info("Destroying and unloading plugin '{}'", pluginId);
        try {
            holder.plugin().destroy();
        } catch (Exception e) {
            log.error("Error invoking destroy() on plugin '{}'", pluginId, e);
        }

        try {
            holder.classLoader().close();
        } catch (IOException e) {
            log.error("Error closing ClassLoader for plugin '{}'", pluginId, e);
        }

        eventSink.tryEmitNext(new PluginEvent.PluginUnloadedEvent(pluginId));
        return true;
    }

    @Override
    public StvrPlugin reloadPlugin(String pluginId) {
        Objects.requireNonNull(pluginId, "pluginId cannot be null");
        PluginHolder holder = plugins.get(pluginId);
        if (holder == null) {
            throw new PluginException("Cannot reload non-loaded plugin: " + pluginId);
        }

        Path jarPath = holder.jarPath();
        unloadPlugin(pluginId);
        StvrPlugin reloaded = loadPlugin(jarPath);
        eventSink.tryEmitNext(new PluginEvent.PluginReloadedEvent(reloaded));
        return reloaded;
    }

    @Override
    public List<StvrPlugin> loadPluginsFromDirectory(Path directory) {
        Objects.requireNonNull(directory, "directory cannot be null");
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            log.debug("Plugins directory does not exist: {}", directory);
            return Collections.emptyList();
        }

        List<StvrPlugin> loaded = new ArrayList<>();
        try (Stream<Path> stream = Files.list(directory)) {
            List<Path> jarFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".jar"))
                    .sorted()
                    .toList();

            for (Path jar : jarFiles) {
                try {
                    loaded.add(loadPlugin(jar));
                } catch (Exception e) {
                    log.error("Failed to load plugin from {}", jar, e);
                }
            }
        } catch (IOException e) {
            throw new PluginException("Failed to scan plugins directory: " + directory, e);
        }

        return Collections.unmodifiableList(loaded);
    }

    @Override
    public Optional<StvrPlugin> getPlugin(String pluginId) {
        PluginHolder holder = plugins.get(pluginId);
        return holder != null ? Optional.of(holder.plugin()) : Optional.empty();
    }

    @Override
    public List<StvrPlugin> getLoadedPlugins() {
        return plugins.values().stream()
                .map(PluginHolder::plugin)
                .toList();
    }

    @Override
    public <T> List<T> getExtensions(Class<T> extensionPoint) {
        Objects.requireNonNull(extensionPoint, "extensionPoint cannot be null");
        return plugins.values().stream()
                .map(PluginHolder::plugin)
                .flatMap(plugin -> plugin.getExtensions(extensionPoint).stream())
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public Flux<PluginEvent> events() {
        return eventSink.asFlux();
    }

    private record PluginHolder(
            StvrPlugin plugin,
            PluginClassLoader classLoader,
            Path jarPath
    ) {
    }
}
