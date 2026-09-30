package com.ggar.stvr.packages.streamlink.plugins;

import com.ggar.stvr.packages.streamlink.command.AbstractStreamlinkCommandBuilder;
import com.ggar.stvr.packages.streamlink.plugins.generic.GenericCommandBuilder;
import com.ggar.stvr.packages.streamlink.plugins.generic.GenericPlugin;
import com.ggar.stvr.packages.streamlink.plugins.kick.KickCommandBuilder;
import com.ggar.stvr.packages.streamlink.plugins.kick.KickPlugin;
import com.ggar.stvr.packages.streamlink.plugins.tiktok.TikTokCommandBuilder;
import com.ggar.stvr.packages.streamlink.plugins.tiktok.TikTokPlugin;
import com.ggar.stvr.packages.streamlink.plugins.twitch.TwitchCommandBuilder;
import com.ggar.stvr.packages.streamlink.plugins.twitch.TwitchPlugin;
import com.ggar.stvr.packages.streamlink.plugins.youtube.YouTubeCommandBuilder;
import com.ggar.stvr.packages.streamlink.plugins.youtube.YouTubePlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StreamlinkPluginRegistryTest {

    private StreamlinkPluginRegistry registry;

    @BeforeEach
    void setUp() {
        registry = StreamlinkPluginRegistry.defaultRegistry();
    }

    @Test
    void testFindTwitchPluginByUrlAndName() {
        StreamlinkPlugin byUrl = registry.findForUrl("https://www.twitch.tv/streamer");
        StreamlinkPlugin byClip = registry.findForUrl("https://clips.twitch.tv/streamer-clip");
        StreamlinkPlugin byName = registry.find("twitch", null);

        assertThat(byUrl).isInstanceOf(TwitchPlugin.class);
        assertThat(byClip).isInstanceOf(TwitchPlugin.class);
        assertThat(byName).isInstanceOf(TwitchPlugin.class);
        assertThat(byUrl.createCommandBuilder("https://twitch.tv/test")).isInstanceOf(TwitchCommandBuilder.class);
    }

    @Test
    void testFindKickPluginByUrlAndName() {
        StreamlinkPlugin byUrl = registry.findForUrl("https://kick.com/streamer");
        StreamlinkPlugin byName = registry.find("kick", null);

        assertThat(byUrl).isInstanceOf(KickPlugin.class);
        assertThat(byName).isInstanceOf(KickPlugin.class);
        assertThat(byUrl.createCommandBuilder("https://kick.com/streamer")).isInstanceOf(KickCommandBuilder.class);
    }

    @Test
    void testFindYouTubePluginByUrlAndName() {
        StreamlinkPlugin byWatch = registry.findForUrl("https://www.youtube.com/watch?v=12345");
        StreamlinkPlugin byShort = registry.findForUrl("https://youtu.be/12345");
        StreamlinkPlugin byName = registry.find("youtube", null);

        assertThat(byWatch).isInstanceOf(YouTubePlugin.class);
        assertThat(byShort).isInstanceOf(YouTubePlugin.class);
        assertThat(byName).isInstanceOf(YouTubePlugin.class);
        assertThat(byWatch.createCommandBuilder("https://youtu.be/12345")).isInstanceOf(YouTubeCommandBuilder.class);
    }

    @Test
    void testFindTikTokPluginByUrlAndName() {
        StreamlinkPlugin byUrl = registry.findForUrl("https://www.tiktok.com/@creator/live");
        StreamlinkPlugin byName = registry.find("tiktok", null);

        assertThat(byUrl).isInstanceOf(TikTokPlugin.class);
        assertThat(byName).isInstanceOf(TikTokPlugin.class);
        assertThat(byUrl.createCommandBuilder("https://www.tiktok.com/@creator/live")).isInstanceOf(TikTokCommandBuilder.class);
    }

    @Test
    void testFindGenericFallback() {
        StreamlinkPlugin byUnknown = registry.findForUrl("https://dailymotion.com/video/123");
        StreamlinkPlugin byName = registry.find("dailymotion", null);

        assertThat(byUnknown).isInstanceOf(GenericPlugin.class);
        assertThat(byName).isInstanceOf(GenericPlugin.class);
        assertThat(byUnknown.createCommandBuilder("https://dailymotion.com/video/123")).isInstanceOf(GenericCommandBuilder.class);
    }

    @Test
    void testCustomPluginRegistrationPrecedence() {
        StreamlinkPlugin customTwitch = new AbstractStreamlinkPlugin() {
            @Override
            public String getName() {
                return "custom-twitch";
            }

            @Override
            public boolean supportsUrl(String url) {
                return url != null && url.contains("twitch.tv");
            }

            @Override
            public AbstractStreamlinkCommandBuilder<?> createCommandBuilder(String url) {
                return new TwitchCommandBuilder().url(url);
            }
        };

        registry.register(customTwitch);

        StreamlinkPlugin resolved = registry.findForUrl("https://twitch.tv/test");
        assertThat(resolved).isSameAs(customTwitch);
    }
}
