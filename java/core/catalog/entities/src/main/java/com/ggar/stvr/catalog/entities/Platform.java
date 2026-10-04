package com.ggar.stvr.catalog.entities;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

/**
 * Value Object representing a streaming platform or video source (e.g. Twitch, YouTube, Kick, RTVE, etc.).
 * Fully agnostic, open, and extensible without hardcoded platform names.
 */
public record Platform(String value) implements Serializable {

    public Platform {
        Objects.requireNonNull(value, "Platform value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Platform value cannot be blank");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
    }

    public static Platform of(String value) {
        if (value == null || value.isBlank()) {
            return new Platform("custom");
        }
        return new Platform(value);
    }

    public String name() {
        return value.toUpperCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return value;
    }
}
