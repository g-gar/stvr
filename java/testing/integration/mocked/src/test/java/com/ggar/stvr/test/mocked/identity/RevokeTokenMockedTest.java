package com.ggar.stvr.test.mocked.identity;

import com.ggar.stvr.identity.api.RevokeTokenCommandHandler;
import com.ggar.stvr.identity.api.token.TokenProvider;
import com.ggar.stvr.identity.implementation.RevokeTokenCommandHandlerImpl;
import com.ggar.stvr.identity.implementation.token.JwtTokenProviderImpl;
import com.ggar.stvr.identity.persistence.TokenRevocationRepository;
import com.ggar.stvr.test.contracts.identity.RevokeTokenContractTest;
import org.junit.jupiter.api.BeforeEach;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Concrete mocked execution of RevokeTokenContractTest.
 * Uses RevokeTokenCommandHandlerImpl backed by in-memory TokenRevocationRepository
 * and JwtTokenProviderImpl.
 */
public class RevokeTokenMockedTest extends RevokeTokenContractTest {

    private final Map<String, Instant> revokedTokens = new ConcurrentHashMap<>();

    private TokenProvider tokenProvider;
    private RevokeTokenCommandHandler handler;

    @BeforeEach
    void setUp() {
        revokedTokens.clear();

        TokenRevocationRepository inMemoryRevocationRepo = new TokenRevocationRepository() {
            @Override
            public Mono<Void> revoke(String tokenId, Instant expiresAt) {
                revokedTokens.put(tokenId, expiresAt);
                return Mono.empty();
            }

            @Override
            public Mono<Boolean> isRevoked(String tokenId) {
                return Mono.just(revokedTokens.containsKey(tokenId));
            }
        };

        tokenProvider = new JwtTokenProviderImpl(inMemoryRevocationRepo);
        handler = new RevokeTokenCommandHandlerImpl(tokenProvider);
    }

    @Override
    protected RevokeTokenCommandHandler getHandler() {
        return handler;
    }

    @Override
    protected TokenProvider getTokenProvider() {
        return tokenProvider;
    }
}
