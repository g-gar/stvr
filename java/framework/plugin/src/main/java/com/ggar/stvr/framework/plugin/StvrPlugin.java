package com.ggar.stvr.framework.plugin;

import java.util.Collections;
import java.util.List;

/**
 * Root extension SPI interface for dynamic STVR plugins.
 * Plugin JARs expose implementations via Java ServiceLoader
 * (e.g., META-INF/services/com.ggar.stvr.framework.plugin.StvrPlugin).
 */
public interface StvrPlugin {

    /**
     * Unique technical identifier for the plugin (e.g. "streamlink", "ytdlp").
     */
    String getId();

    /**
     * Human-readable display name (e.g. "Streamlink Adapter Plugin").
     */
    default String getName() {
        return getId();
    }

    /**
     * Semantic version of the plugin (e.g. "1.0.0").
     */
    default String getVersion() {
        return "1.0.0";
    }

    /**
     * Human-readable description of what this plugin provides.
     */
    default String getDescription() {
        return "";
    }

    /**
     * Lifecycle hook called immediately after the plugin is loaded into the host environment.
     * Allows initializing resources, establishing connections, or parsing configuration.
     *
     * @param context Host-provided context containing properties and shared services.
     */
    default void initialize(PluginContext context) {}

    /**
     * Lifecycle hook called before the plugin is unloaded or reloaded.
     * Implementations must close all opened sockets, cancel running tasks, and release resources.
     */
    default void destroy() {}

    /**
     * Obtains extensions provided by this plugin for a specific extension point class.
     * Allows the plugin framework to remain completely decoupled from specific domain SPIs.
     *
     * @param extensionPoint The target SPI interface (e.g. ChannelResolver.class, InspectionPlugin.class).
     * @param <T> Type of the extension point.
     * @return List of extensions implemented by this plugin, or empty list if none provided.
     */
    default <T> List<T> getExtensions(Class<T> extensionPoint) {
        return Collections.emptyList();
    }
}
