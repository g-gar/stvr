package com.ggar.stvr.inspection.api.resolver;

import com.ggar.stvr.catalog.entities.ChannelUrl;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Composite implementation of ChannelResolverFactory delegating to an ordered list of resolvers.
 */
public class CompositeChannelResolverFactory implements ChannelResolverFactory {

    private final List<ChannelResolver> resolvers;

    public CompositeChannelResolverFactory(List<ChannelResolver> resolvers) {
        this.resolvers = resolvers != null ? List.copyOf(resolvers) : Collections.emptyList();
    }

    @Override
    public Optional<ChannelResolver> getResolver(ChannelUrl url) {
        Objects.requireNonNull(url, "ChannelUrl cannot be null");
        return resolvers.stream()
                .filter(resolver -> resolver.supports(url))
                .findFirst();
    }
}
