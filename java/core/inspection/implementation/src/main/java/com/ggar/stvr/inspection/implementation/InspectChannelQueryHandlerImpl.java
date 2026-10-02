package com.ggar.stvr.inspection.implementation;

import com.ggar.stvr.inspection.api.InspectChannelQueryHandler;
import com.ggar.stvr.inspection.api.exception.UnsupportedPlatformException;
import com.ggar.stvr.inspection.api.resolver.ChannelResolution;
import com.ggar.stvr.inspection.api.resolver.ChannelResolver;
import com.ggar.stvr.inspection.api.resolver.ChannelResolverFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Optional;

/**
 * Implementation of InspectChannelQueryHandler for resolving channel static identity from stream URL.
 */
@Service
public class InspectChannelQueryHandlerImpl implements InspectChannelQueryHandler {

    private final ChannelResolverFactory resolverFactory;

    public InspectChannelQueryHandlerImpl(ChannelResolverFactory resolverFactory) {
        this.resolverFactory = Objects.requireNonNull(resolverFactory, "ChannelResolverFactory cannot be null");
    }

    @Override
    public Mono<ChannelResolution> handle(InspectChannelQuery query) {
        Objects.requireNonNull(query, "InspectChannelQuery cannot be null");

        Optional<ChannelResolver> resolverOpt = resolverFactory.getResolver(query.url());
        if (resolverOpt.isEmpty()) {
            return Mono.error(new UnsupportedPlatformException(query.url()));
        }

        return resolverOpt.get().resolve(query.url())
                .switchIfEmpty(Mono.error(new UnsupportedPlatformException(query.url())));
    }
}
