package com.ggar.stvr.inspection.api.exception;

import com.ggar.stvr.catalog.entities.ChannelUrl;

import java.util.Objects;

/**
 * Exception thrown when a channel URL cannot be resolved or inspected by any registered plugin/resolver.
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
