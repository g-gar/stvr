package com.ggar.stvr.framework.plugin.event;

import com.ggar.stvr.framework.plugin.StvrPlugin;

/**
 * Marker interface for all dynamic plugin lifecycle events emitted by PluginManager.
 */
public sealed interface PluginEvent
        permits PluginEvent.PluginLoadedEvent, PluginEvent.PluginUnloadedEvent, PluginEvent.PluginReloadedEvent {

    String pluginId();

    record PluginLoadedEvent(StvrPlugin plugin) implements PluginEvent {
        @Override
        public String pluginId() {
            return plugin.getId();
        }
    }

    record PluginUnloadedEvent(String pluginId) implements PluginEvent {
    }

    record PluginReloadedEvent(StvrPlugin plugin) implements PluginEvent {
        @Override
        public String pluginId() {
            return plugin.getId();
        }
    }
}
