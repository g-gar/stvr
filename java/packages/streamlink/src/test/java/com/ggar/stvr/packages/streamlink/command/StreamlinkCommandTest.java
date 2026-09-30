package com.ggar.stvr.packages.streamlink.command;

import com.ggar.stvr.packages.streamlink.plugins.generic.GenericCommandBuilder;
import com.ggar.stvr.packages.streamlink.plugins.kick.KickCommandBuilder;
import com.ggar.stvr.packages.streamlink.plugins.tiktok.TikTokCommandBuilder;
import com.ggar.stvr.packages.streamlink.plugins.twitch.TwitchCommandBuilder;
import com.ggar.stvr.packages.streamlink.plugins.youtube.YouTubeCommandBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StreamlinkCommandTest {

    @Test
    void testGenericCommandArgs() {
        StreamlinkCommand cmd = StreamlinkCommand.fromUrl("https://example.com/live")
                .quality("best")
                .json(true)
                .stdout(true)
                .output("/tmp/out.mp4")
                .retryStreams(3)
                .retryOpen(5)
                .streamSegmentThreads(4)
                .httpProxy("http://proxy.local:8080")
                .httpsProxy("https://proxy.local:8443")
                .httpHeader("User-Agent", "TestAgent/1.0")
                .customFlag("--ffmpeg-fout")
                .customOption("--hls-live-edge", "3")
                .build();

        List<String> args = cmd.toArgs();

        assertThat(args).containsSubsequence(
                "streamlink",
                "--json",
                "-O",
                "-o", "/tmp/out.mp4",
                "--retry-streams", "3",
                "--retry-open", "5",
                "--stream-segment-threads", "4",
                "--http-proxy", "http://proxy.local:8080",
                "--https-proxy", "https://proxy.local:8443",
                "--http-header", "User-Agent=TestAgent/1.0",
                "--ffmpeg-fout",
                "--hls-live-edge", "3",
                "https://example.com/live",
                "best"
        );
    }

    @Test
    void testMissingUrlThrowsIllegalStateException() {
        StreamlinkCommand cmd = StreamlinkCommand.builder()
                .quality("best")
                .build();

        assertThatThrownBy(cmd::toArgs)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("URL cannot be null or empty");

        assertThatThrownBy(() -> StreamlinkCommand.fromUrl(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("URL cannot be null or empty");
    }

    @Test
    void testFromUrlAutoDetectsPlatforms() {
        assertThat((Object) StreamlinkCommand.fromUrl("https://twitch.tv/streamer")).isInstanceOf(TwitchCommandBuilder.class);
        assertThat((Object) StreamlinkCommand.fromUrl("https://kick.com/streamer")).isInstanceOf(KickCommandBuilder.class);
        assertThat((Object) StreamlinkCommand.fromUrl("https://youtube.com/watch?v=123")).isInstanceOf(YouTubeCommandBuilder.class);
        assertThat((Object) StreamlinkCommand.fromUrl("https://youtu.be/123")).isInstanceOf(YouTubeCommandBuilder.class);
        assertThat((Object) StreamlinkCommand.fromUrl("https://www.tiktok.com/@creator/live")).isInstanceOf(TikTokCommandBuilder.class);
        assertThat((Object) StreamlinkCommand.fromUrl("https://otherplatform.com/live")).isInstanceOf(GenericCommandBuilder.class);
    }

    @Test
    void testFromUrlWithExpectedClass() {
        TwitchCommandBuilder twitch = StreamlinkCommand.fromUrl("https://twitch.tv/streamer", TwitchCommandBuilder.class);
        assertThat(twitch).isNotNull();

        assertThatThrownBy(() -> StreamlinkCommand.fromUrl("https://twitch.tv/streamer", KickCommandBuilder.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expected KickCommandBuilder");
    }

    @Test
    void testOfFactoryMethods() {
        StreamlinkCommand cmd = StreamlinkCommand.of("https://twitch.tv/streamer");
        assertThat(cmd.getUrl()).isEqualTo("https://twitch.tv/streamer");
        assertThat(cmd.toArgs()).contains("https://twitch.tv/streamer");

        StreamlinkCommand withQuality = StreamlinkCommand.of("https://kick.com/streamer", "720p60");
        assertThat(withQuality.getUrl()).isEqualTo("https://kick.com/streamer");
        assertThat(withQuality.getQuality()).isEqualTo("720p60");
        assertThat(withQuality.toArgs()).containsSubsequence("https://kick.com/streamer", "720p60");
    }

    @Test
    void testTwitchCommandBuilder() {
        TwitchCommandBuilder twitch = StreamlinkCommand.fromUrl("https://twitch.tv/streamer");
        StreamlinkCommand cmd = twitch
                .disableAds()
                .lowLatency()
                .disableHosting()
                .disableReruns()
                .forceClientIntegrity()
                .purgeClientIntegrity()
                .supportedCodecs("h264", "av1")
                .apiHeader("Client-ID", "mock-client-id")
                .accessTokenParam("player_type", "site")
                .build();

        List<String> args = cmd.toArgs();

        assertThat(args).contains(
                "--twitch-disable-ads",
                "--twitch-low-latency",
                "--twitch-disable-hosting",
                "--twitch-disable-reruns",
                "--twitch-force-client-integrity",
                "--twitch-purge-client-integrity"
        );
        assertThat(args).containsSubsequence("--twitch-supported-codecs", "h264,av1");
        assertThat(args).containsSubsequence("--twitch-api-header", "Client-ID=mock-client-id");
        assertThat(args).containsSubsequence("--twitch-access-token-param", "player_type=site");
        assertThat(args.get(args.size() - 1)).isEqualTo("https://twitch.tv/streamer");
    }

    @Test
    void testKickCommandBuilder() {
        KickCommandBuilder kick = StreamlinkCommand.fromUrl("https://kick.com/streamer");
        StreamlinkCommand cmd = kick
                .lowLatency()
                .quality("720p60")
                .build();

        List<String> args = cmd.toArgs();

        assertThat(args).contains("--kick-low-latency");
        assertThat(args).containsSubsequence("https://kick.com/streamer", "720p60");
    }

    @Test
    void testYouTubeCommandBuilder() {
        YouTubeCommandBuilder youtube = StreamlinkCommand.fromUrl("https://youtube.com/watch?v=12345");
        StreamlinkCommand cmd = youtube
                .includeDashManifests()
                .build();

        List<String> args = cmd.toArgs();

        assertThat(args).contains("--youtube-include-dash-manifests");
        assertThat(args).endsWith("https://youtube.com/watch?v=12345");
    }

    @Test
    void testTikTokCommandBuilder() {
        TikTokCommandBuilder tiktok = StreamlinkCommand.fromUrl("https://www.tiktok.com/@creator/live");
        StreamlinkCommand cmd = tiktok
                .appVersion("1.0.0")
                .build();

        List<String> args = cmd.toArgs();

        assertThat(args).containsSubsequence("--tiktok-app-version", "1.0.0");
        assertThat(args).endsWith("https://www.tiktok.com/@creator/live");
    }
}
