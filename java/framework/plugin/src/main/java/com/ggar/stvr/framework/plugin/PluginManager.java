package com.ggar.stvr.framework.plugin;

import com.ggar.stvr.framework.plugin.event.PluginEvent;
import reactor.core.publisher.Flux;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Service contract for managing the lifecycle of dynamic STVR plugins.
 */
public interface PluginManager {

    /**
     * Loads a plugin from a JAR file path.
     *
     * @param jarPath Path to the plugin JAR file.
     * @return The loaded StvrPlugin instance.
     */
    StvrPlugin loadPlugin(Path jarPath);

    /**
     * Unloads a plugin by its technical ID, invoking its destroy() lifecycle and closing its ClassLoader.
     *
     * @param pluginId Technical ID of the plugin to unload.
     * @return true if the plugin was found and unloaded, false otherwise.
     */
    boolean unloadPlugin(String pluginId);

    /**
     * Reloads an existing plugin from its original JAR path.
     *
     * @param pluginId Technical ID of the plugin to reload.
     * @return The newly reloaded StvrPlugin instance.
     */
    StvrPlugin reloadPlugin(String pluginId);

    /**
     * Scans a directory for all .jar files and loads each plugin found.
     *
     * @param directory The plugins directory to scan.
     * @return List of all successfully loaded plugins.
     */
    List<StvrPlugin> loadPluginsFromDirectory(Path directory);

    /**
     * Gets a loaded plugin by its technical ID.
     */
    Optional<StvrPlugin> getPlugin(String pluginId);

    /**
     * Gets all currently loaded plugins.
     */
    List<StvrPlugin> getLoadedPlugins();

    /**
     * Queries all loaded plugins for extensions implementing the requested extension point.
     *
     * @param extensionPoint The target SPI class.
     * @param <T> Type of the extension.
     * @return Aggregated list of all extensions found across all active plugins.
     */
    <T> List<T> getExtensions(Class<T> extensionPoint);

    /**
     * Reactive stream of plugin lifecycle events (loaded, unloaded, reloaded).
     */
    Flux<PluginEvent> events();
}
