package com.ggar.stvr.catalog.entities;

import java.util.Collections;
import java.util.Map;

/**
 * Generic key-value implementation of PlatformMetadata.
 * Used as a fallback when no specialized platform metadata class is registered.
 */
public record GenericPlatformMetadata(Map<String, Object> attributes) implements PlatformMetadata {

    public GenericPlatformMetadata {
        attributes = attributes != null ? Map.copyOf(attributes) : Collections.emptyMap();
    }

    public static GenericPlatformMetadata of(Map<String, Object> attributes) {
        return new GenericPlatformMetadata(attributes);
    }

    public static GenericPlatformMetadata empty() {
        return new GenericPlatformMetadata(Collections.emptyMap());
    }

    @Override
    public Map<String, Object> asMap() {
        return attributes;
    }
}
