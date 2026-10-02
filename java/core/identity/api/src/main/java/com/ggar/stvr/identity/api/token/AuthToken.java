package com.ggar.stvr.identity.api.token;

import java.time.Instant;
import java.util.Objects;

/**
 * Value Object representing an issued security authentication token.
 */
public record AuthToken(
        String value,
        String tokenType,
        Instant issuedAt,
        Instant expiresAt
) {
    public AuthToken {
        Objects.requireNonNull(value, "Token value cannot be null");
        tokenType = tokenType != null ? tokenType : "Bearer";
        issuedAt = issuedAt != null ? issuedAt : Instant.now();
        Objects.requireNonNull(expiresAt, "Token expiresAt cannot be null");
    }

    public static AuthToken bearer(String value, Instant expiresAt) {
        return new AuthToken(value, "Bearer", Instant.now(), expiresAt);
    }
}
