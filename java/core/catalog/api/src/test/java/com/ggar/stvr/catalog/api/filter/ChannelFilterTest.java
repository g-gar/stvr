package com.ggar.stvr.catalog.api.filter;

import com.ggar.stvr.catalog.entities.Platform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChannelFilterTest {

    @Test
    @DisplayName("ChannelFilter.all() should initialize empty filter criteria")
    void shouldCreateDefaultAllFilter() {
        ChannelFilter filter = ChannelFilter.all();

        assertThat(filter.nameQuery()).isNull();
        assertThat(filter.platforms()).isEmpty();
        assertThat(filter.favoritesOnly()).isFalse();
        assertThat(filter.isLive()).isNull();
        assertThat(filter.category()).isNull();
        assertThat(filter.tags()).isEmpty();
    }

    @Test
    @DisplayName("Builder should correctly configure and normalize filter criteria")
    void shouldBuildCustomFilterWithNormalization() {
        ChannelFilter filter = ChannelFilter.builder()
                .nameQuery("Ibai")
                .platform(Platform.TWITCH)
                .platforms(List.of(Platform.YOUTUBE))
                .favoritesOnly(true)
                .isLive(true)
                .category("Just Chatting")
                .tag(" ESPORTS ")
                .tags(List.of("Gaming", " "))
                .build();

        assertThat(filter.nameQuery()).isEqualTo("Ibai");
        assertThat(filter.platforms()).containsExactlyInAnyOrder(Platform.TWITCH, Platform.YOUTUBE);
        assertThat(filter.favoritesOnly()).isTrue();
        assertThat(filter.isLive()).isTrue();
        assertThat(filter.category()).isEqualTo("Just Chatting");
        assertThat(filter.tags()).containsExactlyInAnyOrder("esports", "gaming");
    }

    @Test
    @DisplayName("Platforms and tags sets in ChannelFilter should be immutable")
    void shouldEnsureImmutability() {
        ChannelFilter filter = ChannelFilter.builder()
                .platform(Platform.TWITCH)
                .tag("esports")
                .build();

        assertThatThrownBy(() -> filter.platforms().add(Platform.KICK))
                .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> filter.tags().add("other"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
