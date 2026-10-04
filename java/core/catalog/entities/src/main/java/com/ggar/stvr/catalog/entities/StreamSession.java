package com.ggar.stvr.catalog.entities;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Pure domain model representing a historical or ongoing broadcast session for a channel.
 * Completely decoupled from database annotations and external frameworks.
 */
@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class StreamSession {

    @EqualsAndHashCode.Include
    private final SessionId id;
    private final ChannelId channelId;
    private final String title;
    private final String category;
    private final List<String> tags;
    private final Instant startedAt;
    private final Instant endedAt;
    private final PlatformMetadata platformMetadata;

    @Builder(toBuilder = true)
    public StreamSession(
            SessionId id,
            ChannelId channelId,
            String title,
            String category,
            List<String> tags,
            Instant startedAt,
            Instant endedAt,
            PlatformMetadata platformMetadata
    ) {
        this.id = Objects.requireNonNull(id, "SessionId cannot be null");
        this.channelId = Objects.requireNonNull(channelId, "ChannelId cannot be null");
        this.title = title != null ? title : "";
        this.category = category != null ? category : "";
        this.tags = tags != null ? List.copyOf(tags) : Collections.emptyList();
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt cannot be null");
        this.endedAt = endedAt;
        this.platformMetadata = platformMetadata != null ? platformMetadata : GenericPlatformMetadata.empty();
    }

    public boolean isActive() {
        return endedAt == null;
    }

    /**
     * Returns the metadata as a key-value map for serialization, persistence, and generic clients.
     *
     * @return map of metadata attributes
     */
    public Map<String, Object> getMetadata() {
        return platformMetadata.asMap();
    }

    /**
     * Retrieves the platform-specific session metadata safely typed to the requested class.
     *
     * @param type target metadata class
     * @param <T> metadata type extending PlatformMetadata
     * @return Optional containing the typed metadata if matching, or empty
     */
    public <T extends PlatformMetadata> java.util.Optional<T> getMetadata(Class<T> type) {
        if (type != null && type.isInstance(platformMetadata)) {
            return java.util.Optional.of(type.cast(platformMetadata));
        }
        return java.util.Optional.empty();
    }

    public static class StreamSessionBuilder {
        public StreamSessionBuilder metadata(Map<String, Object> metadata) {
            this.platformMetadata = PlatformMetadata.of(metadata);
            return this;
        }
    }
}

