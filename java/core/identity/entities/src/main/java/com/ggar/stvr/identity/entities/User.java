package com.ggar.stvr.identity.entities;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain entity representing an authenticated user account.
 */
public record User(
        UserId id,
        Username username,
        String passwordHash,
        Role role,
        Instant createdAt
) {
    public User {
        Objects.requireNonNull(id, "UserId cannot be null");
        Objects.requireNonNull(username, "Username cannot be null");
        Objects.requireNonNull(passwordHash, "Password hash cannot be null");
        Objects.requireNonNull(role, "Role cannot be null");
        createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public User(UserId id, Username username, String passwordHash, Role role) {
        this(id, username, passwordHash, role, Instant.now());
    }
}
