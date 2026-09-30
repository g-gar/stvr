package com.ggar.stvr.catalog.api.inspector;

import com.ggar.stvr.catalog.entities.ChannelMetadata;
import com.ggar.stvr.catalog.entities.ChannelName;
import com.ggar.stvr.catalog.entities.GenericChannelMetadata;
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
        ChannelMetadata metadata
) {
    public ChannelInspectionResult {
        Objects.requireNonNull(platform, "Platform cannot be null");
        Objects.requireNonNull(name, "ChannelName cannot be null");
        metadata = metadata != null ? metadata : GenericChannelMetadata.empty();
    }

    public boolean isLive() {
        return metadata.isLive();
    }

    public String category() {
        return metadata.getCategory();
    }

    public Set<String> tags() {
        return metadata.getTags();
    }

    public Set<String> availableQualities() {
        return metadata.getAvailableQualities();
    }

    public static ChannelInspectionResult of(
            Platform platform,
            String slug,
            ChannelName name,
            ChannelMetadata metadata
    ) {
        return new ChannelInspectionResult(platform, slug, name, metadata);
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
        return new ChannelInspectionResult(platform, slug, name,
                GenericChannelMetadata.of(isLive, null, category, tags, availableQualities));
    }

    public static ChannelInspectionResult offline(
            Platform platform,
            String slug,
            ChannelName name
    ) {
        return new ChannelInspectionResult(platform, slug, name, GenericChannelMetadata.offline());
    }

    public static ChannelInspectionResult online(
            Platform platform,
            String slug,
            ChannelName name,
            String category,
            Set<String> qualities
    ) {
        return new ChannelInspectionResult(platform, slug, name,
                GenericChannelMetadata.online(null, category, qualities));
    }
}
