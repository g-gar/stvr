package com.ggar.stvr.inspection.api.resolver;

import com.ggar.stvr.catalog.entities.ChannelName;
import com.ggar.stvr.catalog.entities.Platform;

import java.util.Objects;

/**
 * Resolved channel identity data produced by a ChannelResolver from a stream URL.
 */
public record ChannelResolution(
        Platform platform,
        String slug,
        ChannelName name
) {
    public ChannelResolution {
        Objects.requireNonNull(platform, "Platform cannot be null");
        Objects.requireNonNull(name, "ChannelName cannot be null");
        slug = slug != null ? slug : "";
    }

    public static ChannelResolution of(Platform platform, String slug, ChannelName name) {
        return new ChannelResolution(platform, slug, name);
    }
}
