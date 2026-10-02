package com.ggar.stvr.identity.implementation;

import com.ggar.stvr.identity.api.AuthenticateUserCommandHandler;
import com.ggar.stvr.identity.api.RegisterUserCommandHandler.UserDto;
import com.ggar.stvr.identity.api.exception.BadCredentialsException;
import com.ggar.stvr.identity.api.security.PasswordEncoder;
import com.ggar.stvr.identity.api.token.AuthToken;
import com.ggar.stvr.identity.api.token.TokenProvider;
import com.ggar.stvr.identity.persistence.UserRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Reactive CQRS handler implementation for UC-AUTH-02: Authenticate User (Login).
 */
@Service
public class AuthenticateUserCommandHandlerImpl implements AuthenticateUserCommandHandler {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    public AuthenticateUserCommandHandlerImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TokenProvider tokenProvider
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "UserRepository cannot be null");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "PasswordEncoder cannot be null");
        this.tokenProvider = Objects.requireNonNull(tokenProvider, "TokenProvider cannot be null");
    }

    @Override
    public Mono<AuthTokenResponse> handle(AuthenticateUserCommand command) {
        Objects.requireNonNull(command, "AuthenticateUserCommand cannot be null");

        return userRepository.findByUsername(command.username())
                .switchIfEmpty(Mono.error(new BadCredentialsException()))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(command.password(), user.passwordHash())) {
                        return Mono.error(new BadCredentialsException());
                    }

                    UserDto userDto = new UserDto(
                            user.id(),
                            user.username(),
                            user.role(),
                            user.createdAt()
                    );

                    AuthToken token = tokenProvider.issueToken(userDto);

                    return Mono.just(new AuthTokenResponse(
                            token.value(),
                            token.tokenType(),
                            token.expiresAt(),
                            userDto
                    ));
                });
    }
}
