package com.ggar.stvr.catalog.entities;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;
import java.util.Objects;

/**
 * Pure domain model representing a streaming channel in the catalog.
 * Completely decoupled from database annotations (Neo4j, JPA) and external frameworks.
 */
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class Channel {

    @EqualsAndHashCode.Include
    private final ChannelId id;
    private final ChannelUrl url;
    private final Platform platform;
    private final String slug;
    private final ChannelName name;
    private final PlatformMetadata platformMetadata;

    public Channel(
            ChannelId id,
            ChannelUrl url,
            Platform platform,
            String slug,
            ChannelName name
    ) {
        this(id, url, platform, slug, name, GenericPlatformMetadata.empty());
    }

    public Channel(
            ChannelId id,
            ChannelUrl url,
            Platform platform,
            String slug,
            ChannelName name,
            Map<String, Object> metadata
    ) {
        this(id, url, platform, slug, name, PlatformMetadata.of(metadata));
    }

    public Channel(
            ChannelId id,
            ChannelUrl url,
            Platform platform,
            String slug,
            ChannelName name,
            PlatformMetadata platformMetadata
    ) {
        this.id = Objects.requireNonNull(id, "ChannelId cannot be null");
        this.url = Objects.requireNonNull(url, "ChannelUrl cannot be null");
        this.platform = Objects.requireNonNull(platform, "Platform cannot be null");
        this.slug = slug != null ? slug : "";
        this.name = Objects.requireNonNull(name, "ChannelName cannot be null");
        this.platformMetadata = platformMetadata != null ? platformMetadata : GenericPlatformMetadata.empty();
    }

    /**
     * Retrieves the metadata as a key-value map for serialization, persistence, and generic clients.
     *
     * @return map of metadata attributes
     */
    public Map<String, Object> getMetadata() {
        return platformMetadata.asMap();
    }

    /**
     * Retrieves the platform-specific metadata safely typed to the requested class.
     *
     * @param type target metadata class (e.g. TwitchChannelMetadata.class)
     * @param <T> metadata type extending PlatformMetadata
     * @return Optional containing the typed metadata if matching, or empty
     */
    public <T extends PlatformMetadata> java.util.Optional<T> getMetadata(Class<T> type) {
        if (type != null && type.isInstance(platformMetadata)) {
            return java.util.Optional.of(type.cast(platformMetadata));
        }
        return java.util.Optional.empty();
    }
}

