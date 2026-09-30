package com.ggar.stvr.packages.streamlink.exception;

/**
 * Base unchecked exception for all Streamlink SDK errors.
 */
public class StreamlinkException extends RuntimeException {

    public StreamlinkException(String message) {
        super(message);
    }

    public StreamlinkException(String message, Throwable cause) {
        super(message, cause);
    }
}
