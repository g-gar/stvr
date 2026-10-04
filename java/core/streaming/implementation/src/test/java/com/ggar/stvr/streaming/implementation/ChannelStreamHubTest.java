package com.ggar.stvr.streaming.implementation;

import com.ggar.stvr.catalog.entities.ChannelId;
import com.ggar.stvr.identity.entities.UserId;
import com.ggar.stvr.streaming.api.LiveStreamConnection;
import com.ggar.stvr.streaming.entities.StreamKey;
import com.ggar.stvr.streaming.entities.StreamQuality;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ChannelStreamHubTest {

    @Test
    @DisplayName("Should multicast stream byte chunks to multiple subscribers")
    void shouldMulticastChunksToMultipleSubscribers() {
        StreamKey key = StreamKey.of(ChannelId.random(), StreamQuality.BEST);
        Sinks.Many<byte[]> upstreamSink = Sinks.many().multicast().onBackpressureBuffer();
        AtomicBoolean cancelled = new AtomicBoolean(false);

        LiveStreamConnection connection = new LiveStreamConnection() {
            @Override
            public Flux<byte[]> data() {
                return upstreamSink.asFlux();
            }

            @Override
            public boolean isAlive() {
                return !cancelled.get();
            }

            @Override
            public void cancel() {
                cancelled.set(true);
            }
        };

        ChannelStreamHub hub = new ChannelStreamHub(key, connection, k -> {});

        UserId user1 = UserId.random();
        UserId user2 = UserId.random();

        Flux<byte[]> stream1 = hub.attachViewer(user1);
        Flux<byte[]> stream2 = hub.attachViewer(user2);

        assertThat(hub.getViewerCount()).isEqualTo(2);

        byte[] chunk1 = new byte[]{1, 2, 3};
        byte[] chunk2 = new byte[]{4, 5, 6};

        StepVerifier.create(stream1.take(2))
                .then(() -> {
                    upstreamSink.tryEmitNext(chunk1);
                    upstreamSink.tryEmitNext(chunk2);
                })
                .expectNext(chunk1)
                .expectNext(chunk2)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should decrement viewer count on cancel and trigger grace period shutdown when 0 viewers")
    void shouldShutdownWhenZeroViewersAfterGracePeriod() throws InterruptedException {
        StreamKey key = StreamKey.of(ChannelId.random(), StreamQuality.BEST);
        Sinks.Many<byte[]> upstreamSink = Sinks.many().multicast().onBackpressureBuffer();
        AtomicBoolean cancelled = new AtomicBoolean(false);
        AtomicBoolean onShutdownCalled = new AtomicBoolean(false);

        LiveStreamConnection connection = new LiveStreamConnection() {
            @Override
            public Flux<byte[]> data() {
                return upstreamSink.asFlux();
            }

            @Override
            public boolean isAlive() {
                return !cancelled.get();
            }

            @Override
            public void cancel() {
                cancelled.set(true);
            }
        };

        Duration shortGracePeriod = Duration.ofMillis(100);
        ChannelStreamHub hub = new ChannelStreamHub(key, connection, shortGracePeriod, k -> onShutdownCalled.set(true));

        UserId user1 = UserId.random();
        Flux<byte[]> stream = hub.attachViewer(user1);
        assertThat(hub.getViewerCount()).isEqualTo(1);

        // Cancel viewer subscription
        hub.detachViewer(user1);
        assertThat(hub.getViewerCount()).isEqualTo(0);

        // Wait slightly longer than grace period
        Thread.sleep(250);

        assertThat(hub.isAlive()).isFalse();
        assertThat(cancelled.get()).isTrue();
        assertThat(onShutdownCalled.get()).isTrue();
    }

    @Test
    @DisplayName("Should cancel grace period shutdown if another viewer joins before timer expires")
    void shouldCancelGracePeriodIfViewerJoins() throws InterruptedException {
        StreamKey key = StreamKey.of(ChannelId.random(), StreamQuality.BEST);
        Sinks.Many<byte[]> upstreamSink = Sinks.many().multicast().onBackpressureBuffer();
        AtomicBoolean cancelled = new AtomicBoolean(false);

        LiveStreamConnection connection = new LiveStreamConnection() {
            @Override
            public Flux<byte[]> data() {
                return upstreamSink.asFlux();
            }

            @Override
            public boolean isAlive() {
                return !cancelled.get();
            }

            @Override
            public void cancel() {
                cancelled.set(true);
            }
        };

        Duration gracePeriod = Duration.ofMillis(300);
        ChannelStreamHub hub = new ChannelStreamHub(key, connection, gracePeriod, k -> {});

        UserId user1 = UserId.random();
        UserId user2 = UserId.random();

        hub.attachViewer(user1);
        hub.detachViewer(user1);
        assertThat(hub.getViewerCount()).isEqualTo(0);

        // Viewer 2 joins before 300ms
        Thread.sleep(100);
        hub.attachViewer(user2);
        assertThat(hub.getViewerCount()).isEqualTo(1);

        // Wait past original 300ms
        Thread.sleep(300);

        assertThat(hub.isAlive()).isTrue();
        assertThat(cancelled.get()).isFalse();
    }
}


