package com.ggar.stvr.catalog.implementation;

import com.ggar.stvr.catalog.api.GetStreamSessionQueryHandler;
import com.ggar.stvr.catalog.api.ListChannelSessionsQueryHandler.StreamSessionDto;
import com.ggar.stvr.catalog.api.exception.StreamSessionNotFoundException;
import com.ggar.stvr.catalog.persistence.StreamSessionRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Implementation of the query handler for retrieving a specific stream session by ID.
 */
@Service
public class GetStreamSessionQueryHandlerImpl implements GetStreamSessionQueryHandler {

    private final StreamSessionRepository sessionRepository;

    public GetStreamSessionQueryHandlerImpl(StreamSessionRepository sessionRepository) {
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "StreamSessionRepository cannot be null");
    }

    @Override
    public Mono<StreamSessionDto> handle(GetStreamSessionQuery query) {
        Objects.requireNonNull(query, "Query cannot be null");
        return sessionRepository.findSessionById(query.sessionId())
                .switchIfEmpty(Mono.error(new StreamSessionNotFoundException(query.sessionId())));
    }
}
