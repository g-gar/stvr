package com.ggar.stvr.streaming.implementation;

import com.ggar.stvr.catalog.entities.ChannelId;
import com.ggar.stvr.catalog.entities.ChannelUrl;
import com.ggar.stvr.identity.entities.UserId;
import com.ggar.stvr.streaming.api.LiveStreamConnection;
import com.ggar.stvr.streaming.api.LiveStreamProvider;
import com.ggar.stvr.streaming.api.OpenLivePreviewQueryHandler.OpenLivePreviewQuery;
import com.ggar.stvr.streaming.api.exception.StreamUnavailableException;
import com.ggar.stvr.streaming.entities.StreamKey;
import com.ggar.stvr.streaming.entities.StreamQuality;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class OpenLivePreviewQueryHandlerImplTest {

    @Test
    @DisplayName("Should start new stream hub when no active hub exists, and reuse it for second user")
    void shouldStartAndReuseStreamHub() {
        StreamHubRegistry registry = new StreamHubRegistry();
        AtomicInteger providerCalls = new AtomicInteger(0);
        Sinks.Many<byte[]> upstreamSink = Sinks.many().multicast().onBackpressureBuffer();

        LiveStreamProvider provider = new LiveStreamProvider() {
            @Override
            public boolean supports(ChannelUrl url) {
                return true;
            }

            @Override
            public Mono<LiveStreamConnection> openStream(ChannelUrl url, StreamQuality quality) {
                providerCalls.incrementAndGet();
                return Mono.just(new LiveStreamConnection() {
                    @Override
                    public Flux<byte[]> data() {
                        return upstreamSink.asFlux();
                    }

                    @Override
                    public boolean isAlive() {
                        return true;
                    }

                    @Override
                    public void cancel() {}
                });
            }
        };

        OpenLivePreviewQueryHandlerImpl handler = new OpenLivePreviewQueryHandlerImpl(registry, List.of(provider));

        ChannelId channelId = ChannelId.random();
        ChannelUrl url = ChannelUrl.of("https://twitch.tv/streamer");
        UserId user1 = UserId.random();
        UserId user2 = UserId.random();

        OpenLivePreviewQuery query1 = new OpenLivePreviewQuery(channelId, url, StreamQuality.BEST, user1);
        OpenLivePreviewQuery query2 = new OpenLivePreviewQuery(channelId, url, StreamQuality.BEST, user2);

        Flux<byte[]> stream1 = handler.handle(query1);
        Flux<byte[]> stream2 = handler.handle(query2);

        byte[] chunk1 = new byte[]{1, 2};
        byte[] chunk2 = new byte[]{3, 4};

        // Check provider was only called ONCE for the hub
        StepVerifier.create(stream1.take(1))
                .then(() -> upstreamSink.tryEmitNext(chunk1))
                .expectNextMatches(b -> Arrays.equals(b, chunk1))
                .verifyComplete();

        StepVerifier.create(stream2.take(1))
                .then(() -> upstreamSink.tryEmitNext(chunk2))
                .expectNextMatches(b -> Arrays.equals(b, chunk2))
                .verifyComplete();

        assertThat(providerCalls.get()).isEqualTo(1);
        assertThat(registry.activeHubCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should create independent hubs when different qualities are requested for same channel")
    void shouldCreateIndependentHubsForDifferentQualities() {
        StreamHubRegistry registry = new StreamHubRegistry();
        AtomicInteger providerCalls = new AtomicInteger(0);
        ConcurrentMap<String, Sinks.Many<byte[]>> sinks = new ConcurrentHashMap<>();

        LiveStreamProvider provider = new LiveStreamProvider() {
            @Override
            public boolean supports(ChannelUrl url) {
                return true;
            }

            @Override
            public Mono<LiveStreamConnection> openStream(ChannelUrl url, StreamQuality quality) {
                providerCalls.incrementAndGet();
                Sinks.Many<byte[]> sink = Sinks.many().multicast().onBackpressureBuffer();
                sinks.put(quality.value(), sink);

                return Mono.just(new LiveStreamConnection() {
                    @Override
                    public Flux<byte[]> data() {
                        return sink.asFlux();
                    }

                    @Override
                    public boolean isAlive() {
                        return true;
                    }

                    @Override
                    public void cancel() {}
                });
            }
        };

        OpenLivePreviewQueryHandlerImpl handler = new OpenLivePreviewQueryHandlerImpl(registry, List.of(provider));

        ChannelId channelId = ChannelId.random();
        ChannelUrl url = ChannelUrl.of("https://twitch.tv/streamer");

        OpenLivePreviewQuery query1080p = new OpenLivePreviewQuery(channelId, url, StreamQuality.of("1080p"), UserId.random());
        OpenLivePreviewQuery query720p = new OpenLivePreviewQuery(channelId, url, StreamQuality.of("720p"), UserId.random());

        Flux<byte[]> stream1080p = handler.handle(query1080p);
        Flux<byte[]> stream720p = handler.handle(query720p);

        byte[] chunk1080 = new byte[]{10, 80};
        byte[] chunk720 = new byte[]{7, 20};

        StepVerifier.create(stream1080p.take(1))
                .then(() -> sinks.get("1080p").tryEmitNext(chunk1080))
                .expectNextMatches(b -> Arrays.equals(b, chunk1080))
                .verifyComplete();

        StepVerifier.create(stream720p.take(1))
                .then(() -> sinks.get("720p").tryEmitNext(chunk720))
                .expectNextMatches(b -> Arrays.equals(b, chunk720))
                .verifyComplete();

        assertThat(providerCalls.get()).isEqualTo(2);
        assertThat(registry.activeHubCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should throw StreamUnavailableException when no provider supports URL")
    void shouldThrowWhenNoProviderSupportsUrl() {
        StreamHubRegistry registry = new StreamHubRegistry();
        OpenLivePreviewQueryHandlerImpl handler = new OpenLivePreviewQueryHandlerImpl(registry, List.of());

        OpenLivePreviewQuery query = new OpenLivePreviewQuery(
                ChannelId.random(),
                ChannelUrl.of("https://unknown.com/stream"),
                StreamQuality.BEST,
                UserId.random()
        );

        StepVerifier.create(handler.handle(query))
                .expectError(StreamUnavailableException.class)
                .verify();
    }
}
