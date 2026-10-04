package com.ggar.stvr.streaming.api;

import com.ggar.stvr.catalog.entities.ChannelUrl;
import com.ggar.stvr.streaming.entities.StreamQuality;
import reactor.core.publisher.Mono;

/**
 * SPI Port implemented by streaming backend plugins (e.g. plugins:streamlink)
 * to open and capture live video streams.
 */
public interface LiveStreamProvider {

    /**
     * Checks if this provider supports the given channel URL.
     *
     * @param url channel URL
     * @return true if supported, false otherwise
     */
    boolean supports(ChannelUrl url);

    /**
     * Opens a live stream session for the specified URL and quality.
     *
     * @param url target channel URL
     * @param quality requested video quality
     * @return Mono emitting an active LiveStreamConnection
     */
    Mono<LiveStreamConnection> openStream(ChannelUrl url, StreamQuality quality);
}
