package com.ggar.stvr.identity.implementation.security;

import com.ggar.stvr.identity.api.security.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * BCrypt password encoder implementation defaulting to cost factor 12.
 */
@Component
public class BCryptPasswordEncoderImpl implements PasswordEncoder {

    public static final int DEFAULT_STRENGTH = 12;

    private final BCryptPasswordEncoder delegate;

    public BCryptPasswordEncoderImpl() {
        this(DEFAULT_STRENGTH);
    }

    public BCryptPasswordEncoderImpl(int strength) {
        this.delegate = new BCryptPasswordEncoder(strength);
    }

    @Override
    public String encode(CharSequence rawPassword) {
        Objects.requireNonNull(rawPassword, "rawPassword cannot be null");
        return delegate.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return delegate.matches(rawPassword, encodedPassword);
    }
}
