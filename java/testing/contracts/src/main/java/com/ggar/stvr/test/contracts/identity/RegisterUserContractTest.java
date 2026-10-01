package com.ggar.stvr.test.contracts.identity;

import com.ggar.stvr.identity.api.RegisterUserCommandHandler;
import com.ggar.stvr.identity.api.RegisterUserCommandHandler.RegisterUserCommand;
import com.ggar.stvr.identity.api.exception.InvalidUsernameException;
import com.ggar.stvr.identity.api.exception.UsernameAlreadyTakenException;
import com.ggar.stvr.identity.api.exception.WeakPasswordException;
import com.ggar.stvr.identity.api.security.PasswordEncoder;
import com.ggar.stvr.identity.entities.Role;
import com.ggar.stvr.identity.entities.User;
import com.ggar.stvr.identity.entities.Username;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Generic contract test suite for UC-AUTH-01: Register User.
 * Can be executed against mocked units, real database integration, or load test suites.
 */
public abstract class RegisterUserContractTest {

    protected abstract RegisterUserCommandHandler getHandler();

    protected abstract boolean userExistsInStorage(Username username);

    protected abstract Optional<User> findUserInStorage(Username username);

    protected abstract PasswordEncoder getPasswordEncoder();

    @Test
    @DisplayName("Scenario: register user successfully with hashed password")
    void shouldRegisterUserSuccessfullyWithHashedPassword() {
        Username username = Username.of("streamer123");
        String rawPassword = "superSecretPassword123";
        Role role = Role.USER;

        RegisterUserCommand command = new RegisterUserCommand(username, rawPassword, role);

        StepVerifier.create(getHandler().handle(command))
                .assertNext(dto -> {
                    assertThat(dto.id()).isNotNull();
                    assertThat(dto.username()).isEqualTo(username);
                    assertThat(dto.role()).isEqualTo(role);
                    assertThat(dto.createdAt()).isNotNull();
                })
                .verifyComplete();

        assertThat(userExistsInStorage(username)).isTrue();

        Optional<User> stored = findUserInStorage(username);
        assertThat(stored).isPresent();
        User user = stored.get();
        assertThat(user.username()).isEqualTo(username);
        assertThat(user.role()).isEqualTo(role);
        assertThat(user.passwordHash()).isNotEqualTo(rawPassword);
        assertThat(user.passwordHash()).startsWith("$2");
        assertThat(getPasswordEncoder().matches(rawPassword, user.passwordHash())).isTrue();
    }

    @Test
    @DisplayName("Scenario: throw exception when username is already taken")
    void shouldThrowExceptionWhenUsernameAlreadyTaken() {
        Username username = Username.of("existingUser");
        String rawPassword = "password12345";
        Role role = Role.USER;

        // 1. Initial registration
        StepVerifier.create(getHandler().handle(new RegisterUserCommand(username, rawPassword, role)))
                .expectNextCount(1)
                .verifyComplete();

        // 2. Duplicate registration attempt
        StepVerifier.create(getHandler().handle(new RegisterUserCommand(username, "anotherPassword99", Role.ADMIN)))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(UsernameAlreadyTakenException.class);
                    UsernameAlreadyTakenException ex = (UsernameAlreadyTakenException) error;
                    assertThat(ex.getUsername()).isEqualTo(username);
                })
                .verify();
    }

    @Test
    @DisplayName("Scenario: reject weak password under minimum length")
    void shouldRejectWeakPassword() {
        Username username = Username.of("validUser");

        // Passwords with length < 8
        StepVerifier.create(getHandler().handle(new RegisterUserCommand(username, "short", Role.USER)))
                .expectError(WeakPasswordException.class)
                .verify();

        StepVerifier.create(getHandler().handle(new RegisterUserCommand(username, "1234567", Role.USER)))
                .expectError(WeakPasswordException.class)
                .verify();

        StepVerifier.create(getHandler().handle(new RegisterUserCommand(username, "", Role.USER)))
                .expectError(WeakPasswordException.class)
                .verify();

        assertThat(userExistsInStorage(username)).isFalse();
    }

    @Test
    @DisplayName("Scenario: reject invalid username syntax when building command")
    void shouldRejectInvalidUsernameSyntax() {
        // Less than 3 chars
        assertThatThrownBy(() -> RegisterUserCommand.of("ab", "password123", Role.USER))
                .isInstanceOf(InvalidUsernameException.class);

        // Non-alphanumeric chars
        assertThatThrownBy(() -> RegisterUserCommand.of("user@name", "password123", Role.USER))
                .isInstanceOf(InvalidUsernameException.class);

        // Blank username
        assertThatThrownBy(() -> RegisterUserCommand.of("   ", "password123", Role.USER))
                .isInstanceOf(InvalidUsernameException.class);

        // Null username
        assertThatThrownBy(() -> RegisterUserCommand.of((String) null, "password123", Role.USER))
                .isInstanceOf(InvalidUsernameException.class);
    }
}
