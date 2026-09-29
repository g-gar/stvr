package com.ggar.stvr.catalog.persistence;

import com.ggar.stvr.catalog.api.AddChannelCommandHandler.ChannelDto;
import com.ggar.stvr.catalog.api.ListUserChannelsQueryHandler.UserChannelListItemDto;
import com.ggar.stvr.catalog.api.filter.ChannelFilter;
import com.ggar.stvr.catalog.entities.ChannelId;
import com.ggar.stvr.catalog.entities.ChannelUrl;
import com.ggar.stvr.identity.entities.UserId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive persistence repository port for Channel catalog operations.
 */
public interface ChannelRepository {

    /**
     * Finds channels for a given user according to filter criteria.
     *
     * @param userId The authenticated user ID.
     * @param filter Filter criteria (platforms, name query, favorites, etc.).
     * @return Reactive Flux of user channel list items.
     */
    Flux<UserChannelListItemDto> findUserChannels(UserId userId, ChannelFilter filter);

    /**
     * Toggles the favorite status of a channel for a given user.
     *
     * @param userId The authenticated user ID.
     * @param channelId The channel ID.
     * @return Mono emitting the new boolean favorite status, or Mono.empty() if the channel is not found.
     */
    Mono<Boolean> toggleFavorite(UserId userId, ChannelId channelId);

    /**
     * Removes the association between a user and a channel.
     *
     * @param userId The authenticated user ID.
     * @param channelId The channel ID to remove.
     * @return Mono emitting true if deleted, or Mono.empty() if the channel was not found for this user.
     */
    Mono<Boolean> deleteUserChannel(UserId userId, ChannelId channelId);

    /**
     * Checks if a user is already tracking a channel by URL.
     *
     * @param userId The authenticated user ID.
     * @param url The channel URL.
     * @return Mono emitting true if the user already tracks this URL, false otherwise.
     */
    Mono<Boolean> existsUserChannel(UserId userId, ChannelUrl url);

    /**
     * Looks up an existing channel in the global catalog by URL.
     *
     * @param url The channel URL.
     * @return Mono emitting the ChannelDto if found, or Mono.empty() if it does not exist in the global catalog.
     */
    Mono<ChannelDto> findChannelByUrl(ChannelUrl url);

    /**
     * Saves a new channel entity and associates it with the specified user.
     *
     * @param userId The authenticated user ID.
     * @param channel The channel data to persist.
     * @return Mono emitting the persisted ChannelDto.
     */
    Mono<ChannelDto> saveAndAssociate(UserId userId, ChannelDto channel);

    /**
     * Associates an existing channel with the specified user.
     *
     * @param userId The authenticated user ID.
     * @param channelId The existing channel ID.
     * @return Mono emitting the associated ChannelDto.
     */
    Mono<ChannelDto> associateExistingChannel(UserId userId, ChannelId channelId);
}
