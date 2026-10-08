package com.ggar.stvr.streaming.api;

import com.ggar.stvr.catalog.entities.ChannelId;
import com.ggar.stvr.catalog.entities.ChannelUrl;
import com.ggar.stvr.framework.cqrs.Query;
import com.ggar.stvr.framework.cqrs.QueryHandler;
import com.ggar.stvr.identity.entities.UserId;
import com.ggar.stvr.streaming.entities.StreamQuality;
import reactor.core.publisher.Flux;

import java.util.Objects;

/**
 * CQRS query handler interface for UC-STR-01: Open Live Preview Stream.
 * Connects to a matching {@link LiveStreamProvider} SPI, returning a continuous reactive flux of video bytes,
 * while publishing visitor and quota metrics for the session.
 */
public interface OpenLivePreviewQueryHandler
        extends QueryHandler<OpenLivePreviewQueryHandler.OpenLivePreviewQuery, byte[]> {

    @Override
    Flux<byte[]> handle(OpenLivePreviewQuery query);

    record OpenLivePreviewQuery(
            ChannelId channelId,
            ChannelUrl url,
            StreamQuality quality,
            UserId userId
    ) implements Query<byte[]> {
        public OpenLivePreviewQuery {
            Objects.requireNonNull(channelId, "channelId cannot be null");
            Objects.requireNonNull(url, "url cannot be null");
            quality = quality != null ? quality : StreamQuality.BEST;
        }

        public OpenLivePreviewQuery(ChannelId channelId, ChannelUrl url, UserId userId) {
            this(channelId, url, StreamQuality.BEST, userId);
        }

        public OpenLivePreviewQuery(ChannelId channelId, ChannelUrl url) {
            this(channelId, url, StreamQuality.BEST, null);
        }
    }
}
