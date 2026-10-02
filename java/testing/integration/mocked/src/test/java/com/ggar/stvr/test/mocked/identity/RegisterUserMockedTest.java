package com.ggar.stvr.test.mocked.identity;

import com.ggar.stvr.identity.api.RegisterUserCommandHandler;
import com.ggar.stvr.identity.api.security.PasswordEncoder;
import com.ggar.stvr.identity.entities.User;
import com.ggar.stvr.identity.entities.UserId;
import com.ggar.stvr.identity.entities.Username;
import com.ggar.stvr.identity.implementation.RegisterUserCommandHandlerImpl;
import com.ggar.stvr.identity.implementation.security.BCryptPasswordEncoderImpl;
import com.ggar.stvr.identity.persistence.UserRepository;
import com.ggar.stvr.test.contracts.identity.RegisterUserContractTest;
import org.junit.jupiter.api.BeforeEach;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Concrete mocked execution of RegisterUserContractTest.
 * Uses RegisterUserCommandHandlerImpl backed by an in-memory UserRepository and BCryptPasswordEncoderImpl.
 */
public class RegisterUserMockedTest extends RegisterUserContractTest {

    private final Map<Username, User> usersByUsername = new ConcurrentHashMap<>();
    private final Map<UserId, User> usersById = new ConcurrentHashMap<>();

    private PasswordEncoder passwordEncoder;
    private RegisterUserCommandHandler handler;

    @BeforeEach
    void setUp() {
        usersByUsername.clear();
        usersById.clear();

        // Using cost factor 10 or 12 for testing (fast enough and verifies real BCrypt hashing)
        passwordEncoder = new BCryptPasswordEncoderImpl(10);

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

        handler = new RegisterUserCommandHandlerImpl(inMemoryUserRepository, passwordEncoder);
    }

    @Override
    protected RegisterUserCommandHandler getHandler() {
        return handler;
    }

    @Override
    protected boolean userExistsInStorage(Username username) {
        return usersByUsername.containsKey(username);
    }

    @Override
    protected Optional<User> findUserInStorage(Username username) {
        return Optional.ofNullable(usersByUsername.get(username));
    }

    @Override
    protected PasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }
}
