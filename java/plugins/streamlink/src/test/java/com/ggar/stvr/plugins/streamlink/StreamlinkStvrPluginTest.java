package com.ggar.stvr.plugins.streamlink;

import com.ggar.stvr.framework.plugin.PluginContext;
import com.ggar.stvr.framework.plugin.StvrPlugin;
import com.ggar.stvr.inspection.api.InspectionPlugin;
import com.ggar.stvr.inspection.api.resolver.ChannelResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.ServiceLoader;

import static org.assertj.core.api.Assertions.assertThat;

class StreamlinkStvrPluginTest {

    private StreamlinkStvrPlugin plugin;

    @BeforeEach
    void setUp() {
        plugin = new StreamlinkStvrPlugin();
        plugin.initialize(PluginContext.empty());
    }

    @Test
    void shouldExposePluginMetadata() {
        assertThat(plugin.getId()).isEqualTo("streamlink");
        assertThat(plugin.getName()).isEqualTo("Streamlink Adapter Plugin");
        assertThat(plugin.getVersion()).isEqualTo("1.0.0");
        assertThat(plugin.getDescription()).isNotEmpty();
    }

    @Test
    void shouldProvideChannelResolverExtension() {
        List<ChannelResolver> resolvers = plugin.getExtensions(ChannelResolver.class);
        assertThat(resolvers).hasSize(1);
        assertThat(resolvers.get(0)).isInstanceOf(StreamlinkChannelResolver.class);
    }

    @Test
    void shouldProvideInspectionPluginExtension() {
        List<InspectionPlugin> inspectionPlugins = plugin.getExtensions(InspectionPlugin.class);
        assertThat(inspectionPlugins).hasSize(1);
        assertThat(inspectionPlugins.get(0)).isInstanceOf(StreamlinkInspectionPlugin.class);
    }

    @Test
    void shouldProvideLiveStreamProviderExtension() {
        List<com.ggar.stvr.streaming.api.LiveStreamProvider> providers = plugin.getExtensions(com.ggar.stvr.streaming.api.LiveStreamProvider.class);
        assertThat(providers).hasSize(1);
        assertThat(providers.get(0)).isInstanceOf(StreamlinkLiveStreamProvider.class);
    }

    @Test
    void shouldReturnEmptyForUnknownExtensionPoint() {
        assertThat(plugin.getExtensions(String.class)).isEmpty();
    }

    @Test
    void shouldBeDiscoverableViaServiceLoader() {
        ServiceLoader<StvrPlugin> loader = ServiceLoader.load(StvrPlugin.class);
        boolean found = false;
        for (StvrPlugin loaded : loader) {
            if ("streamlink".equals(loaded.getId())) {
                found = true;
                break;
            }
        }
        assertThat(found).isTrue();
    }

    @Test
    void shouldCleanUpOnDestroy() {
        plugin.destroy();
        assertThat(plugin.getExtensions(ChannelResolver.class)).isEmpty();
        assertThat(plugin.getExtensions(InspectionPlugin.class)).isEmpty();
        assertThat(plugin.getExtensions(com.ggar.stvr.streaming.api.LiveStreamProvider.class)).isEmpty();
    }
}
