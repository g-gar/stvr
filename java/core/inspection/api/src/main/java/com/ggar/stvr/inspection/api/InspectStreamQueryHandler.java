package com.ggar.stvr.inspection.api;

import com.ggar.stvr.catalog.entities.ChannelUrl;
import com.ggar.stvr.catalog.entities.Platform;
import com.ggar.stvr.framework.cqrs.Query;
import com.ggar.stvr.framework.cqrs.QueryHandler;
import com.ggar.stvr.inspection.entities.StreamInfo;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * CQRS query handler interface for inspecting an active stream (live status, title, qualities, telemetry).
 */
public interface InspectStreamQueryHandler
        extends QueryHandler<InspectStreamQueryHandler.InspectStreamQuery, StreamInfo> {

    @Override
    Mono<StreamInfo> handle(InspectStreamQuery query);

    record InspectStreamQuery(
            ChannelUrl url,
            Platform platform
    ) implements Query<StreamInfo> {
        public InspectStreamQuery {
            Objects.requireNonNull(url, "url cannot be null");
        }

        public InspectStreamQuery(ChannelUrl url) {
            this(url, null);
        }
    }
}
