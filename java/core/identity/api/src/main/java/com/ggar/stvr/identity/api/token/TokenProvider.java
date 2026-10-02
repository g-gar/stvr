package com.ggar.stvr.identity.api.token;

import com.ggar.stvr.identity.api.RegisterUserCommandHandler.UserDto;

import reactor.core.publisher.Mono;

/**
 * Service port interface for issuing, decoding, validating, and revoking authentication tokens.
 */
public interface TokenProvider {

    /**
     * Issues an authentication token for the given authenticated user.
     *
     * @param user The authenticated user details.
     * @return The issued AuthToken containing value, type, and expiration.
     */
    AuthToken issueToken(UserDto user);

    /**
     * Validates whether a token string is syntactically valid, has a correct signature, is unexpired, and not revoked.
     *
     * @param tokenValue The raw token string.
     * @return true if valid, unexpired, and not revoked; false otherwise.
     */
    boolean validateToken(String tokenValue);

    /**
     * Revokes an authentication token (e.g., on logout or account compromise).
     *
     * @param tokenValue The raw token string to revoke.
     * @return Mono completing when the token is marked as revoked.
     */
    Mono<Void> revokeToken(String tokenValue);
}

