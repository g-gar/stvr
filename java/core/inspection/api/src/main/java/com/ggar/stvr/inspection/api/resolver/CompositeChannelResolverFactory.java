package com.ggar.stvr.inspection.api.resolver;

import com.ggar.stvr.catalog.entities.ChannelUrl;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Composite implementation of ChannelResolverFactory delegating to an ordered list of resolvers.
 * Can be instantiated with a static list or a dynamic Supplier for hot-reloadable plugin architectures.
 */
public class CompositeChannelResolverFactory implements ChannelResolverFactory {

    private final Supplier<List<ChannelResolver>> resolverSupplier;

    public CompositeChannelResolverFactory(List<ChannelResolver> resolvers) {
        List<ChannelResolver> copy = resolvers != null ? List.copyOf(resolvers) : Collections.emptyList();
        this.resolverSupplier = () -> copy;
    }

    public CompositeChannelResolverFactory(Supplier<List<ChannelResolver>> resolverSupplier) {
        this.resolverSupplier = Objects.requireNonNull(resolverSupplier, "resolverSupplier cannot be null");
    }

    @Override
    public Optional<ChannelResolver> getResolver(ChannelUrl url) {
        Objects.requireNonNull(url, "ChannelUrl cannot be null");
        return resolverSupplier.get().stream()
                .filter(resolver -> resolver.supports(url))
                .findFirst();
    }
}
