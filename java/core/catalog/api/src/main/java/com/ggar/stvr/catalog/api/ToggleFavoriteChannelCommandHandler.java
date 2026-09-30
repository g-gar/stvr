package com.ggar.stvr.catalog.api;

import com.ggar.stvr.catalog.entities.ChannelId;
import com.ggar.stvr.framework.cqrs.Command;
import com.ggar.stvr.framework.cqrs.CommandHandler;
import com.ggar.stvr.identity.entities.UserId;

import java.util.Objects;

/**
 * CQRS command handler interface for toggling the favorite status of a channel.
 * Encapsulates the command input parameters and the response projection.
 */
public interface ToggleFavoriteChannelCommandHandler
        extends CommandHandler<ToggleFavoriteChannelCommandHandler.ToggleFavoriteChannelCommand, ToggleFavoriteChannelCommandHandler.ToggleFavoriteResponseDto> {

    /**
     * Command parameters for toggling a channel's favorite status.
     *
     * @param userId The authenticated user who owns the channel association.
     * @param channelId The unique identifier of the channel to toggle.
     */
    record ToggleFavoriteChannelCommand(
            UserId userId,
            ChannelId channelId
    ) implements Command<ToggleFavoriteResponseDto> {

        public ToggleFavoriteChannelCommand {
            Objects.requireNonNull(userId, "UserId cannot be null");
            Objects.requireNonNull(channelId, "ChannelId cannot be null");
        }
    }

    /**
     * Result of toggling a channel's favorite status.
     *
     * @param channelId The unique identifier of the affected channel.
     * @param isFavorite The new favorite state (true if favorite, false otherwise).
     */
    record ToggleFavoriteResponseDto(
            ChannelId channelId,
            boolean isFavorite
    ) {
        public ToggleFavoriteResponseDto {
            Objects.requireNonNull(channelId, "ChannelId cannot be null");
        }
    }
}
