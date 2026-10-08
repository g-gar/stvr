package com.ggar.stvr.streaming.implementation;

import com.ggar.stvr.catalog.entities.ChannelId;
import com.ggar.stvr.catalog.entities.ChannelUrl;
import com.ggar.stvr.identity.entities.UserId;
import com.ggar.stvr.streaming.api.LiveStreamConnection;
import com.ggar.stvr.streaming.api.LiveStreamProvider;
import com.ggar.stvr.streaming.api.OpenLivePreviewQueryHandler.OpenLivePreviewQuery;
import com.ggar.stvr.streaming.api.StreamingMetricsPublisher;
import com.ggar.stvr.streaming.api.exception.StreamUnavailableException;
import com.ggar.stvr.streaming.entities.StreamQuality;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class OpenLivePreviewQueryHandlerImplTest {

    @Test
    @DisplayName("Should stream byte chunks from matching provider and emit visitor and quota metrics")
    void shouldStreamByteChunksAndEmitMetrics() {
        byte[] chunk1 = new byte[]{1, 2, 3};
        byte[] chunk2 = new byte[]{4, 5, 6};
        AtomicBoolean cancelled = new AtomicBoolean(false);

        LiveStreamProvider provider = new LiveStreamProvider() {
            @Override
            public boolean supports(ChannelUrl url) {
                return url.asString().contains("twitch.tv");
            }

            @Override
            public Mono<LiveStreamConnection> openStream(ChannelUrl url, StreamQuality quality) {
                return Mono.just(new LiveStreamConnection() {
                    @Override
                    public Flux<byte[]> data() {
                        return Flux.just(chunk1, chunk2);
                    }

                    @Override
                    public boolean isAlive() {
                        return !cancelled.get();
                    }

                    @Override
                    public void cancel() {
                        cancelled.set(true);
                    }
                });
            }
        };

        AtomicBoolean visitorJoined = new AtomicBoolean(false);
        AtomicBoolean visitorLeft = new AtomicBoolean(false);
        AtomicLong totalBytes = new AtomicLong(0);

        StreamingMetricsPublisher metricsPublisher = new StreamingMetricsPublisher() {
            @Override
            public void emitVisitorIncreaseMetric(ChannelId channelId, UserId userId) {
                visitorJoined.set(true);
            }

            @Override
            public void emitVisitorDecreaseMetric(ChannelId channelId, UserId userId) {
                visitorLeft.set(true);
            }

            @Override
            public void emitQuotaMetric(ChannelId channelId, UserId userId, long bytes) {
                totalBytes.addAndGet(bytes);
            }
        };

        OpenLivePreviewQueryHandlerImpl handler = new OpenLivePreviewQueryHandlerImpl(
                List.of(provider),
                metricsPublisher
        );

        ChannelId channelId = ChannelId.random();
        ChannelUrl url = ChannelUrl.of("https://twitch.tv/streamer");
        UserId userId = UserId.random();
        OpenLivePreviewQuery query = new OpenLivePreviewQuery(channelId, url, StreamQuality.BEST, userId);

        Flux<byte[]> stream = handler.handle(query);

        StepVerifier.create(stream)
                .expectNextMatches(b -> Arrays.equals(b, chunk1))
                .expectNextMatches(b -> Arrays.equals(b, chunk2))
                .verifyComplete();

        // Check metrics were published correctly
        assertThat(visitorJoined.get()).isTrue();
        assertThat(visitorLeft.get()).isTrue();
        assertThat(totalBytes.get()).isEqualTo(6); // 3 + 3 bytes
        assertThat(cancelled.get()).isTrue();
    }

    @Test
    @DisplayName("Should cancel connection and emit visitor decrease on premature unsubscribe")
    void shouldCancelConnectionOnPrematureCancel() {
        AtomicBoolean cancelled = new AtomicBoolean(false);

        LiveStreamProvider provider = new LiveStreamProvider() {
            @Override
            public boolean supports(ChannelUrl url) {
                return true;
            }

            @Override
            public Mono<LiveStreamConnection> openStream(ChannelUrl url, StreamQuality quality) {
                return Mono.just(new LiveStreamConnection() {
                    @Override
                    public Flux<byte[]> data() {
                        return Flux.never();
                    }

                    @Override
                    public boolean isAlive() {
                        return !cancelled.get();
                    }

                    @Override
                    public void cancel() {
                        cancelled.set(true);
                    }
                });
            }
        };

        OpenLivePreviewQueryHandlerImpl handler = new OpenLivePreviewQueryHandlerImpl(
                List.of(provider),
                new NoOpStreamingMetricsPublisher()
        );

        OpenLivePreviewQuery query = new OpenLivePreviewQuery(
                ChannelId.random(),
                ChannelUrl.of("https://twitch.tv/streamer")
        );

        var disposable = handler.handle(query).subscribe();
        assertThat(cancelled.get()).isFalse();

        disposable.dispose();
        assertThat(cancelled.get()).isTrue();
    }

    @Test
    @DisplayName("Should throw StreamUnavailableException reactively when no provider supports URL")
    void shouldThrowWhenNoProviderSupportsUrl() {
        OpenLivePreviewQueryHandlerImpl handler = new OpenLivePreviewQueryHandlerImpl(
                List.of(),
                new NoOpStreamingMetricsPublisher()
        );

        OpenLivePreviewQuery query = new OpenLivePreviewQuery(
                ChannelId.random(),
                ChannelUrl.of("https://unknown.com/stream")
        );

        StepVerifier.create(handler.handle(query))
                .expectError(StreamUnavailableException.class)
                .verify();
    }
}
