package com.ggar.stvr.streaming.implementation;

import com.ggar.stvr.streaming.api.CloseLivePreviewCommandHandler;
import com.ggar.stvr.streaming.entities.StreamKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Implementation of UC-STR-02: Close Live Preview Stream.
 * Explicitly detaches an active viewer from the channel's multicast hub.
 */
@Service
public class CloseLivePreviewCommandHandlerImpl implements CloseLivePreviewCommandHandler {

    private static final Logger log = LoggerFactory.getLogger(CloseLivePreviewCommandHandlerImpl.class);

    private final StreamHubRegistry hubRegistry;

    public CloseLivePreviewCommandHandlerImpl(StreamHubRegistry hubRegistry) {
        this.hubRegistry = Objects.requireNonNull(hubRegistry, "hubRegistry cannot be null");
    }

    @Override
    public Mono<Void> handle(CloseLivePreviewCommand command) {
        Objects.requireNonNull(command, "CloseLivePreviewCommand cannot be null");
        StreamKey streamKey = StreamKey.of(command.channelId(), command.quality());

        log.debug("Explicitly closing live preview for user {} on stream {}",
                command.userId().value(), streamKey);

        hubRegistry.get(streamKey).ifPresent(hub -> hub.detachViewer(command.userId()));
        return Mono.empty();
    }
}
