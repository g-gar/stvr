package com.ggar.stvr.catalog.api;

import com.ggar.stvr.catalog.entities.Channel;
import com.ggar.stvr.catalog.entities.ChannelId;
import com.ggar.stvr.catalog.entities.ChannelName;
import com.ggar.stvr.catalog.entities.ChannelUrl;
import com.ggar.stvr.catalog.entities.Platform;
import com.ggar.stvr.framework.cqrs.Command;
import com.ggar.stvr.framework.cqrs.CommandHandler;
import com.ggar.stvr.identity.entities.UserId;

import java.util.Objects;

/**
 * CQRS command handler interface for adding an identified channel to a user's catalog.
 */
public interface AddChannelCommandHandler
        extends CommandHandler<AddChannelCommandHandler.AddChannelCommand, AddChannelCommandHandler.ChannelDto> {

    /**
     * Atomic command representing the intent to add an identified channel to a user's catalog.
     *
     * @param userId The authenticated user adding the channel.
     * @param url The validated channel URL.
     * @param platform The streaming platform.
     * @param slug Channel slug or handle on the platform.
     * @param name Display name of the channel.
     */
    record AddChannelCommand(
            UserId userId,
            ChannelUrl url,
            Platform platform,
            String slug,
            ChannelName name
    ) implements Command<ChannelDto> {

        public AddChannelCommand {
            Objects.requireNonNull(userId, "UserId cannot be null");
            Objects.requireNonNull(url, "ChannelUrl cannot be null");
            Objects.requireNonNull(platform, "Platform cannot be null");
            slug = slug != null ? slug : "";
            Objects.requireNonNull(name, "ChannelName cannot be null");
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
            boolean isFavorite
    ) {
        public ChannelDto {
            Objects.requireNonNull(id, "ChannelId cannot be null");
            Objects.requireNonNull(name, "ChannelName cannot be null");
            Objects.requireNonNull(url, "ChannelUrl cannot be null");
            Objects.requireNonNull(platform, "Platform cannot be null");
        }

        public static ChannelDto fromDomain(Channel channel) {
            Objects.requireNonNull(channel, "Channel cannot be null");
            return new ChannelDto(
                    channel.getId(),
                    channel.getName(),
                    channel.getUrl(),
                    channel.getPlatform(),
                    false
            );
        }
    }
}
