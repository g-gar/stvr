package com.ggar.stvr.identity.api;

import com.ggar.stvr.framework.cqrs.Command;
import com.ggar.stvr.framework.cqrs.CommandHandler;
import com.ggar.stvr.identity.api.exception.InvalidUsernameException;
import com.ggar.stvr.identity.entities.Role;
import com.ggar.stvr.identity.entities.UserId;
import com.ggar.stvr.identity.entities.Username;

import java.time.Instant;
import java.util.Objects;

/**
 * CQRS command handler interface for registering a new user (UC-AUTH-01).
 */
public interface RegisterUserCommandHandler
        extends CommandHandler<RegisterUserCommandHandler.RegisterUserCommand, RegisterUserCommandHandler.UserDto> {

    /**
     * Command representing the intent to register a new user in the system.
     *
     * @param username The validated username.
     * @param rawPassword The raw password provided by the user.
     * @param role The role to assign to the user.
     */
    record RegisterUserCommand(
            Username username,
            String rawPassword,
            Role role
    ) implements Command<UserDto> {

        public RegisterUserCommand {
            Objects.requireNonNull(username, "Username cannot be null");
            Objects.requireNonNull(rawPassword, "Raw password cannot be null");
            Objects.requireNonNull(role, "Role cannot be null");
        }

        public static RegisterUserCommand of(Username username, String rawPassword, Role role) {
            return new RegisterUserCommand(username, rawPassword, role);
        }

        public static RegisterUserCommand of(String rawUsername, String rawPassword, Role role) {
            if (rawUsername == null || rawUsername.isBlank()) {
                throw new InvalidUsernameException(rawUsername, "Username cannot be null or blank");
            }
            Username username;
            try {
                username = Username.of(rawUsername);
            } catch (Exception e) {
                throw new InvalidUsernameException(rawUsername, e);
            }
            return new RegisterUserCommand(username, rawPassword, role);
        }
    }

    /**
     * DTO representing a registered user without exposing sensitive credentials.
     */
    record UserDto(
            UserId id,
            Username username,
            Role role,
            Instant createdAt
    ) {
        public UserDto {
            Objects.requireNonNull(id, "UserId cannot be null");
            Objects.requireNonNull(username, "Username cannot be null");
            Objects.requireNonNull(role, "Role cannot be null");
            createdAt = createdAt != null ? createdAt : Instant.now();
        }

        public UserDto(UserId id, Username username, Role role) {
            this(id, username, role, Instant.now());
        }
    }
}
