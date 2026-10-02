package com.ggar.stvr.inspection.api;

import com.ggar.stvr.catalog.entities.ChannelUrl;
import com.ggar.stvr.framework.cqrs.Query;
import com.ggar.stvr.framework.cqrs.QueryHandler;
import com.ggar.stvr.inspection.api.resolver.ChannelResolution;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * CQRS query handler interface for inspecting a channel's static identity (platform, slug, name) from its stream URL.
 */
public interface InspectChannelQueryHandler
        extends QueryHandler<InspectChannelQueryHandler.InspectChannelQuery, ChannelResolution> {

    @Override
    Mono<ChannelResolution> handle(InspectChannelQuery query);

    record InspectChannelQuery(ChannelUrl url) implements Query<ChannelResolution> {
        public InspectChannelQuery {
            Objects.requireNonNull(url, "ChannelUrl cannot be null");
        }
    }
}
