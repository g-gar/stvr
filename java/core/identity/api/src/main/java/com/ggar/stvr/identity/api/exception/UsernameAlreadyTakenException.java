package com.ggar.stvr.identity.api.exception;

import com.ggar.stvr.identity.entities.Username;

/**
 * Exception thrown when attempting to register a username that already exists in the system.
 */
public class UsernameAlreadyTakenException extends RuntimeException {

    private final Username username;

    public UsernameAlreadyTakenException(Username username) {
        super("Username is already taken: " + username.value());
        this.username = username;
    }

    public Username getUsername() {
        return username;
    }
}
