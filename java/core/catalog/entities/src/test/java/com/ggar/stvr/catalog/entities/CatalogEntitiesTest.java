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
    void shouldCreateAndValidateGenericChannelMetadata() {
        GenericChannelMetadata metadata = GenericChannelMetadata.of(
                true,
                "Stream Title",
                "Just Chatting",
                java.util.Set.of("es", "irl"),
                java.util.Set.of("1080p60", "720p60")
        );

        assertThat(metadata.isLive()).isTrue();
        assertThat(metadata.getTitle()).isEqualTo("Stream Title");
        assertThat(metadata.getCategory()).isEqualTo("Just Chatting");
        assertThat(metadata.getTags()).containsExactlyInAnyOrder("es", "irl");
        assertThat(metadata.getAvailableQualities()).containsExactlyInAnyOrder("1080p60", "720p60");

        GenericChannelMetadata empty = GenericChannelMetadata.empty();
        assertThat(empty.isLive()).isFalse();
        assertThat(empty.getTitle()).isNull();
        assertThat(empty.getCategory()).isNull();
        assertThat(empty.getTags()).isEmpty();
        assertThat(empty.getAvailableQualities()).isEmpty();
    }

    @Test
    void shouldCreateAndValidateChannelEntity() {
        ChannelId id = ChannelId.random();
        ChannelUrl url = ChannelUrl.of("https://twitch.tv/ibai");
        ChannelName name = ChannelName.of("Ibai");
        GenericChannelMetadata metadata = GenericChannelMetadata.offline();

        Channel channel = new Channel(id, url, Platform.TWITCH, "ibai", name, metadata);

        assertThat(channel.getId()).isEqualTo(id);
        assertThat(channel.getUrl()).isEqualTo(url);
        assertThat(channel.getPlatform()).isEqualTo(Platform.TWITCH);
        assertThat(channel.getSlug()).isEqualTo("ibai");
        assertThat(channel.getName()).isEqualTo(name);
        assertThat(channel.getMetadata()).isEqualTo(metadata);

        GenericChannelMetadata liveMetadata = GenericChannelMetadata.online("Live now", "Gaming", java.util.Set.of("1080p60"));
        channel.updateMetadata(liveMetadata);
        assertThat(channel.getMetadata()).isEqualTo(liveMetadata);

        Channel sameIdChannel = new Channel(id, url, Platform.TWITCH, "ibai", name);
        assertThat(channel).isEqualTo(sameIdChannel);
        assertThat(channel.hashCode()).isEqualTo(sameIdChannel.hashCode());
    }
}
