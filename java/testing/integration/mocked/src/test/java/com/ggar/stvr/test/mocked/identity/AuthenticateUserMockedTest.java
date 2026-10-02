package com.ggar.stvr.test.mocked.identity;

import com.ggar.stvr.identity.api.AuthenticateUserCommandHandler;
import com.ggar.stvr.identity.api.security.PasswordEncoder;
import com.ggar.stvr.identity.api.token.TokenProvider;
import com.ggar.stvr.identity.entities.User;
import com.ggar.stvr.identity.entities.UserId;
import com.ggar.stvr.identity.entities.Username;
import com.ggar.stvr.identity.implementation.AuthenticateUserCommandHandlerImpl;
import com.ggar.stvr.identity.implementation.security.BCryptPasswordEncoderImpl;
import com.ggar.stvr.identity.implementation.token.JwtTokenProviderImpl;
import com.ggar.stvr.identity.persistence.UserRepository;
import com.ggar.stvr.test.contracts.identity.AuthenticateUserContractTest;
import org.junit.jupiter.api.BeforeEach;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Concrete mocked execution of AuthenticateUserContractTest.
 * Uses AuthenticateUserCommandHandlerImpl backed by in-memory UserRepository,
 * BCryptPasswordEncoderImpl, and JwtTokenProviderImpl.
 */
public class AuthenticateUserMockedTest extends AuthenticateUserContractTest {

    private final Map<Username, User> usersByUsername = new ConcurrentHashMap<>();
    private final Map<UserId, User> usersById = new ConcurrentHashMap<>();

    private PasswordEncoder passwordEncoder;
    private TokenProvider tokenProvider;
    private AuthenticateUserCommandHandler handler;

    @BeforeEach
    void setUp() {
        usersByUsername.clear();
        usersById.clear();

        passwordEncoder = new BCryptPasswordEncoderImpl(10);
        tokenProvider = new JwtTokenProviderImpl();

        UserRepository inMemoryUserRepository = new UserRepository() {
            @Override
            public Mono<Boolean> existsByUsername(Username username) {
                return Mono.just(usersByUsername.containsKey(username));
            }

            @Override
            public Mono<User> save(User user) {
                usersByUsername.put(user.username(), user);
                usersById.put(user.id(), user);
                return Mono.just(user);
            }

            @Override
            public Mono<User> findByUsername(Username username) {
                return Mono.justOrEmpty(usersByUsername.get(username));
            }

            @Override
            public Mono<User> findById(UserId id) {
                return Mono.justOrEmpty(usersById.get(id));
            }
        };

        handler = new AuthenticateUserCommandHandlerImpl(
                inMemoryUserRepository,
                passwordEncoder,
                tokenProvider
        );
    }

    @Override
    protected AuthenticateUserCommandHandler getHandler() {
        return handler;
    }

    @Override
    protected void registerUser(User user) {
        usersByUsername.put(user.username(), user);
        usersById.put(user.id(), user);
    }

    @Override
    protected PasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }

    @Override
    protected TokenProvider getTokenProvider() {
        return tokenProvider;
    }
}
