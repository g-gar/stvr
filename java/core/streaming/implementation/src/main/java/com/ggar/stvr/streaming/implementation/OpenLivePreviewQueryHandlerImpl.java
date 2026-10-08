package com.ggar.stvr.streaming.implementation;

import com.ggar.stvr.streaming.api.LiveStreamProvider;
import com.ggar.stvr.streaming.api.OpenLivePreviewQueryHandler;
import com.ggar.stvr.streaming.api.StreamingMetricsPublisher;
import com.ggar.stvr.streaming.api.exception.StreamUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;

/**
 * Implementation of UC-STR-01: Open Live Preview Stream.
 * Pure stateless reactive pass-through that locates a matching {@link LiveStreamProvider} SPI,
 * acquires the stream connection, emits visitor/quota metrics via {@link StreamingMetricsPublisher},
 * and safely cancels the connection when the consumer disconnects.
 */
@Service
public class OpenLivePreviewQueryHandlerImpl implements OpenLivePreviewQueryHandler {

    private static final Logger log = LoggerFactory.getLogger(OpenLivePreviewQueryHandlerImpl.class);

    private final List<LiveStreamProvider> providers;
    private final StreamingMetricsPublisher metricsPublisher;

    public OpenLivePreviewQueryHandlerImpl(
            List<LiveStreamProvider> providers,
            StreamingMetricsPublisher metricsPublisher
    ) {
        this.providers = providers != null ? List.copyOf(providers) : List.of();
        this.metricsPublisher = Objects.requireNonNull(metricsPublisher, "metricsPublisher cannot be null");
    }

    @Override
    public Flux<byte[]> handle(OpenLivePreviewQuery query) {
        Objects.requireNonNull(query, "OpenLivePreviewQuery cannot be null");

        return Flux.defer(() -> {
            LiveStreamProvider provider = providers.stream()
                    .filter(p -> p.supports(query.url()))
                    .findFirst()
                    .orElseThrow(() -> new StreamUnavailableException(
                            query.url(),
                            query.quality(),
                            "No registered LiveStreamProvider supports channel URL: " + query.url().asString()
                    ));

            log.debug("Opening stateless live stream for channel {} ({}) via provider {}",
                    query.channelId().value(), query.url().asString(), provider.getClass().getSimpleName());

            return provider.openStream(query.url(), query.quality())
                    .onErrorMap(err -> err instanceof StreamUnavailableException
                            ? err
                            : new StreamUnavailableException(query.url(), query.quality(), err))
                    .flatMapMany(connection -> connection.data()
                            .doOnSubscribe(subscription ->
                                    metricsPublisher.emitVisitorIncreaseMetric(query.channelId(), query.userId()))
                            .doOnNext(chunk ->
                                    metricsPublisher.emitQuotaMetric(query.channelId(), query.userId(), chunk.length))
                            .doFinally(signalType -> {
                                metricsPublisher.emitVisitorDecreaseMetric(query.channelId(), query.userId());
                                log.debug("Preview stream disconnected for channel {} (signal: {})",
                                        query.channelId().value(), signalType);
                                connection.cancel();
                            }));
        });
    }
}
