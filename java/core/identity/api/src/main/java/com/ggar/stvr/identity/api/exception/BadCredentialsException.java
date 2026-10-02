package com.ggar.stvr.identity.api.exception;

/**
 * Exception thrown when authentication fails due to invalid credentials (username or password).
 */
public class BadCredentialsException extends RuntimeException {

    public BadCredentialsException() {
        super("Invalid username or password");
    }

    public BadCredentialsException(String message) {
        super(message);
    }
}
