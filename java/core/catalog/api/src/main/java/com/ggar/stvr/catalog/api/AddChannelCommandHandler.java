package com.ggar.stvr.catalog.api;

import com.ggar.stvr.catalog.api.exception.InvalidChannelUrlException;
import com.ggar.stvr.catalog.entities.ChannelId;
import com.ggar.stvr.catalog.entities.ChannelName;
import com.ggar.stvr.catalog.entities.ChannelUrl;
import com.ggar.stvr.catalog.entities.Platform;
import com.ggar.stvr.framework.cqrs.Command;
import com.ggar.stvr.framework.cqrs.CommandHandler;
import com.ggar.stvr.identity.entities.UserId;

import java.util.Objects;
import java.util.Set;

/**
 * CQRS command handler interface for adding a new channel to a user's catalog.
 */
public interface AddChannelCommandHandler
        extends CommandHandler<AddChannelCommandHandler.AddChannelCommand, AddChannelCommandHandler.ChannelDto> {

    /**
     * Command representing the intent to add a channel to a user's catalog.
     *
     * @param userId The authenticated user adding the channel.
     * @param url The validated channel URL.
     * @param customName Optional custom display name override.
     */
    record AddChannelCommand(
            UserId userId,
            ChannelUrl url,
            ChannelName customName
    ) implements Command<ChannelDto> {

        public AddChannelCommand {
            Objects.requireNonNull(userId, "UserId cannot be null");
            Objects.requireNonNull(url, "ChannelUrl cannot be null");
        }

        public AddChannelCommand(UserId userId, ChannelUrl url) {
            this(userId, url, null);
        }

        public static AddChannelCommand of(UserId userId, String rawUrl) {
            return of(userId, rawUrl, null);
        }

        public static AddChannelCommand of(UserId userId, String rawUrl, String rawCustomName) {
            Objects.requireNonNull(userId, "UserId cannot be null");
            if (rawUrl == null || rawUrl.isBlank()) {
                throw new InvalidChannelUrlException(rawUrl, "Channel URL cannot be null or blank");
            }
            ChannelUrl channelUrl;
            try {
                channelUrl = ChannelUrl.of(rawUrl);
            } catch (Exception e) {
                throw new InvalidChannelUrlException(rawUrl, e);
            }
            ChannelName name = (rawCustomName != null && !rawCustomName.isBlank()) ? ChannelName.of(rawCustomName) : null;
            return new AddChannelCommand(userId, channelUrl, name);
        }
    }

    /**
     * DTO representing the added channel in the user's catalog.
     */
    record ChannelDto(
            ChannelId id,
            ChannelName name,
            ChannelUrl url,
            Platform platform,
            boolean isFavorite,
            boolean isLive,
            String category,
            Set<String> tags,
            Set<String> availableQualities
    ) {
        public ChannelDto {
            Objects.requireNonNull(id, "ChannelId cannot be null");
            Objects.requireNonNull(name, "ChannelName cannot be null");
            Objects.requireNonNull(url, "ChannelUrl cannot be null");
            Objects.requireNonNull(platform, "Platform cannot be null");
            tags = tags != null ? Set.copyOf(tags) : Set.of();
            availableQualities = availableQualities != null ? Set.copyOf(availableQualities) : Set.of();
        }

        public ChannelDto(
                ChannelId id,
                ChannelName name,
                ChannelUrl url,
                Platform platform,
                boolean isFavorite
        ) {
            this(id, name, url, platform, isFavorite, false, null, Set.of(), Set.of());
        }
    }
}
