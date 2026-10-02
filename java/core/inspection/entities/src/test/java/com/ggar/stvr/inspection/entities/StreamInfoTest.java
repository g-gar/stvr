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
        StreamInfo info = StreamInfo.offline(url, Platform.TWITCH);

        assertThat(info.getChannelUrl()).isEqualTo(url);
        assertThat(info.getPlatform()).isEqualTo(Platform.TWITCH);
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
                .platform(Platform.KICK)
                .live(true)
                .title("Playing Elden Ring")
                .category("Action RPG")
                .tags(List.of("english", "gaming"))
                .availableQualities(List.of("1080p60", "720p60", "audio_only"))
                .startedAt(now)
                .metadata(Map.of("viewers", 1500))
                .build();

        assertThat(info.getChannelUrl()).isEqualTo(url);
        assertThat(info.getPlatform()).isEqualTo(Platform.KICK);
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
        assertThatThrownBy(() -> StreamInfo.builder().platform(Platform.TWITCH).build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("channelUrl");
    }
}
