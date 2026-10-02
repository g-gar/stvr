package com.ggar.stvr.inspection.api.resolver;

import com.ggar.stvr.catalog.entities.ChannelUrl;
import reactor.core.publisher.Mono;

/**
 * Domain port for resolving a stream URL into a recognized platform and channel identity.
 */
public interface ChannelResolver {

    /**
     * Determines whether this resolver supports resolving the provided channel URL.
     *
     * @param url The channel URL.
     * @return true if supported, false otherwise.
     */
    boolean supports(ChannelUrl url);

    /**
     * Asynchronously resolves the channel URL into its platform and identity.
     *
     * @param url The channel URL to resolve.
     * @return Mono emitting the resolution result, or Mono.empty() if unrecognized.
     */
    Mono<ChannelResolution> resolve(ChannelUrl url);
}
