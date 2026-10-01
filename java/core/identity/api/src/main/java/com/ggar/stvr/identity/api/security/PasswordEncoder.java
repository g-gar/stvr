package com.ggar.stvr.identity.api.security;

/**
 * Service port interface for hashing and verifying user passwords.
 */
public interface PasswordEncoder {

    /**
     * Hashes the raw password string.
     *
     * @param rawPassword The plain-text password to hash.
     * @return The hashed password string.
     */
    String encode(CharSequence rawPassword);

    /**
     * Verifies that the raw password matches the encoded hash.
     *
     * @param rawPassword The plain-text password.
     * @param encodedPassword The stored password hash.
     * @return true if the passwords match, false otherwise.
     */
    boolean matches(CharSequence rawPassword, String encodedPassword);
}
