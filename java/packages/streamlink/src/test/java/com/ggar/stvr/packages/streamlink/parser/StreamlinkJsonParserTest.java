package com.ggar.stvr.packages.streamlink.parser;

import com.ggar.stvr.packages.streamlink.exception.StreamlinkNoStreamsException;
import com.ggar.stvr.packages.streamlink.exception.StreamlinkParseException;
import com.ggar.stvr.packages.streamlink.exception.StreamlinkPluginNotFoundException;
import com.ggar.stvr.packages.streamlink.model.StreamlinkInspection;
import com.ggar.stvr.packages.streamlink.plugins.kick.KickStreamlinkException;
import com.ggar.stvr.packages.streamlink.plugins.tiktok.TikTokStreamlinkException;
import com.ggar.stvr.packages.streamlink.plugins.twitch.TwitchStreamlinkException;
import com.ggar.stvr.packages.streamlink.plugins.youtube.YouTubeStreamlinkException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StreamlinkJsonParserTest {

    private StreamlinkJsonParser parser;

    @BeforeEach
    void setUp() {
        parser = new StreamlinkJsonParser();
    }

    @Test
    void testParseSuccessfulTwitchJson() {
        String json = """
                {
                  "plugin": "twitch",
                  "metadata": {
                    "id": "41234567890",
                    "author": "ibai",
                    "category": "Just Chatting",
                    "title": "CHARLANDO UN RATO ANTES DE DORMIR"
                  },
                  "streams": {
                    "audio_only": {
                      "type": "hls",
                      "url": "https://video-weaver.mad01.hls.ttvnw.net/v1/playlist/audio_only.m3u8"
                    },
                    "1080p60": {
                      "type": "hls",
                      "url": "https://video-weaver.mad01.hls.ttvnw.net/v1/playlist/1080p60.m3u8",
                      "master": "https://usher.ttvnw.net/api/channel/hls/ibai.m3u8"
                    },
                    "best": {
                      "type": "hls",
                      "url": "https://video-weaver.mad01.hls.ttvnw.net/v1/playlist/1080p60.m3u8"
                    }
                  }
                }
                """;

        StreamlinkInspection inspection = parser.parse(json, "https://twitch.tv/ibai");

        assertThat(inspection).isNotNull();
        assertThat(inspection.plugin()).isEqualTo("twitch");
        assertThat(inspection.metadata()).isNotNull();
        assertThat(inspection.metadata().id()).isEqualTo("41234567890");
        assertThat(inspection.metadata().author()).isEqualTo("ibai");
        assertThat(inspection.metadata().category()).isEqualTo("Just Chatting");
        assertThat(inspection.metadata().title()).isEqualTo("CHARLANDO UN RATO ANTES DE DORMIR");

        assertThat(inspection.streams()).hasSize(3);
        assertThat(inspection.streams()).containsKey("1080p60");
        assertThat(inspection.streams().get("1080p60").type()).isEqualTo("hls");
        assertThat(inspection.streams().get("1080p60").master()).isEqualTo("https://usher.ttvnw.net/api/channel/hls/ibai.m3u8");
        assertThat(inspection.hasStreams()).isTrue();
    }

    @Test
    void testParseNoPlayableStreamsError() {
        String json = """
                {
                  "error": "No playable streams found on this URL: https://twitch.tv/offline_channel"
                }
                """;

        assertThatThrownBy(() -> parser.parse(json, "https://twitch.tv/offline_channel"))
                .isInstanceOf(StreamlinkNoStreamsException.class)
                .hasMessageContaining("No playable streams found")
                .matches(e -> ((StreamlinkNoStreamsException) e).getUrl().equals("https://twitch.tv/offline_channel"));
    }

    @Test
    void testParseNoPluginError() {
        String json = """
                {
                  "error": "No plugin can handle URL: https://example.com/unsupported"
                }
                """;

        assertThatThrownBy(() -> parser.parse(json, "https://example.com/unsupported"))
                .isInstanceOf(StreamlinkPluginNotFoundException.class)
                .hasMessageContaining("No plugin can handle URL")
                .matches(e -> ((StreamlinkPluginNotFoundException) e).getUrl().equals("https://example.com/unsupported"));
    }

    @Test
    void testParseKickSpecificErrorThrowsKickStreamlinkException() {
        String json = """
                {
                  "plugin": "kick",
                  "error": "Error while querying Kick API: 403 status response"
                }
                """;

        assertThatThrownBy(() -> parser.parse(json, "https://kick.com/streamer"))
                .isInstanceOf(KickStreamlinkException.class)
                .hasMessageContaining("Error while querying Kick API");
    }

    @Test
    void testParseTwitchSpecificErrorThrowsTwitchStreamlinkException() {
        String json = """
                {
                  "plugin": "twitch",
                  "error": "Failed to acquire client-integrity token"
                }
                """;

        assertThatThrownBy(() -> parser.parse(json, "https://twitch.tv/streamer"))
                .isInstanceOf(TwitchStreamlinkException.class)
                .hasMessageContaining("client-integrity token");
    }

    @Test
    void testParseYouTubeSpecificErrorThrowsYouTubeStreamlinkException() {
        String json = """
                {
                  "error": "This plugin does not support protected videos, try yt-dlp instead"
                }
                """;

        assertThatThrownBy(() -> parser.parse(json, "https://www.youtube.com/watch?v=123"))
                .isInstanceOf(YouTubeStreamlinkException.class)
                .hasMessageContaining("does not support protected videos");
    }

    @Test
    void testParseTikTokSpecificErrorThrowsTikTokStreamlinkException() {
        String json = """
                {
                  "error": "TikTok live room is private or blocked"
                }
                """;

        assertThatThrownBy(() -> parser.parse(json, "https://www.tiktok.com/@user/live"))
                .isInstanceOf(TikTokStreamlinkException.class)
                .hasMessageContaining("live room is private");
    }

    @Test
    void testParseMissingPluginFieldThrowsParseException() {
        String json = """
                {
                  "metadata": { "id": "1", "author": "a", "category": "c", "title": "t" },
                  "streams": { "best": { "url": "https://stream" } }
                }
                """;

        assertThatThrownBy(() -> parser.parse(json, "https://example.com"))
                .isInstanceOf(StreamlinkParseException.class)
                .hasMessageContaining("missing or empty 'plugin'");
    }

    @Test
    void testParseMissingMetadataFieldThrowsParseException() {
        String json = """
                {
                  "plugin": "twitch",
                  "streams": { "best": { "url": "https://stream" } }
                }
                """;

        assertThatThrownBy(() -> parser.parse(json, "https://twitch.tv/test"))
                .isInstanceOf(StreamlinkParseException.class)
                .hasMessageContaining("missing 'metadata'");
    }

    @Test
    void testParseEmptyStreamsThrowsNoStreamsException() {
        String json = """
                {
                  "plugin": "twitch",
                  "metadata": { "id": "1", "author": "test", "category": "Gaming", "title": "Playing" },
                  "streams": {}
                }
                """;

        assertThatThrownBy(() -> parser.parse(json, "https://twitch.tv/test"))
                .isInstanceOf(StreamlinkNoStreamsException.class)
                .hasMessageContaining("No stream qualities available");
    }

    @Test
    void testParseStreamWithBlankUrlThrowsParseException() {
        String json = """
                {
                  "plugin": "twitch",
                  "metadata": { "id": "1", "author": "test", "category": "Gaming", "title": "Playing" },
                  "streams": {
                    "best": { "url": "", "master": "" }
                  }
                }
                """;

        assertThatThrownBy(() -> parser.parse(json, "https://twitch.tv/test"))
                .isInstanceOf(StreamlinkParseException.class)
                .hasMessageContaining("contains no valid stream URL");
    }

    @Test
    void testParseEmptyOrBlankStringThrowsParseException() {
        assertThatThrownBy(() -> parser.parse("", "https://example.com"))
                .isInstanceOf(StreamlinkParseException.class)
                .hasMessageContaining("empty");

        assertThatThrownBy(() -> parser.parse(null, "https://example.com"))
                .isInstanceOf(StreamlinkParseException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void testParseMalformedJsonThrowsParseException() {
        String malformed = "{ \"plugin\": \"twitch\", invalid... }";

        assertThatThrownBy(() -> parser.parse(malformed, "https://example.com"))
                .isInstanceOf(StreamlinkParseException.class);
    }
}
