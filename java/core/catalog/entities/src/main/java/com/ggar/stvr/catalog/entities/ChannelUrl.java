package com.ggar.stvr.catalog.entities;

import java.net.URI;
import java.util.Objects;

/**
 * Value Object representing a Channel's stream URI.
 */
public record ChannelUrl(URI value) {

    public ChannelUrl {
        Objects.requireNonNull(value, "ChannelUrl URI cannot be null");
        if (value.getScheme() == null || value.getHost() == null) {
            throw new IllegalArgumentException("ChannelUrl must have a valid scheme and host: " + value);
        }
    }

    public static ChannelUrl of(String url) {
        Objects.requireNonNull(url, "Url string cannot be null");
        String trimmed = url.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException("ChannelUrl cannot be blank");
        }
        if (!trimmed.contains("://")) {
            trimmed = "https://" + trimmed;
        }
        try {
            return new ChannelUrl(URI.create(trimmed));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid channel URL: " + url, e);
        }
    }

    public static ChannelUrl of(URI uri) {
        return new ChannelUrl(uri);
    }

    public String asString() {
        return value.toString();
    }
}
