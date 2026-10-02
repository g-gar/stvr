package com.ggar.stvr.inspection.api.resolver;

import com.ggar.stvr.catalog.entities.ChannelUrl;

import java.util.Optional;

/**
 * Factory port for obtaining a matching ChannelResolver for a given URL.
 */
public interface ChannelResolverFactory {

    /**
     * Finds the first resolver supporting the given channel URL.
     *
     * @param url The channel URL.
     * @return Optional containing the matching resolver, or empty if no resolver supports the URL.
     */
    Optional<ChannelResolver> getResolver(ChannelUrl url);
}
