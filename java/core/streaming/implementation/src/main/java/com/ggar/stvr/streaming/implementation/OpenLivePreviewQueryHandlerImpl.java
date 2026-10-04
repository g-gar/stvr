package com.ggar.stvr.streaming.implementation;

import com.ggar.stvr.streaming.api.LiveStreamProvider;
import com.ggar.stvr.streaming.api.OpenLivePreviewQueryHandler;
import com.ggar.stvr.streaming.api.exception.StreamUnavailableException;
import com.ggar.stvr.streaming.entities.StreamKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

/**
 * Implementation of UC-STR-01: Open Live Preview Stream.
 * Joins an existing multicast hub or opens a new stream connection via a matching LiveStreamProvider.
 */
@Service
public class OpenLivePreviewQueryHandlerImpl implements OpenLivePreviewQueryHandler {

    private static final Logger log = LoggerFactory.getLogger(OpenLivePreviewQueryHandlerImpl.class);

    private final StreamHubRegistry hubRegistry;
    private final List<LiveStreamProvider> providers;

    public OpenLivePreviewQueryHandlerImpl(
            StreamHubRegistry hubRegistry,
            List<LiveStreamProvider> providers
    ) {
        this.hubRegistry = Objects.requireNonNull(hubRegistry, "hubRegistry cannot be null");
        this.providers = providers != null ? List.copyOf(providers) : List.of();
    }

    @Override
    public Flux<byte[]> handle(OpenLivePreviewQuery query) {
        Objects.requireNonNull(query, "OpenLivePreviewQuery cannot be null");
        StreamKey streamKey = StreamKey.of(query.channelId(), query.quality());

        log.debug("User {} opening live preview for stream {} ({})",
                query.userId().value(), streamKey, query.url().asString());

        // Fast-path: attach to existing active hub if alive
        return Mono.defer(() -> Mono.justOrEmpty(hubRegistry.get(streamKey)))
                .filter(ChannelStreamHub::isAlive)
                .flatMapMany(hub -> hub.attachViewer(query.userId()))
                .switchIfEmpty(Flux.defer(() -> openNewHubAndAttach(streamKey, query)));
    }

    private Flux<byte[]> openNewHubAndAttach(StreamKey streamKey, OpenLivePreviewQuery query) {
        LiveStreamProvider provider = providers.stream()
                .filter(p -> p.supports(query.url()))
                .findFirst()
                .orElseThrow(() -> new StreamUnavailableException(
                        query.url(),
                        query.quality(),
                        "No registered LiveStreamProvider supports channel URL: " + query.url().asString()
                ));

        log.info("Opening new live stream connection for key {} using provider {}",
                streamKey, provider.getClass().getSimpleName());

        return provider.openStream(query.url(), query.quality())
                .onErrorMap(err -> err instanceof StreamUnavailableException
                        ? err
                        : new StreamUnavailableException(query.url(), query.quality(), err))
                .flatMapMany(connection -> {
                    ChannelStreamHub hub = hubRegistry.computeIfAbsent(
                            streamKey,
                            key -> new ChannelStreamHub(key, connection, hubRegistry::remove)
                    );
                    return hub.attachViewer(query.userId());
                });
    }
}
