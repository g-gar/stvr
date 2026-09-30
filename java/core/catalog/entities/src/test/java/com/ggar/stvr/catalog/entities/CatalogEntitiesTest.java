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
}
