package com.ggar.stvr.identity.api.exception;

/**
 * Exception thrown when a provided username is blank, malformed, or invalid.
 */
public class InvalidUsernameException extends RuntimeException {

    private final String rawUsername;

    public InvalidUsernameException(String rawUsername, String message) {
        super(message);
        this.rawUsername = rawUsername;
    }

    public InvalidUsernameException(String rawUsername, Throwable cause) {
        super("Invalid username: " + rawUsername, cause);
        this.rawUsername = rawUsername;
    }

    public String getRawUsername() {
        return rawUsername;
    }
}
