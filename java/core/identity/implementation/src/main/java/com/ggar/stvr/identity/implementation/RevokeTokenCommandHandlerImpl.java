package com.ggar.stvr.identity.implementation;

import com.ggar.stvr.identity.api.RevokeTokenCommandHandler;
import com.ggar.stvr.identity.api.token.TokenProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Reactive CQRS handler implementation for UC-AUTH-03: Revoke Token (Logout).
 */
@Service
public class RevokeTokenCommandHandlerImpl implements RevokeTokenCommandHandler {

    private final TokenProvider tokenProvider;

    public RevokeTokenCommandHandlerImpl(TokenProvider tokenProvider) {
        this.tokenProvider = Objects.requireNonNull(tokenProvider, "TokenProvider cannot be null");
    }

    @Override
    public Mono<Boolean> handle(RevokeTokenCommand command) {
        Objects.requireNonNull(command, "RevokeTokenCommand cannot be null");

        return tokenProvider.revokeToken(command.token())
                .thenReturn(true);
    }
}
