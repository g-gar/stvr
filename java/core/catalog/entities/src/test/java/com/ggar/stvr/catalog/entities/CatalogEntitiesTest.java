package com.ggar.stvr.catalog.entities;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogEntitiesTest {

    @Test
    void shouldCreateAndValidateChannelId() {
        ChannelId id1 = ChannelId.random();
        assertThat(id1.value()).isNotNull();

        ChannelId id2 = ChannelId.fromString(id1.value().toString());
        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }

    @Test
    void shouldValidateChannelName() {
        ChannelName name = ChannelName.of("Ibai");
        assertThat(name.value()).isEqualTo("Ibai");

        assertThatThrownBy(() -> ChannelName.of("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldValidateChannelUrl() {
        ChannelUrl url = ChannelUrl.of("https://twitch.tv/ibai");
        assertThat(url.value()).isEqualTo(URI.create("https://twitch.tv/ibai"));
        assertThat(url.asString()).isEqualTo("https://twitch.tv/ibai");

        // Auto-normalize scheme
        ChannelUrl normalized = ChannelUrl.of("twitch.tv/ibai");
        assertThat(normalized.value().getScheme()).isEqualTo("https");
        assertThat(normalized.asString()).isEqualTo("https://twitch.tv/ibai");

        assertThatThrownBy(() -> ChannelUrl.of(""))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> ChannelUrl.of("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCreateAndValidateChannelEntity() {
        ChannelId id = ChannelId.random();
        ChannelUrl url = ChannelUrl.of("https://twitch.tv/ibai");
        ChannelName name = ChannelName.of("Ibai");

        Channel channel = new Channel(id, url, Platform.TWITCH, "ibai", name);

        assertThat(channel.getId()).isEqualTo(id);
        assertThat(channel.getUrl()).isEqualTo(url);
        assertThat(channel.getPlatform()).isEqualTo(Platform.TWITCH);
        assertThat(channel.getSlug()).isEqualTo("ibai");
        assertThat(channel.getName()).isEqualTo(name);

        Channel sameIdChannel = new Channel(id, url, Platform.TWITCH, "ibai", name);
        assertThat(channel).isEqualTo(sameIdChannel);
        assertThat(channel.hashCode()).isEqualTo(sameIdChannel.hashCode());
    }

    @Test
    void shouldCreateAndValidateSessionId() {
        SessionId id1 = SessionId.random();
        assertThat(id1.value()).isNotNull();

        SessionId id2 = SessionId.fromString(id1.asString());
        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }

    @Test
    void shouldCreateAndValidateStreamSession() {
        SessionId sessionId = SessionId.random();
        ChannelId channelId = ChannelId.random();
        java.time.Instant startedAt = java.time.Instant.now();

        StreamSession session = StreamSession.builder()
                .id(sessionId)
                .channelId(channelId)
                .title("Chill Stream")
                .category("Just Chatting")
                .tags(java.util.List.of("español", "relax"))
                .startedAt(startedAt)
                .build();

        assertThat(session.getId()).isEqualTo(sessionId);
        assertThat(session.getChannelId()).isEqualTo(channelId);
        assertThat(session.getTitle()).isEqualTo("Chill Stream");
        assertThat(session.getCategory()).isEqualTo("Just Chatting");
        assertThat(session.getTags()).containsExactly("español", "relax");
        assertThat(session.getStartedAt()).isEqualTo(startedAt);
        assertThat(session.getEndedAt()).isNull();
        assertThat(session.isActive()).isTrue();

        StreamSession endedSession = session.toBuilder()
                .endedAt(startedAt.plusSeconds(3600))
                .build();

        assertThat(endedSession.isActive()).isFalse();
        assertThat(endedSession.getEndedAt()).isNotNull();
    }
}
