package com.ggar.stvr.catalog.api.exception;

import com.ggar.stvr.catalog.entities.ChannelId;
import com.ggar.stvr.identity.entities.UserId;

import java.util.Objects;

/**
 * Exception thrown when a channel cannot be found for the given user.
 */
public class ChannelNotFoundException extends RuntimeException {

    private final ChannelId channelId;
    private final UserId userId;

    public ChannelNotFoundException(ChannelId channelId, UserId userId) {
        super(String.format("Channel with id '%s' not found for user '%s'",
                Objects.requireNonNull(channelId, "channelId cannot be null").value(),
                Objects.requireNonNull(userId, "userId cannot be null").value()));
        this.channelId = channelId;
        this.userId = userId;
    }

    public ChannelId getChannelId() {
        return channelId;
    }

    public UserId getUserId() {
        return userId;
    }
}
