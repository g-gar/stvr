package com.ggar.stvr.streaming.entities;

import com.ggar.stvr.catalog.entities.ChannelId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StreamingEntitiesTest {

    @Test
    void shouldCreateAndNormalizeStreamQuality() {
        StreamQuality q1 = StreamQuality.of("1080p60");
        assertThat(q1.value()).isEqualTo("1080p60");

        StreamQuality q2 = StreamQuality.of("  720P  ");
        assertThat(q2.value()).isEqualTo("720p");

        StreamQuality qDefault = StreamQuality.of(null);
        assertThat(qDefault.value()).isEqualTo("best");

        StreamQuality qBlank = StreamQuality.of("   ");
        assertThat(qBlank.value()).isEqualTo("best");

        assertThat(StreamQuality.BEST.value()).isEqualTo("best");
        assertThat(StreamQuality.WORST.value()).isEqualTo("worst");
        assertThat(StreamQuality.AUDIO_ONLY.value()).isEqualTo("audio_only");
    }

    @Test
    void shouldCreateAndValidateStreamKey() {
        ChannelId channelId = ChannelId.random();
        StreamKey key1 = StreamKey.of(channelId, StreamQuality.of("720p"));
        StreamKey key2 = StreamKey.of(channelId, "720p");

        assertThat(key1).isEqualTo(key2);
        assertThat(key1.hashCode()).isEqualTo(key2.hashCode());
        assertThat(key1.channelId()).isEqualTo(channelId);
        assertThat(key1.quality()).isEqualTo(StreamQuality.of("720p"));

        // Default quality
        StreamKey keyDefault = new StreamKey(channelId, null);
        assertThat(keyDefault.quality()).isEqualTo(StreamQuality.BEST);

        assertThatThrownBy(() -> new StreamKey(null, StreamQuality.BEST))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldCreateAndValidateActiveStreamSnapshot() {
        ChannelId channelId = ChannelId.random();
        StreamKey key = StreamKey.of(channelId, StreamQuality.BEST);
        Instant now = Instant.now();

        ActiveStreamSnapshot snapshot = ActiveStreamSnapshot.builder()
                .streamKey(key)
                .startedAt(now)
                .connectedViewers(3)
                .active(true)
                .build();

        assertThat(snapshot.getStreamKey()).isEqualTo(key);
        assertThat(snapshot.getStartedAt()).isEqualTo(now);
        assertThat(snapshot.getConnectedViewers()).isEqualTo(3);
        assertThat(snapshot.isActive()).isTrue();

        ActiveStreamSnapshot updated = snapshot.toBuilder()
                .connectedViewers(4)
                .build();

        assertThat(updated.getConnectedViewers()).isEqualTo(4);
        assertThat(updated).isEqualTo(snapshot); // Equals based on streamKey
    }
}

