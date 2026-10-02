package com.ggar.stvr.catalog.api.exception;

import com.ggar.stvr.catalog.entities.SessionId;

import java.util.Objects;

/**
 * Exception thrown when a stream session cannot be found.
 */
public class StreamSessionNotFoundException extends RuntimeException {

    private final SessionId sessionId;

    public StreamSessionNotFoundException(SessionId sessionId) {
        super(String.format("Stream session with id '%s' not found",
                Objects.requireNonNull(sessionId, "sessionId cannot be null").value()));
        this.sessionId = sessionId;
    }

    public SessionId getSessionId() {
        return sessionId;
    }
}
