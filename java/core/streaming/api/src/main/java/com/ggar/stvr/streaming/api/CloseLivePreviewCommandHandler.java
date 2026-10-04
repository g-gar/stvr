package com.ggar.stvr.streaming.api;

import com.ggar.stvr.catalog.entities.ChannelId;
import com.ggar.stvr.framework.cqrs.Command;
import com.ggar.stvr.framework.cqrs.CommandHandler;
import com.ggar.stvr.identity.entities.UserId;
import com.ggar.stvr.streaming.entities.StreamQuality;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * CQRS command handler interface for UC-STR-02: Close Live Preview Stream.
 * Explicitly detaches an active viewer from a channel's multicast hub.
 */
public interface CloseLivePreviewCommandHandler
        extends CommandHandler<CloseLivePreviewCommandHandler.CloseLivePreviewCommand, Void> {

    @Override
    Mono<Void> handle(CloseLivePreviewCommand command);

    record CloseLivePreviewCommand(
            ChannelId channelId,
            StreamQuality quality,
            UserId userId
    ) implements Command<Void> {
        public CloseLivePreviewCommand {
            Objects.requireNonNull(channelId, "channelId cannot be null");
            quality = quality != null ? quality : StreamQuality.BEST;
            Objects.requireNonNull(userId, "userId cannot be null");
        }

        public CloseLivePreviewCommand(ChannelId channelId, UserId userId) {
            this(channelId, StreamQuality.BEST, userId);
        }
    }
}
