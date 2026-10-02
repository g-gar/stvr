package com.ggar.stvr.test.mocked.catalog;

import com.ggar.stvr.catalog.api.GetStreamSessionQueryHandler;
import com.ggar.stvr.catalog.api.ListChannelSessionsQueryHandler.StreamSessionDto;
import com.ggar.stvr.catalog.entities.ChannelId;
import com.ggar.stvr.catalog.entities.SessionId;
import com.ggar.stvr.catalog.implementation.GetStreamSessionQueryHandlerImpl;
import com.ggar.stvr.catalog.persistence.StreamSessionRepository;
import com.ggar.stvr.test.contracts.catalog.GetStreamSessionContractTest;
import org.junit.jupiter.api.BeforeEach;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Concrete mocked execution of GetStreamSessionContractTest.
 * Uses GetStreamSessionQueryHandlerImpl backed by an in-memory StreamSessionRepository.
 */
public class GetStreamSessionMockedTest extends GetStreamSessionContractTest {

    private final Map<SessionId, StreamSessionDto> storage = new ConcurrentHashMap<>();
    private GetStreamSessionQueryHandler handler;

    @BeforeEach
    void setUp() {
        storage.clear();

        StreamSessionRepository inMemoryRepository = new StreamSessionRepository() {
            @Override
            public Flux<StreamSessionDto> findSessionsByChannelId(ChannelId channelId) {
                return Flux.fromIterable(storage.values())
                        .filter(s -> s.channelId().equals(channelId));
            }

            @Override
            public Mono<StreamSessionDto> findSessionById(SessionId sessionId) {
                StreamSessionDto dto = storage.get(sessionId);
                return dto != null ? Mono.just(dto) : Mono.empty();
            }

            @Override
            public Mono<StreamSessionDto> save(StreamSessionDto session) {
                storage.put(session.id(), session);
                return Mono.just(session);
            }
        };

        this.handler = new GetStreamSessionQueryHandlerImpl(inMemoryRepository);
    }

    @Override
    protected GetStreamSessionQueryHandler getHandler() {
        return this.handler;
    }

    @Override
    protected void setupSession(StreamSessionDto session) {
        storage.put(session.id(), session);
    }
}
