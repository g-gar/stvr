package com.ggar.stvr.test.contracts.catalog;

import com.ggar.stvr.catalog.api.GetStreamSessionQueryHandler;
import com.ggar.stvr.catalog.api.GetStreamSessionQueryHandler.GetStreamSessionQuery;
import com.ggar.stvr.catalog.api.ListChannelSessionsQueryHandler.StreamSessionDto;
import com.ggar.stvr.catalog.api.exception.StreamSessionNotFoundException;
import com.ggar.stvr.catalog.entities.ChannelId;
import com.ggar.stvr.catalog.entities.SessionId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Generic contract test suite for Get Stream Session.
 */
public abstract class GetStreamSessionContractTest {

    protected abstract GetStreamSessionQueryHandler getHandler();

    /**
     * Prepares a stream session in the underlying storage or mock environment.
     *
     * @param session Stream session to store
     */
    protected abstract void setupSession(StreamSessionDto session);

    @Test
    @DisplayName("Scenario: return existing stream session by ID")
    void shouldReturnSessionWhenFound() {
        SessionId sessionId = SessionId.random();
        ChannelId channelId = ChannelId.random();
        Instant now = Instant.now();

        StreamSessionDto session = new StreamSessionDto(
                sessionId,
                channelId,
                "Championship Final",
                "Esports",
                List.of("final", "tournament"),
                now.minusSeconds(1800),
                null,
                true,
                Map.of()
        );

        setupSession(session);

        StepVerifier.create(getHandler().handle(new GetStreamSessionQuery(sessionId)))
                .assertNext(s -> {
                    assertThat(s.id()).isEqualTo(sessionId);
                    assertThat(s.channelId()).isEqualTo(channelId);
                    assertThat(s.title()).isEqualTo("Championship Final");
                    assertThat(s.category()).isEqualTo("Esports");
                    assertThat(s.active()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Scenario: throw StreamSessionNotFoundException when session does not exist")
    void shouldThrowWhenSessionNotFound() {
        SessionId nonExistent = SessionId.random();

        StepVerifier.create(getHandler().handle(new GetStreamSessionQuery(nonExistent)))
                .expectError(StreamSessionNotFoundException.class)
                .verify();
    }
}
