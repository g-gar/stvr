package com.ggar.stvr.catalog.api.filter;

import com.ggar.stvr.catalog.entities.Platform;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Filter criteria for listing channels in the catalog.
 * Supports filtering by name substring, platforms, favorite status, live status, category and tags.
 */
public record ChannelFilter(
        String nameQuery,
        Set<Platform> platforms,
        boolean favoritesOnly,
        Boolean isLive,
        String category,
        Set<String> tags
) {
    public ChannelFilter {
        platforms = platforms != null ? Set.copyOf(platforms) : Set.of();
        tags = tags != null ? Set.copyOf(tags) : Set.of();
    }

    public static ChannelFilter all() {
        return new ChannelFilter(null, Set.of(), false, null, null, Set.of());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String nameQuery;
        private Set<Platform> platforms = new HashSet<>();
        private boolean favoritesOnly;
        private Boolean isLive;
        private String category;
        private Set<String> tags = new HashSet<>();

        public Builder nameQuery(String nameQuery) {
            this.nameQuery = nameQuery;
            return this;
        }

        public Builder platform(Platform platform) {
            if (platform != null) {
                this.platforms.add(platform);
            }
            return this;
        }

        public Builder platforms(Collection<Platform> platforms) {
            if (platforms != null) {
                this.platforms.addAll(platforms);
            }
            return this;
        }

        public Builder favoritesOnly(boolean favoritesOnly) {
            this.favoritesOnly = favoritesOnly;
            return this;
        }

        public Builder isLive(Boolean isLive) {
            this.isLive = isLive;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder tag(String tag) {
            if (tag != null && !tag.isBlank()) {
                this.tags.add(tag.trim().toLowerCase());
            }
            return this;
        }

        public Builder tags(Collection<String> tags) {
            if (tags != null) {
                tags.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(t -> !t.isBlank())
                        .map(String::toLowerCase)
                        .forEach(this.tags::add);
            }
            return this;
        }

        public ChannelFilter build() {
            return new ChannelFilter(nameQuery, platforms, favoritesOnly, isLive, category, tags);
        }
    }
}
