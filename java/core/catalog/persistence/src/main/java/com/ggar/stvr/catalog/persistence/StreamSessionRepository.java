package com.ggar.stvr.catalog.persistence;

import com.ggar.stvr.catalog.api.ListChannelSessionsQueryHandler.StreamSessionDto;
import com.ggar.stvr.catalog.entities.ChannelId;
import com.ggar.stvr.catalog.entities.SessionId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive persistence repository port for StreamSession catalog operations.
 */
public interface StreamSessionRepository {

    /**
     * Finds stream sessions for a given channel.
     *
     * @param channelId The channel identifier.
     * @return Reactive Flux of StreamSessionDto.
     */
    Flux<StreamSessionDto> findSessionsByChannelId(ChannelId channelId);

    /**
     * Finds a specific stream session by its unique identifier.
     *
     * @param sessionId The session identifier.
     * @return Mono emitting the StreamSessionDto or empty if not found.
     */
    Mono<StreamSessionDto> findSessionById(SessionId sessionId);

    /**
     * Saves or updates a stream session record.
     *
     * @param session The session to persist.
     * @return Mono emitting the persisted StreamSessionDto.
     */
    Mono<StreamSessionDto> save(StreamSessionDto session);
}
