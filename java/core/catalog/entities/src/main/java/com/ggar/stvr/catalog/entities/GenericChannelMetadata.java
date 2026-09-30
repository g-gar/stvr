package com.ggar.stvr.catalog.entities;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Set;

/**
 * Standard, platform-agnostic implementation of ChannelMetadata.
 * Used for generic streams, fallback inspectors, or platforms without dedicated SDK providers.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class GenericChannelMetadata extends ChannelMetadata {

    public GenericChannelMetadata(
            boolean isLive,
            String title,
            String category,
            Set<String> tags,
            Set<String> availableQualities
    ) {
        super(isLive, title, category, tags, availableQualities);
    }

    public static GenericChannelMetadata empty() {
        return new GenericChannelMetadata(false, null, null, Set.of(), Set.of());
    }

    public static GenericChannelMetadata offline() {
        return new GenericChannelMetadata(false, null, null, Set.of(), Set.of());
    }

    public static GenericChannelMetadata online(String title, String category, Set<String> availableQualities) {
        return new GenericChannelMetadata(true, title, category, Set.of(), availableQualities);
    }

    public static GenericChannelMetadata of(
            boolean isLive,
            String title,
            String category,
            Set<String> tags,
            Set<String> availableQualities
    ) {
        return new GenericChannelMetadata(isLive, title, category, tags, availableQualities);
    }
}
