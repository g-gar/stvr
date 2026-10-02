package com.ggar.stvr.identity.api;

import com.ggar.stvr.framework.cqrs.Command;
import com.ggar.stvr.framework.cqrs.CommandHandler;
import com.ggar.stvr.identity.api.exception.InvalidTokenException;

import java.util.Objects;

/**
 * CQRS command handler interface for revoking tokens / logging out (UC-AUTH-03).
 */
public interface RevokeTokenCommandHandler
        extends CommandHandler<RevokeTokenCommandHandler.RevokeTokenCommand, Boolean> {

    /**
     * Command representing the intent to revoke an active authentication token.
     *
     * @param token The raw token string to revoke.
     */
    record RevokeTokenCommand(String token) implements Command<Boolean> {

        public RevokeTokenCommand {
            Objects.requireNonNull(token, "Token cannot be null");
            if (token.isBlank()) {
                throw new InvalidTokenException(token, "Token cannot be blank");
            }
        }

        public static RevokeTokenCommand of(String token) {
            return new RevokeTokenCommand(token);
        }
    }
}
