package com.ggar.stvr.streaming.api.exception;

import com.ggar.stvr.catalog.entities.ChannelUrl;
import com.ggar.stvr.streaming.entities.StreamQuality;

/**
 * Thrown when a requested live stream cannot be opened because the streamer is offline,
 * origin connection failed, or the requested quality is unavailable.
 */
public class StreamUnavailableException extends RuntimeException {

    private final ChannelUrl url;
    private final StreamQuality quality;

    public StreamUnavailableException(ChannelUrl url, StreamQuality quality, String reason) {
        super(String.format("Stream is unavailable for URL '%s' at quality '%s': %s",
                url != null ? url.asString() : "unknown",
                quality != null ? quality.value() : "unknown",
                reason));
        this.url = url;
        this.quality = quality;
    }

    public StreamUnavailableException(ChannelUrl url, StreamQuality quality, Throwable cause) {
        super(String.format("Stream is unavailable for URL '%s' at quality '%s': %s",
                url != null ? url.asString() : "unknown",
                quality != null ? quality.value() : "unknown",
                cause != null ? cause.getMessage() : "unknown error"), cause);
        this.url = url;
        this.quality = quality;
    }

    public ChannelUrl getUrl() {
        return url;
    }

    public StreamQuality getQuality() {
        return quality;
    }
}
