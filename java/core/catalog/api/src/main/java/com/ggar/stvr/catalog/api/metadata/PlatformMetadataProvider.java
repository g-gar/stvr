package com.ggar.stvr.catalog.api.metadata;

import com.ggar.stvr.catalog.entities.ChannelMetadata;
import com.ggar.stvr.catalog.entities.ChannelUrl;
import com.ggar.stvr.catalog.entities.Platform;
import reactor.core.publisher.Mono;

/**
 * Pluggable SPI port for fetching platform-specific channel metadata.
 * Submodules for specific platforms (e.g. Twitch Helix, YouTube Data API) implement
 * this interface parameterized with their specific strongly-typed ChannelMetadata subclass.
 *
 * @param <T> The concrete ChannelMetadata type provided by this platform provider.
 */
public interface PlatformMetadataProvider<T extends ChannelMetadata> {

    /**
     * Identifies the streaming platform handled by this provider.
     *
     * @return The supported Platform enum.
     */
    Platform supportedPlatform();

    /**
     * Determines whether this provider can inspect or handle the given channel URL.
     *
     * @param url The channel URL.
     * @return true if supported, false otherwise.
     */
    boolean supports(ChannelUrl url);

    /**
     * Asynchronously fetches metadata for the given channel URL.
     *
     * @param url The channel URL.
     * @return Mono emitting the strongly-typed metadata, or Mono.empty() if not found.
     */
    Mono<T> fetchMetadata(ChannelUrl url);
}
