package com.ggar.stvr.identity.entities;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object representing a User's unique identifier.
 */
public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, "UserId value cannot be null");
    }

    public static UserId random() {
        return new UserId(UUID.randomUUID());
    }

    public static UserId fromString(String uuid) {
        return new UserId(UUID.fromString(uuid));
    }

    public static UserId of(UUID uuid) {
        return new UserId(uuid);
    }

    public String asString() {
        return value.toString();
    }
}
