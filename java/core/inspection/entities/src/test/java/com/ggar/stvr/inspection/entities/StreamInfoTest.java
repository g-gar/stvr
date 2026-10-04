package com.ggar.stvr.inspection.entities;

import com.ggar.stvr.catalog.entities.ChannelUrl;
import com.ggar.stvr.catalog.entities.Platform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StreamInfoTest {

    @Test
    @DisplayName("Should create offline stream info with default values")
    void shouldCreateOfflineStreamInfo() {
        ChannelUrl url = ChannelUrl.of("https://twitch.tv/example");
        StreamInfo info = StreamInfo.offline(url, Platform.of("twitch"));

        assertThat(info.getChannelUrl()).isEqualTo(url);
        assertThat(info.getPlatform()).isEqualTo(Platform.of("twitch"));
        assertThat(info.isLive()).isFalse();
        assertThat(info.getTitle()).isEmpty();
        assertThat(info.getCategory()).isEmpty();
        assertThat(info.getTags()).isEmpty();
        assertThat(info.getAvailableQualities()).isEmpty();
        assertThat(info.getMetadata()).isEmpty();
    }

    @Test
    @DisplayName("Should create active stream info with telemetry details")
    void shouldCreateActiveStreamInfo() {
        ChannelUrl url = ChannelUrl.of("https://kick.com/streamer");
        Instant now = Instant.now();

        StreamInfo info = StreamInfo.builder()
                .channelUrl(url)
                .platform(Platform.of("kick"))
                .live(true)
                .title("Playing Elden Ring")
                .category("Action RPG")
                .tags(List.of("english", "gaming"))
                .availableQualities(List.of("1080p60", "720p60", "audio_only"))
                .startedAt(now)
                .metadata(Map.of("viewers", 1500))
                .build();

        assertThat(info.getChannelUrl()).isEqualTo(url);
        assertThat(info.getPlatform()).isEqualTo(Platform.of("kick"));
        assertThat(info.isLive()).isTrue();
        assertThat(info.getTitle()).isEqualTo("Playing Elden Ring");
        assertThat(info.getCategory()).isEqualTo("Action RPG");
        assertThat(info.getTags()).containsExactly("english", "gaming");
        assertThat(info.getAvailableQualities()).containsExactly("1080p60", "720p60", "audio_only");
        assertThat(info.getStartedAt()).isEqualTo(now);
        assertThat(info.getMetadata()).containsEntry("viewers", 1500);
    }

    @Test
    @DisplayName("Should throw exception if channelUrl is null")
    void shouldThrowWhenChannelUrlMissing() {
        assertThatThrownBy(() -> StreamInfo.builder().platform(Platform.of("twitch")).build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("channelUrl");
    }

    record SampleStreamMetadata(int viewers, String resolution) implements com.ggar.stvr.catalog.entities.PlatformMetadata {
        @Override
        public Map<String, Object> asMap() {
            return Map.of("viewers", viewers, "resolution", resolution);
        }
    }

    @Test
    @DisplayName("Should support typed PlatformMetadata on StreamInfo")
    void shouldSupportTypedPlatformMetadata() {
        ChannelUrl url = ChannelUrl.of("https://twitch.tv/streamer");
        SampleStreamMetadata custom = new SampleStreamMetadata(5200, "1080p60");

        StreamInfo info = StreamInfo.builder()
                .channelUrl(url)
                .platform(Platform.of("twitch"))
                .live(true)
                .platformMetadata(custom)
                .build();

        assertThat(info.getMetadata(SampleStreamMetadata.class)).contains(custom);
        assertThat(info.getMetadata()).containsEntry("viewers", 5200);
        assertThat(info.getMetadata()).containsEntry("resolution", "1080p60");
    }
}

