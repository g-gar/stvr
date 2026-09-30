package com.ggar.stvr.catalog.entities;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Set;

/**
 * Base abstract domain model for channel and live stream metadata.
 * Subclasses in platform-specific modules (e.g. Twitch, YouTube) may extend this class
 * to provide strongly-typed platform-specific attributes without polluting the core domain.
 */
@Getter
@EqualsAndHashCode
@ToString
public abstract class ChannelMetadata {

    private final boolean isLive;
    private final String title;
    private final String category;
    private final Set<String> tags;
    private final Set<String> availableQualities;

    protected ChannelMetadata(
            boolean isLive,
            String title,
            String category,
            Set<String> tags,
            Set<String> availableQualities
    ) {
        this.isLive = isLive;
        this.title = title;
        this.category = category;
        this.tags = tags != null ? Set.copyOf(tags) : Set.of();
        this.availableQualities = availableQualities != null ? Set.copyOf(availableQualities) : Set.of();
    }
}
