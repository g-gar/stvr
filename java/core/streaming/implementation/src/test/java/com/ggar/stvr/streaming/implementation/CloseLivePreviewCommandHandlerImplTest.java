package com.ggar.stvr.streaming.implementation;

import com.ggar.stvr.catalog.entities.ChannelId;
import com.ggar.stvr.identity.entities.UserId;
import com.ggar.stvr.streaming.api.CloseLivePreviewCommandHandler.CloseLivePreviewCommand;
import com.ggar.stvr.streaming.api.LiveStreamConnection;
import com.ggar.stvr.streaming.entities.StreamKey;
import com.ggar.stvr.streaming.entities.StreamQuality;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class CloseLivePreviewCommandHandlerImplTest {

    @Test
    @DisplayName("Should detach viewer on close command")
    void shouldDetachViewerOnCloseCommand() {
        StreamHubRegistry registry = new StreamHubRegistry();
        CloseLivePreviewCommandHandlerImpl handler = new CloseLivePreviewCommandHandlerImpl(registry);

        ChannelId channelId = ChannelId.random();
        StreamKey key = StreamKey.of(channelId, StreamQuality.BEST);

        LiveStreamConnection connection = new LiveStreamConnection() {
            @Override
            public Flux<byte[]> data() {
                return Flux.never();
            }

            @Override
            public boolean isAlive() {
                return true;
            }

            @Override
            public void cancel() {}
        };

        ChannelStreamHub hub = new ChannelStreamHub(key, connection, Duration.ofMinutes(1), registry::remove);
        registry.computeIfAbsent(key, k -> hub);

        UserId userId = UserId.random();
        hub.attachViewer(userId);
        assertThat(hub.getViewerCount()).isEqualTo(1);

        CloseLivePreviewCommand command = new CloseLivePreviewCommand(channelId, StreamQuality.BEST, userId);

        StepVerifier.create(handler.handle(command))
                .verifyComplete();

        assertThat(hub.getViewerCount()).isEqualTo(0);
    }
}
