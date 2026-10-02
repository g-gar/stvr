package com.ggar.stvr.test.contracts.identity;

import com.ggar.stvr.identity.api.AuthenticateUserCommandHandler;
import com.ggar.stvr.identity.api.AuthenticateUserCommandHandler.AuthenticateUserCommand;
import com.ggar.stvr.identity.api.exception.BadCredentialsException;
import com.ggar.stvr.identity.api.exception.InvalidUsernameException;
import com.ggar.stvr.identity.api.security.PasswordEncoder;
import com.ggar.stvr.identity.api.token.TokenProvider;
import com.ggar.stvr.identity.entities.Role;
import com.ggar.stvr.identity.entities.User;
import com.ggar.stvr.identity.entities.UserId;
import com.ggar.stvr.identity.entities.Username;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Generic contract test suite for UC-AUTH-02: Authenticate User (Login).
 * Can be executed against mocked units, real database integration, or load test suites.
 */
public abstract class AuthenticateUserContractTest {

    protected abstract AuthenticateUserCommandHandler getHandler();

    protected abstract void registerUser(User user);

    protected abstract PasswordEncoder getPasswordEncoder();

    protected abstract TokenProvider getTokenProvider();

    @Test
    @DisplayName("Scenario: authenticate and generate token when credentials are valid")
    void shouldAuthenticateAndGenerateTokenWhenCredentialsAreValid() {
        Username username = Username.of("validStreamer");
        String rawPassword = "correctPassword123";
        String encodedHash = getPasswordEncoder().encode(rawPassword);

        User user = new User(UserId.random(), username, encodedHash, Role.USER);
        registerUser(user);

        AuthenticateUserCommand command = new AuthenticateUserCommand(username, rawPassword);

        StepVerifier.create(getHandler().handle(command))
                .assertNext(response -> {
                    assertThat(response.accessToken()).isNotBlank();
                    assertThat(response.tokenType()).isEqualTo("Bearer");
                    assertThat(response.expiresAt()).isAfter(Instant.now());

                    assertThat(response.user()).isNotNull();
                    assertThat(response.user().id()).isEqualTo(user.id());
                    assertThat(response.user().username()).isEqualTo(username);
                    assertThat(response.user().role()).isEqualTo(Role.USER);

                    // Verify token validity using token provider
                    assertThat(getTokenProvider().validateToken(response.accessToken())).isTrue();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Scenario: reject login when password is incorrect")
    void shouldRejectLoginWhenPasswordIsIncorrect() {
        Username username = Username.of("secureUser");
        String rawPassword = "rightPassword123";
        String encodedHash = getPasswordEncoder().encode(rawPassword);

        registerUser(new User(UserId.random(), username, encodedHash, Role.USER));

        AuthenticateUserCommand command = new AuthenticateUserCommand(username, "wrongPassword999");

        StepVerifier.create(getHandler().handle(command))
                .expectError(BadCredentialsException.class)
                .verify();
    }

    @Test
    @DisplayName("Scenario: reject login when user does not exist")
    void shouldRejectLoginWhenUserDoesNotExist() {
        Username nonExistentUser = Username.of("ghostUser");
        AuthenticateUserCommand command = new AuthenticateUserCommand(nonExistentUser, "anyPassword123");

        StepVerifier.create(getHandler().handle(command))
                .expectError(BadCredentialsException.class)
                .verify();
    }

    @Test
    @DisplayName("Scenario: reject invalid username syntax when building command")
    void shouldRejectInvalidUsernameSyntaxWhenBuildingCommand() {
        assertThatThrownBy(() -> AuthenticateUserCommand.of("ab", "password123"))
                .isInstanceOf(InvalidUsernameException.class);

        assertThatThrownBy(() -> AuthenticateUserCommand.of("invalid@user", "password123"))
                .isInstanceOf(InvalidUsernameException.class);

        assertThatThrownBy(() -> AuthenticateUserCommand.of("", "password123"))
                .isInstanceOf(InvalidUsernameException.class);

        assertThatThrownBy(() -> AuthenticateUserCommand.of((String) null, "password123"))
                .isInstanceOf(InvalidUsernameException.class);
    }
}
