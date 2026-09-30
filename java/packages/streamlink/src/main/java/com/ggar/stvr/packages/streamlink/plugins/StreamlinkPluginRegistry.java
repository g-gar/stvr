package com.ggar.stvr.packages.streamlink.plugins;

import com.ggar.stvr.packages.streamlink.i18n.StreamlinkMessages;
import com.ggar.stvr.packages.streamlink.plugins.generic.GenericPlugin;
import com.ggar.stvr.packages.streamlink.plugins.kick.KickPlugin;
import com.ggar.stvr.packages.streamlink.plugins.tiktok.TikTokPlugin;
import com.ggar.stvr.packages.streamlink.plugins.twitch.TwitchPlugin;
import com.ggar.stvr.packages.streamlink.plugins.youtube.YouTubePlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Registry and Strategy Factory for Streamlink plugins.
 * Manages plugin discovery, URL matching, and platform-specific execution delegation.
 */
public class StreamlinkPluginRegistry {

    private final List<StreamlinkPlugin> plugins = new ArrayList<>();
    private final StreamlinkPlugin fallbackPlugin;

    public StreamlinkPluginRegistry() {
        this(new GenericPlugin());
    }

    public StreamlinkPluginRegistry(StreamlinkPlugin fallbackPlugin) {
        this.fallbackPlugin = Objects.requireNonNull(fallbackPlugin,
                StreamlinkMessages.get("error.null_arg", "fallbackPlugin"));
    }

    /**
     * Creates a default registry populated with standard platform plugin strategies.
     */
    public static StreamlinkPluginRegistry defaultRegistry() {
        StreamlinkPluginRegistry registry = new StreamlinkPluginRegistry();
        registry.register(new TwitchPlugin());
        registry.register(new KickPlugin());
        registry.register(new YouTubePlugin());
        registry.register(new TikTokPlugin());
        return registry;
    }

    /**
     * Registers a new plugin strategy with high priority.
     */
    public void register(StreamlinkPlugin plugin) {
        if (plugin != null) {
            plugins.add(0, plugin);
        }
    }

    /**
     * Resolves the matching plugin strategy by inspecting the target URL against each plugin's patterns.
     *
     * @param url target stream URL
     * @return matching StreamlinkPlugin strategy, or fallback
     */
    public StreamlinkPlugin findForUrl(String url) {
        if (url != null && !url.isBlank()) {
            for (StreamlinkPlugin plugin : plugins) {
                if (plugin.supportsUrl(url)) {
                    return plugin;
                }
            }
        }
        return fallbackPlugin;
    }

    /**
     * Resolves the matching plugin strategy for the given plugin name and/or target URL.
     *
     * @param pluginName name of the plugin from JSON (may be null)
     * @param targetUrl target stream URL (may be null)
     * @return matching StreamlinkPlugin strategy, or fallback
     */
    public StreamlinkPlugin find(String pluginName, String targetUrl) {
        for (StreamlinkPlugin plugin : plugins) {
            if ((pluginName != null && plugin.supportsPluginName(pluginName))
                    || (targetUrl != null && plugin.supportsUrl(targetUrl))) {
                return plugin;
            }
        }
        return fallbackPlugin;
    }
}
