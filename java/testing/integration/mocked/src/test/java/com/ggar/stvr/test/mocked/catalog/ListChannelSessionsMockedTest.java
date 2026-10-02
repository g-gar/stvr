package com.ggar.stvr.test.mocked.catalog;

import com.ggar.stvr.catalog.api.ListChannelSessionsQueryHandler;
import com.ggar.stvr.catalog.api.ListChannelSessionsQueryHandler.StreamSessionDto;
import com.ggar.stvr.catalog.entities.ChannelId;
import com.ggar.stvr.catalog.entities.SessionId;
import com.ggar.stvr.catalog.implementation.ListChannelSessionsQueryHandlerImpl;
import com.ggar.stvr.catalog.persistence.StreamSessionRepository;
import com.ggar.stvr.test.contracts.catalog.ListChannelSessionsContractTest;
import org.junit.jupiter.api.BeforeEach;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Concrete mocked execution of ListChannelSessionsContractTest.
 * Uses ListChannelSessionsQueryHandlerImpl backed by an in-memory StreamSessionRepository.
 */
public class ListChannelSessionsMockedTest extends ListChannelSessionsContractTest {

    private final Map<ChannelId, List<StreamSessionDto>> storage = new ConcurrentHashMap<>();
    private ListChannelSessionsQueryHandler handler;

    @BeforeEach
    void setUp() {
        storage.clear();

        StreamSessionRepository inMemoryRepository = new StreamSessionRepository() {
            @Override
            public Flux<StreamSessionDto> findSessionsByChannelId(ChannelId channelId) {
                List<StreamSessionDto> sessions = storage.getOrDefault(channelId, List.of());
                return Flux.fromIterable(sessions);
            }

            @Override
            public Mono<StreamSessionDto> findSessionById(SessionId sessionId) {
                return Flux.fromIterable(storage.values())
                        .flatMap(Flux::fromIterable)
                        .filter(s -> s.id().equals(sessionId))
                        .next();
            }

            @Override
            public Mono<StreamSessionDto> save(StreamSessionDto session) {
                storage.computeIfAbsent(session.channelId(), k -> new ArrayList<>()).add(session);
                return Mono.just(session);
            }
        };

        this.handler = new ListChannelSessionsQueryHandlerImpl(inMemoryRepository);
    }

    @Override
    protected ListChannelSessionsQueryHandler getHandler() {
        return this.handler;
    }

    @Override
    protected void setupSessionsForChannel(ChannelId channelId, List<StreamSessionDto> sessions) {
        storage.put(channelId, new ArrayList<>(sessions));
    }
}
