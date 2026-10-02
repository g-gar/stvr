package com.ggar.stvr.catalog.api;

import com.ggar.stvr.catalog.entities.ChannelId;
import com.ggar.stvr.catalog.entities.SessionId;
import com.ggar.stvr.catalog.entities.StreamSession;
import com.ggar.stvr.framework.cqrs.Query;
import com.ggar.stvr.framework.cqrs.QueryHandler;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * CQRS query handler interface for retrieving historical or ongoing stream sessions of a channel.
 */
public interface ListChannelSessionsQueryHandler
        extends QueryHandler<ListChannelSessionsQueryHandler.ListChannelSessionsQuery, ListChannelSessionsQueryHandler.StreamSessionDto> {

    /**
     * Query parameters for retrieving a channel's stream sessions.
     *
     * @param channelId The channel whose sessions will be listed.
     */
    record ListChannelSessionsQuery(
            ChannelId channelId
    ) implements Query<StreamSessionDto> {

        public ListChannelSessionsQuery {
            Objects.requireNonNull(channelId, "ChannelId cannot be null");
        }
    }

    /**
     * Projected representation of a stream session for catalog history views.
     */
    record StreamSessionDto(
            SessionId id,
            ChannelId channelId,
            String title,
            String category,
            List<String> tags,
            Instant startedAt,
            Instant endedAt,
            boolean active,
            Map<String, Object> metadata
    ) {
        public StreamSessionDto {
            Objects.requireNonNull(id, "id cannot be null");
            Objects.requireNonNull(channelId, "channelId cannot be null");
            Objects.requireNonNull(startedAt, "startedAt cannot be null");
            tags = tags != null ? List.copyOf(tags) : Collections.emptyList();
            metadata = metadata != null ? Map.copyOf(metadata) : Collections.emptyMap();
        }

        public static StreamSessionDto fromDomain(StreamSession session) {
            Objects.requireNonNull(session, "session cannot be null");
            return new StreamSessionDto(
                    session.getId(),
                    session.getChannelId(),
                    session.getTitle(),
                    session.getCategory(),
                    session.getTags(),
                    session.getStartedAt(),
                    session.getEndedAt(),
                    session.isActive(),
                    session.getMetadata()
            );
        }
    }
}
