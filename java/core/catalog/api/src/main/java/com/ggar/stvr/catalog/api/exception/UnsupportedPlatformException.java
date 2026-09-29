package com.ggar.stvr.catalog.api.exception;

import com.ggar.stvr.catalog.entities.ChannelUrl;

import java.util.Objects;

/**
 * Exception thrown when a channel URL cannot be resolved by any registered inspector
 * and no fallback custom name was provided.
 */
public class UnsupportedPlatformException extends RuntimeException {

    private final ChannelUrl url;

    public UnsupportedPlatformException(ChannelUrl url) {
        super("Unsupported platform or stream URL: " + (url != null ? url.asString() : "null"));
        this.url = Objects.requireNonNull(url, "ChannelUrl cannot be null");
    }

    public ChannelUrl getUrl() {
        return url;
    }
}
