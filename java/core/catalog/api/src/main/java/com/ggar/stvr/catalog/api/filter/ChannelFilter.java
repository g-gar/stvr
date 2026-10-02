package com.ggar.stvr.catalog.api.filter;

import com.ggar.stvr.catalog.entities.Platform;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Filter criteria for listing channels in the catalog.
 * Supports filtering by name substring, platforms, and favorite status.
 */
public record ChannelFilter(
        String nameQuery,
        Set<Platform> platforms,
        boolean favoritesOnly
) {
    public ChannelFilter {
        platforms = platforms != null ? Set.copyOf(platforms) : Set.of();
    }

    public static ChannelFilter all() {
        return new ChannelFilter(null, Set.of(), false);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String nameQuery;
        private Set<Platform> platforms = new HashSet<>();
        private boolean favoritesOnly;

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

        public ChannelFilter build() {
            return new ChannelFilter(nameQuery, platforms, favoritesOnly);
        }
    }
}
