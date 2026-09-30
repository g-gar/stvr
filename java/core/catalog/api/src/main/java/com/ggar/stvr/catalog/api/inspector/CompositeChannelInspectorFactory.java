package com.ggar.stvr.catalog.api.inspector;

import com.ggar.stvr.catalog.entities.ChannelUrl;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Composite ChannelInspectorFactory that queries registered inspectors in order of precedence.
 */
public class CompositeChannelInspectorFactory implements ChannelInspectorFactory {

    private final List<ChannelInspector> inspectors;

    public CompositeChannelInspectorFactory(List<ChannelInspector> inspectors) {
        this.inspectors = inspectors != null ? List.copyOf(inspectors) : List.of();
    }

    public static CompositeChannelInspectorFactory of(ChannelInspector... inspectors) {
        return new CompositeChannelInspectorFactory(inspectors != null ? List.of(inspectors) : List.of());
    }

    @Override
    public Optional<ChannelInspector> getInspector(ChannelUrl url) {
        Objects.requireNonNull(url, "ChannelUrl cannot be null");
        return inspectors.stream()
                .filter(inspector -> inspector.supports(url))
                .findFirst();
    }
}
