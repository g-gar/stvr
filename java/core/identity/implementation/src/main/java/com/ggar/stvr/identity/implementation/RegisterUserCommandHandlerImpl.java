package com.ggar.stvr.identity.implementation;

import com.ggar.stvr.identity.api.RegisterUserCommandHandler;
import com.ggar.stvr.identity.api.exception.UsernameAlreadyTakenException;
import com.ggar.stvr.identity.api.exception.WeakPasswordException;
import com.ggar.stvr.identity.api.security.PasswordEncoder;
import com.ggar.stvr.identity.entities.User;
import com.ggar.stvr.identity.entities.UserId;
import com.ggar.stvr.identity.persistence.UserRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;

/**
 * Reactive CQRS handler implementation for UC-AUTH-01: Register User.
 */
@Service
public class RegisterUserCommandHandlerImpl implements RegisterUserCommandHandler {

    public static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterUserCommandHandlerImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = Objects.requireNonNull(userRepository, "UserRepository cannot be null");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "PasswordEncoder cannot be null");
    }

    @Override
    public Mono<UserDto> handle(RegisterUserCommand command) {
        Objects.requireNonNull(command, "RegisterUserCommand cannot be null");

        // 1. Validate password strength (min 8 chars)
        if (command.rawPassword() == null || command.rawPassword().length() < MIN_PASSWORD_LENGTH) {
            return Mono.error(new WeakPasswordException(
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long"
            ));
        }

        // 2. Ensure username does not already exist
        return userRepository.existsByUsername(command.username())
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        return Mono.error(new UsernameAlreadyTakenException(command.username()));
                    }

                    // 3. Hash password with BCrypt (cost 12)
                    String passwordHash = passwordEncoder.encode(command.rawPassword());

                    // 4. Persist User in storage
                    User user = new User(
                            UserId.random(),
                            command.username(),
                            passwordHash,
                            command.role(),
                            Instant.now()
                    );

                    return userRepository.save(user)
                            .map(savedUser -> new UserDto(
                                    savedUser.id(),
                                    savedUser.username(),
                                    savedUser.role(),
                                    savedUser.createdAt()
                            ));
                });
    }
}
