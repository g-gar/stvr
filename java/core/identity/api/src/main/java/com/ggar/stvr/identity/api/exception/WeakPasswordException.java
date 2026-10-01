package com.ggar.stvr.identity.api.exception;

/**
 * Exception thrown when a provided password fails security strength requirements.
 */
public class WeakPasswordException extends RuntimeException {

    public WeakPasswordException(String message) {
        super(message);
    }
}
