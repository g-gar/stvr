package com.ggar.stvr.test.contracts.identity;

import com.ggar.stvr.identity.api.RegisterUserCommandHandler.UserDto;
import com.ggar.stvr.identity.api.RevokeTokenCommandHandler;
import com.ggar.stvr.identity.api.RevokeTokenCommandHandler.RevokeTokenCommand;
import com.ggar.stvr.identity.api.exception.InvalidTokenException;
import com.ggar.stvr.identity.api.token.AuthToken;
import com.ggar.stvr.identity.api.token.TokenProvider;
import com.ggar.stvr.identity.entities.Role;
import com.ggar.stvr.identity.entities.UserId;
import com.ggar.stvr.identity.entities.Username;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Generic contract test suite for UC-AUTH-03: Revoke Token (Logout).
 * Can be executed against mocked units, real database integration, or load test suites.
 */
public abstract class RevokeTokenContractTest {

    protected abstract RevokeTokenCommandHandler getHandler();

    protected abstract TokenProvider getTokenProvider();

    @Test
    @DisplayName("Scenario: revoke token successfully and reject subsequent validations")
    void shouldRevokeTokenSuccessfullyAndRejectSubsequentValidations() {
        UserDto user = new UserDto(UserId.random(), Username.of("testStreamer"), Role.USER);
        AuthToken token = getTokenProvider().issueToken(user);

        // Precondition: token is valid
        assertThat(getTokenProvider().validateToken(token.value())).isTrue();

        // Revoke token
        StepVerifier.create(getHandler().handle(new RevokeTokenCommand(token.value())))
                .expectNext(true)
                .verifyComplete();

        // Postcondition: token is no longer valid
        assertThat(getTokenProvider().validateToken(token.value())).isFalse();
    }

    @Test
    @DisplayName("Scenario: reject revocation when token is blank or malformed")
    void shouldRejectRevocationWhenTokenIsBlankOrMalformed() {
        // Blank command
        assertThatThrownBy(() -> RevokeTokenCommand.of(""))
                .isInstanceOf(InvalidTokenException.class);

        assertThatThrownBy(() -> RevokeTokenCommand.of("   "))
                .isInstanceOf(InvalidTokenException.class);

        // Malformed token structure
        StepVerifier.create(getHandler().handle(new RevokeTokenCommand("invalid-jwt-token")))
                .expectError(InvalidTokenException.class)
                .verify();

        // Tampered token signature
        UserDto user = new UserDto(UserId.random(), Username.of("tamperUser"), Role.USER);
        AuthToken token = getTokenProvider().issueToken(user);
        String tamperedToken = token.value() + "corrupted";

        StepVerifier.create(getHandler().handle(new RevokeTokenCommand(tamperedToken)))
                .expectError(InvalidTokenException.class)
                .verify();
    }

    @Test
    @DisplayName("Scenario: handle already expired or invalid token gracefully")
    void shouldHandleAlreadyExpiredOrInvalidTokenGracefully() {
        // Validating an unrevoked but malformed token returns false
        assertThat(getTokenProvider().validateToken("corrupted.payload.signature")).isFalse();
        assertThat(getTokenProvider().validateToken("")).isFalse();
        assertThat(getTokenProvider().validateToken(null)).isFalse();
    }
}
