package com.ggar.stvr.catalog.implementation;

import com.ggar.stvr.catalog.api.ListChannelSessionsQueryHandler;
import com.ggar.stvr.catalog.persistence.StreamSessionRepository;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Objects;

/**
 * Implementation of the query handler for listing a channel's stream sessions.
 * Fetches sessions from StreamSessionRepository and guarantees sort order
 * (startedAt descending, newest sessions first).
 */
@Service
public class ListChannelSessionsQueryHandlerImpl implements ListChannelSessionsQueryHandler {

    private static final Comparator<StreamSessionDto> DEFAULT_SORT =
            Comparator.comparing(StreamSessionDto::startedAt, Comparator.nullsLast(Comparator.reverseOrder()));

    private final StreamSessionRepository sessionRepository;

    public ListChannelSessionsQueryHandlerImpl(StreamSessionRepository sessionRepository) {
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "StreamSessionRepository cannot be null");
    }

    @Override
    public Publisher<StreamSessionDto> handle(ListChannelSessionsQuery query) {
        Objects.requireNonNull(query, "Query cannot be null");
        return sessionRepository.findSessionsByChannelId(query.channelId())
                .sort(DEFAULT_SORT);
    }
}
