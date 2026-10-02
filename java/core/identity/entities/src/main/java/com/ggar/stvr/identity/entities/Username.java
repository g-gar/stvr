package com.ggar.stvr.identity.entities;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object representing a validated user username.
 * Must be 3 to 30 characters long and alphanumeric.
 */
public record Username(String value) {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9]{3,30}$");

    public Username {
        Objects.requireNonNull(value, "Username cannot be null");
        String trimmed = value.trim();
        if (trimmed.length() < 3 || trimmed.length() > 30) {
            throw new IllegalArgumentException("Username length must be between 3 and 30 characters, but was: " + trimmed.length());
        }
        if (!USERNAME_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Username must be alphanumeric, but was: " + trimmed);
        }
        value = trimmed;
    }

    public static Username of(String value) {
        return new Username(value);
    }

    public String asString() {
        return value;
    }
}
