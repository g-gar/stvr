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
    private final Map<String, Object> metadata;

    @Builder(toBuilder = true)
    public StreamSession(
            SessionId id,
            ChannelId channelId,
            String title,
            String category,
            List<String> tags,
            Instant startedAt,
            Instant endedAt,
            Map<String, Object> metadata
    ) {
        this.id = Objects.requireNonNull(id, "SessionId cannot be null");
        this.channelId = Objects.requireNonNull(channelId, "ChannelId cannot be null");
        this.title = title != null ? title : "";
        this.category = category != null ? category : "";
        this.tags = tags != null ? List.copyOf(tags) : Collections.emptyList();
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt cannot be null");
        this.endedAt = endedAt;
        this.metadata = metadata != null ? Map.copyOf(metadata) : Collections.emptyMap();
    }

    public boolean isActive() {
        return endedAt == null;
    }
}
