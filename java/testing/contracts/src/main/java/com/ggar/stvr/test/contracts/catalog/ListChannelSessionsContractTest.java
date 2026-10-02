package com.ggar.stvr.test.contracts.catalog;

import com.ggar.stvr.catalog.api.ListChannelSessionsQueryHandler;
import com.ggar.stvr.catalog.api.ListChannelSessionsQueryHandler.ListChannelSessionsQuery;
import com.ggar.stvr.catalog.api.ListChannelSessionsQueryHandler.StreamSessionDto;
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
 * Generic contract test suite for List Channel Sessions.
 */
public abstract class ListChannelSessionsContractTest {

    protected abstract ListChannelSessionsQueryHandler getHandler();

    /**
     * Prepares stream sessions for a given channel in the underlying storage or mock environment.
     *
     * @param channelId Channel identifier
     * @param sessions  Stream sessions to associate with this channel
     */
    protected abstract void setupSessionsForChannel(ChannelId channelId, List<StreamSessionDto> sessions);

    @Test
    @DisplayName("Scenario: return all sessions for channel sorted by startedAt descending (newest first)")
    void shouldReturnSessionsSortedByStartedAtDescending() {
        ChannelId channelId = ChannelId.random();
        Instant now = Instant.now();

        StreamSessionDto oldest = new StreamSessionDto(
                SessionId.random(),
                channelId,
                "Oldest Stream",
                "Gaming",
                List.of("retro"),
                now.minusSeconds(7200),
                now.minusSeconds(3600),
                false,
                Map.of()
        );

        StreamSessionDto middle = new StreamSessionDto(
                SessionId.random(),
                channelId,
                "Yesterday Stream",
                "Just Chatting",
                List.of("chat"),
                now.minusSeconds(3600),
                now.minusSeconds(1800),
                false,
                Map.of()
        );

        StreamSessionDto activeLatest = new StreamSessionDto(
                SessionId.random(),
                channelId,
                "Current Live Stream",
                "Software & Game Development",
                List.of("dev", "java"),
                now.minusSeconds(600),
                null,
                true,
                Map.of()
        );

        // Another channel's session to ensure isolation
        ChannelId otherChannelId = ChannelId.random();
        StreamSessionDto otherSession = new StreamSessionDto(
                SessionId.random(),
                otherChannelId,
                "Other Channel Stream",
                "Music",
                List.of("guitar"),
                now.minusSeconds(100),
                null,
                true,
                Map.of()
        );

        // Arrange (intentionally unordered)
        setupSessionsForChannel(channelId, List.of(oldest, activeLatest, middle));
        setupSessionsForChannel(otherChannelId, List.of(otherSession));

        // Act & Assert: Order should be activeLatest -> middle -> oldest
        StepVerifier.create(getHandler().handle(new ListChannelSessionsQuery(channelId)))
                .assertNext(s -> {
                    assertThat(s.id()).isEqualTo(activeLatest.id());
                    assertThat(s.title()).isEqualTo("Current Live Stream");
                    assertThat(s.active()).isTrue();
                })
                .assertNext(s -> {
                    assertThat(s.id()).isEqualTo(middle.id());
                    assertThat(s.title()).isEqualTo("Yesterday Stream");
                    assertThat(s.active()).isFalse();
                })
                .assertNext(s -> {
                    assertThat(s.id()).isEqualTo(oldest.id());
                    assertThat(s.title()).isEqualTo("Oldest Stream");
                    assertThat(s.active()).isFalse();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Scenario: return empty stream when channel has no sessions")
    void shouldReturnEmptyWhenNoSessions() {
        ChannelId emptyChannel = ChannelId.random();

        StepVerifier.create(getHandler().handle(new ListChannelSessionsQuery(emptyChannel)))
                .verifyComplete();
    }
}
