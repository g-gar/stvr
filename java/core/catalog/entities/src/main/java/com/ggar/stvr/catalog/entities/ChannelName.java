package com.ggar.stvr.catalog.entities;

import java.util.Objects;

/**
 * Value Object representing a Channel's display name.
 */
public record ChannelName(String value) {

    public ChannelName {
        Objects.requireNonNull(value, "ChannelName cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("ChannelName cannot be blank");
        }
    }

    public static ChannelName of(String value) {
        return new ChannelName(value);
    }
}
