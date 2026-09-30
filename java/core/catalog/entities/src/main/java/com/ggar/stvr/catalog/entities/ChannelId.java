package com.ggar.stvr.catalog.entities;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object representing a Channel's unique identifier.
 */
public record ChannelId(UUID value) {

    public ChannelId {
        Objects.requireNonNull(value, "ChannelId value cannot be null");
    }

    public static ChannelId random() {
        return new ChannelId(UUID.randomUUID());
    }

    public static ChannelId fromString(String uuid) {
        return new ChannelId(UUID.fromString(uuid));
    }

    public static ChannelId of(UUID uuid) {
        return new ChannelId(uuid);
    }

    public String asString() {
        return value.toString();
    }
}
