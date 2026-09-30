package com.ggar.stvr.catalog.api;

import com.ggar.stvr.catalog.entities.ChannelId;
import com.ggar.stvr.framework.cqrs.Command;
import com.ggar.stvr.framework.cqrs.CommandHandler;
import com.ggar.stvr.identity.entities.UserId;

import java.util.Objects;

/**
 * CQRS command handler interface for removing a channel from a user's catalog.
 * Encapsulates the command parameters for channel deletion.
 */
public interface RemoveChannelCommandHandler
        extends CommandHandler<RemoveChannelCommandHandler.RemoveChannelCommand, Void> {

    /**
     * Command parameters for removing a channel association from a user.
     *
     * @param userId The authenticated user requesting the removal.
     * @param channelId The unique identifier of the channel to be removed.
     */
    record RemoveChannelCommand(
            UserId userId,
            ChannelId channelId
    ) implements Command<Void> {

        public RemoveChannelCommand {
            Objects.requireNonNull(userId, "UserId cannot be null");
            Objects.requireNonNull(channelId, "ChannelId cannot be null");
        }
    }
}
