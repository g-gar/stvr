package com.ggar.stvr.identity.api;

import com.ggar.stvr.framework.cqrs.Command;
import com.ggar.stvr.framework.cqrs.CommandHandler;
import com.ggar.stvr.identity.api.RegisterUserCommandHandler.UserDto;
import com.ggar.stvr.identity.api.exception.InvalidUsernameException;
import com.ggar.stvr.identity.entities.Username;

import java.time.Instant;
import java.util.Objects;

/**
 * CQRS command handler interface for authenticating users (UC-AUTH-02).
 */
public interface AuthenticateUserCommandHandler
        extends CommandHandler<AuthenticateUserCommandHandler.AuthenticateUserCommand, AuthenticateUserCommandHandler.AuthTokenResponse> {

    /**
     * Command representing the intent to authenticate with username and password.
     *
     * @param username The user's username.
     * @param password The plain-text password to verify.
     */
    record AuthenticateUserCommand(
            Username username,
            String password
    ) implements Command<AuthTokenResponse> {

        public AuthenticateUserCommand {
            Objects.requireNonNull(username, "Username cannot be null");
            Objects.requireNonNull(password, "Password cannot be null");
        }

        public static AuthenticateUserCommand of(Username username, String password) {
            return new AuthenticateUserCommand(username, password);
        }

        public static AuthenticateUserCommand of(String rawUsername, String password) {
            if (rawUsername == null || rawUsername.isBlank()) {
                throw new InvalidUsernameException(rawUsername, "Username cannot be null or blank");
            }
            Username username;
            try {
                username = Username.of(rawUsername);
            } catch (Exception e) {
                throw new InvalidUsernameException(rawUsername, e);
            }
            return new AuthenticateUserCommand(username, password);
        }
    }

    /**
     * Response payload containing the issued access token and authenticated user context.
     *
     * @param accessToken The issued JWT or bearer token string.
     * @param tokenType The token scheme, typically "Bearer".
     * @param expiresAt The timestamp when this token expires.
     * @param user The authenticated user summary.
     */
    record AuthTokenResponse(
            String accessToken,
            String tokenType,
            Instant expiresAt,
            UserDto user
    ) {
        public AuthTokenResponse {
            Objects.requireNonNull(accessToken, "AccessToken cannot be null");
            tokenType = tokenType != null ? tokenType : "Bearer";
            Objects.requireNonNull(expiresAt, "ExpiresAt cannot be null");
            Objects.requireNonNull(user, "User cannot be null");
        }
    }
}
