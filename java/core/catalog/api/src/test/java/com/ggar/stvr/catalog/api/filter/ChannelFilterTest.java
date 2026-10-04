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
    }

    @Test
    @DisplayName("Builder should correctly configure and normalize filter criteria")
    void shouldBuildCustomFilterWithNormalization() {
        ChannelFilter filter = ChannelFilter.builder()
                .nameQuery("Ibai")
                .platform(Platform.of("twitch"))
                .platforms(List.of(Platform.of("youtube")))
                .favoritesOnly(true)
                .build();

        assertThat(filter.nameQuery()).isEqualTo("Ibai");
        assertThat(filter.platforms()).containsExactlyInAnyOrder(Platform.of("twitch"), Platform.of("youtube"));
        assertThat(filter.favoritesOnly()).isTrue();
    }

    @Test
    @DisplayName("Platforms set in ChannelFilter should be immutable")
    void shouldEnsureImmutability() {
        ChannelFilter filter = ChannelFilter.builder()
                .platform(Platform.of("twitch"))
                .build();

        assertThatThrownBy(() -> filter.platforms().add(Platform.of("kick")))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
