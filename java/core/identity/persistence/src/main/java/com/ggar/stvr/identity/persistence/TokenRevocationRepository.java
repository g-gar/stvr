package com.ggar.stvr.identity.persistence;

import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Reactive persistence repository port for storing and checking revoked tokens.
 */
public interface TokenRevocationRepository {

    /**
     * Records a token identifier (e.g. jti) as revoked until its expiration.
     *
     * @param tokenId The unique token identifier.
     * @param expiresAt The timestamp when the token naturally expires.
     * @return Mono completing when recorded.
     */
    Mono<Void> revoke(String tokenId, Instant expiresAt);

    /**
     * Checks if a token identifier has been recorded as revoked.
     *
     * @param tokenId The unique token identifier.
     * @return Mono emitting true if revoked, false otherwise.
     */
    Mono<Boolean> isRevoked(String tokenId);
}
