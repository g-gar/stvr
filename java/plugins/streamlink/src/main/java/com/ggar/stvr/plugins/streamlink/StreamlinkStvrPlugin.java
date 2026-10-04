package com.ggar.stvr.plugins.streamlink;

import com.ggar.stvr.framework.plugin.PluginContext;
import com.ggar.stvr.framework.plugin.StvrPlugin;
import com.ggar.stvr.inspection.api.InspectionPlugin;
import com.ggar.stvr.inspection.api.resolver.ChannelResolver;
import com.ggar.stvr.packages.streamlink.StreamlinkClient;
import com.ggar.stvr.packages.streamlink.executor.CommandExecutor;
import com.ggar.stvr.packages.streamlink.executor.LocalProcessExecutor;
import com.ggar.stvr.plugins.streamlink.sidecar.StreamlinkSidecarCommandExecutor;
import com.ggar.stvr.streaming.api.LiveStreamProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Root dynamic plugin entry point for Streamlink integration in STVR.
 * Provides StreamlinkChannelResolver, StreamlinkInspectionPlugin, and StreamlinkLiveStreamProvider to the host application.
 */
public class StreamlinkStvrPlugin implements StvrPlugin {

    public static final String PLUGIN_ID = "streamlink";

    private StreamlinkClient client;
    private StreamlinkChannelResolver channelResolver;
    private StreamlinkInspectionPlugin inspectionPlugin;
    private StreamlinkLiveStreamProvider liveStreamProvider;

    @Override
    public String getId() {
        return PLUGIN_ID;
    }

    @Override
    public String getName() {
        return "Streamlink Adapter Plugin";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Adapts Streamlink CLI for channel identity resolution, live stream inspection, and video streaming.";
    }

    @Override
    public void initialize(PluginContext context) {
        String sidecarUrl = context != null ? context.getProperty("stvr.streamlink.sidecar.url", null) : null;
        CommandExecutor executor = (sidecarUrl != null && !sidecarUrl.isBlank())
                ? new StreamlinkSidecarCommandExecutor(sidecarUrl)
                : new LocalProcessExecutor();
        this.client = new StreamlinkClient(executor);
        this.channelResolver = new StreamlinkChannelResolver(this.client);
        this.inspectionPlugin = new StreamlinkInspectionPlugin(this.client);
        this.liveStreamProvider = new StreamlinkLiveStreamProvider(this.client);
    }

    @Override
    public void destroy() {
        // No persistent background daemon to kill for LocalProcessExecutor
        this.channelResolver = null;
        this.inspectionPlugin = null;
        this.liveStreamProvider = null;
        this.client = null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> getExtensions(Class<T> extensionPoint) {
        if (extensionPoint == null) {
            return List.of();
        }

        List<T> extensions = new ArrayList<>();
        if (channelResolver != null && extensionPoint.isAssignableFrom(ChannelResolver.class)) {
            extensions.add((T) channelResolver);
        }
        if (inspectionPlugin != null && extensionPoint.isAssignableFrom(InspectionPlugin.class)) {
            extensions.add((T) inspectionPlugin);
        }
        if (liveStreamProvider != null && extensionPoint.isAssignableFrom(LiveStreamProvider.class)) {
            extensions.add((T) liveStreamProvider);
        }
        return extensions;
    }

    // Package-private getters for testing
    StreamlinkChannelResolver getChannelResolver() {
        return channelResolver;
    }

    StreamlinkInspectionPlugin getInspectionPlugin() {
        return inspectionPlugin;
    }

    StreamlinkLiveStreamProvider getLiveStreamProvider() {
        return liveStreamProvider;
    }
}
