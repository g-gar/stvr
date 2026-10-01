package com.ggar.stvr.identity.api.exception;

/**
 * Exception thrown when a provided authentication token is blank, malformed, or invalid.
 */
public class InvalidTokenException extends RuntimeException {

    private final String rawToken;

    public InvalidTokenException(String rawToken, String message) {
        super(message);
        this.rawToken = rawToken;
    }

    public InvalidTokenException(String rawToken, Throwable cause) {
        super("Invalid token: " + rawToken, cause);
        this.rawToken = rawToken;
    }

    public String getRawToken() {
        return rawToken;
    }
}
