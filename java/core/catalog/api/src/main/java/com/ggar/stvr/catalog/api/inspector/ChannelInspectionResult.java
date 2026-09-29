package com.ggar.stvr.catalog.api.inspector;

import com.ggar.stvr.catalog.entities.ChannelName;
import com.ggar.stvr.catalog.entities.Platform;

import java.util.Objects;
import java.util.Set;

/**
 * Result data produced by a ChannelInspector when resolving a stream URL.
 */
public record ChannelInspectionResult(
        Platform platform,
        String slug,
        ChannelName name,
        boolean isLive,
        String category,
        Set<String> tags,
        Set<String> availableQualities
) {
    public ChannelInspectionResult {
        Objects.requireNonNull(platform, "Platform cannot be null");
        Objects.requireNonNull(name, "ChannelName cannot be null");
        tags = tags != null ? Set.copyOf(tags) : Set.of();
        availableQualities = availableQualities != null ? Set.copyOf(availableQualities) : Set.of();
    }

    public static ChannelInspectionResult of(
            Platform platform,
            String slug,
            ChannelName name,
            boolean isLive,
            String category,
            Set<String> tags,
            Set<String> availableQualities
    ) {
        return new ChannelInspectionResult(platform, slug, name, isLive, category, tags, availableQualities);
    }

    public static ChannelInspectionResult offline(
            Platform platform,
            String slug,
            ChannelName name
    ) {
        return new ChannelInspectionResult(platform, slug, name, false, null, Set.of(), Set.of());
    }

    public static ChannelInspectionResult online(
            Platform platform,
            String slug,
            ChannelName name,
            String category,
            Set<String> qualities
    ) {
        return new ChannelInspectionResult(platform, slug, name, true, category, Set.of(), qualities);
    }
}
