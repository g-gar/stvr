package com.ggar.stvr.catalog.api;

import com.ggar.stvr.catalog.api.ListChannelSessionsQueryHandler.StreamSessionDto;
import com.ggar.stvr.catalog.entities.SessionId;
import com.ggar.stvr.framework.cqrs.Query;
import com.ggar.stvr.framework.cqrs.QueryHandler;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * CQRS query handler interface for retrieving a specific stream session by its unique ID.
 */
public interface GetStreamSessionQueryHandler
        extends QueryHandler<GetStreamSessionQueryHandler.GetStreamSessionQuery, StreamSessionDto> {

    @Override
    Mono<StreamSessionDto> handle(GetStreamSessionQuery query);

    /**
     * Query parameters for retrieving a specific stream session.
     *
     * @param sessionId The unique stream session identifier.
     */
    record GetStreamSessionQuery(
            SessionId sessionId
    ) implements Query<StreamSessionDto> {

        public GetStreamSessionQuery {
            Objects.requireNonNull(sessionId, "SessionId cannot be null");
        }
    }
}
