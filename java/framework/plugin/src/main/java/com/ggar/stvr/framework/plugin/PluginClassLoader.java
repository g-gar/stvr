package com.ggar.stvr.framework.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Isolated ClassLoader for loading dynamic STVR plugin JARs.
 * Uses standard parent-first delegation so host-provided runtime classes
 * (SLF4J, Project Reactor, Framework interfaces, SPI contracts)
 * are shared seamlessly, while plugin-specific classes and embedded libraries
 * remain isolated to this ClassLoader instance.
 */
public class PluginClassLoader extends URLClassLoader {

    private static final Logger log = LoggerFactory.getLogger(PluginClassLoader.class);

    private final Path jarPath;

    public PluginClassLoader(Path jarPath, ClassLoader parent) throws IOException {
        super(new URL[]{Objects.requireNonNull(jarPath, "jarPath cannot be null").toUri().toURL()},
                parent != null ? parent : ClassLoader.getSystemClassLoader());
        this.jarPath = jarPath;
    }

    public Path getJarPath() {
        return jarPath;
    }

    @Override
    public void close() throws IOException {
        log.debug("Closing PluginClassLoader for JAR: {}", jarPath);
        super.close();
    }
}
