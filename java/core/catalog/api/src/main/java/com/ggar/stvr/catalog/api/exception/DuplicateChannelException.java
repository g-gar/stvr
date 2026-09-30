package com.ggar.stvr.catalog.api.exception;

import com.ggar.stvr.catalog.entities.ChannelUrl;
import com.ggar.stvr.identity.entities.UserId;

import java.util.Objects;

/**
 * Exception thrown when a user attempts to add a channel URL they are already tracking.
 */
public class DuplicateChannelException extends RuntimeException {

    private final UserId userId;
    private final ChannelUrl url;

    public DuplicateChannelException(UserId userId, ChannelUrl url) {
        super("User [" + (userId != null ? userId.asString() : "unknown") + "] is already tracking channel: " + (url != null ? url.asString() : "null"));
        this.userId = Objects.requireNonNull(userId, "UserId cannot be null");
        this.url = Objects.requireNonNull(url, "ChannelUrl cannot be null");
    }

    public UserId getUserId() {
        return userId;
    }

    public ChannelUrl getUrl() {
        return url;
    }
}
