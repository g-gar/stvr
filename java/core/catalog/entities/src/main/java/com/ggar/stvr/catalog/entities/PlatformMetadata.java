package com.ggar.stvr.catalog.entities;

import java.io.Serializable;
import java.util.Map;

/**
 * Marker interface for platform-specific channel, stream, and session metadata.
 * Extended by platform plugins (Twitch, YouTube, Kick, etc.) to provide
 * static compile-time typing for heterogeneous platform attributes.
 */
public interface PlatformMetadata extends Serializable {

    /**
     * Returns a key-value map representation of the metadata for serialization and persistence.
     *
     * @return map of attribute names to values
     */
    default Map<String, Object> asMap() {
        return Map.of();
    }

    /**
     * Returns an empty PlatformMetadata instance.
     *
     * @return empty metadata
     */
    static PlatformMetadata empty() {
        return GenericPlatformMetadata.empty();
    }

    /**
     * Wraps a raw map into a GenericPlatformMetadata instance.
     *
     * @param attributes key-value attributes
     * @return PlatformMetadata wrapping the attributes
     */
    static PlatformMetadata of(Map<String, Object> attributes) {
        return GenericPlatformMetadata.of(attributes);
    }
}
