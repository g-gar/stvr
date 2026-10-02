package com.ggar.stvr.catalog.entities;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object representing a StreamSession's unique identifier.
 */
public record SessionId(UUID value) {

    public SessionId {
        Objects.requireNonNull(value, "SessionId value cannot be null");
    }

    public static SessionId random() {
        return new SessionId(UUID.randomUUID());
    }

    public static SessionId fromString(String uuid) {
        return new SessionId(UUID.fromString(uuid));
    }

    public static SessionId of(UUID uuid) {
        return new SessionId(uuid);
    }

    public String asString() {
        return value.toString();
    }
}
