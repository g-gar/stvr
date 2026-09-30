package com.ggar.stvr.catalog.api.exception;

/**
 * Exception thrown when a provided channel URL string is blank, malformed, or invalid.
 */
public class InvalidChannelUrlException extends RuntimeException {

    private final String rawUrl;

    public InvalidChannelUrlException(String rawUrl, String message) {
        super(message);
        this.rawUrl = rawUrl;
    }

    public InvalidChannelUrlException(String rawUrl, Throwable cause) {
        super("Invalid channel URL: " + rawUrl, cause);
        this.rawUrl = rawUrl;
    }

    public String getRawUrl() {
        return rawUrl;
    }
}
