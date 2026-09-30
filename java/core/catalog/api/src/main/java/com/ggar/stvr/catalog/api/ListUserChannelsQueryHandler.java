package com.ggar.stvr.catalog.api;

import com.ggar.stvr.catalog.entities.ChannelId;
import com.ggar.stvr.catalog.entities.ChannelName;
import com.ggar.stvr.catalog.entities.ChannelUrl;
import com.ggar.stvr.catalog.entities.Platform;
import com.ggar.stvr.catalog.api.filter.ChannelFilter;
import com.ggar.stvr.framework.cqrs.Query;
import com.ggar.stvr.framework.cqrs.QueryHandler;
import com.ggar.stvr.identity.entities.UserId;

import java.util.Objects;
import java.util.Set;

/**
 * CQRS query handler interface for retrieving the channels associated with an authenticated user.
 * Encapsulates the use case query parameters and the resulting projection for catalog display.
 */
public interface ListUserChannelsQueryHandler
        extends QueryHandler<ListUserChannelsQueryHandler.ListUserChannelsQuery, ListUserChannelsQueryHandler.UserChannelListItemDto> {

    /**
     * Query parameters for retrieving a user's channel list.
     *
     * @param userId The authenticated user whose channels will be retrieved.
     * @param filter Filter criteria applied to the channels (defaults to {@link ChannelFilter#all()} if null).
     */
    record ListUserChannelsQuery(
            UserId userId,
            ChannelFilter filter
    ) implements Query<UserChannelListItemDto> {

        public ListUserChannelsQuery {
            Objects.requireNonNull(userId, "UserId cannot be null");
            if (filter == null) {
                filter = ChannelFilter.all();
            }
        }

        public ListUserChannelsQuery(UserId userId) {
            this(userId, ChannelFilter.all());
        }
    }

    /**
     * Projected representation of a channel for display in user catalogs, sidebars, and navigation views.
     *
     * @param id Channel unique identifier.
     * @param name Display name of the streamer or channel.
     * @param url Stream URL for playback ingestion.
     * @param platform Streaming service provider.
     * @param isFavorite Flag indicating if the channel is marked as a user favorite.
     * @param isLive Flag indicating if the channel is currently streaming live.
     * @param category Active category or game title, or null if offline/unavailable.
     * @param tags Associated categorization tags.
     */
    record UserChannelListItemDto(
            ChannelId id,
            ChannelName name,
            ChannelUrl url,
            Platform platform,
            boolean isFavorite,
            boolean isLive,
            String category,
            Set<String> tags
    ) {
        public UserChannelListItemDto {
            Objects.requireNonNull(id, "Channel id cannot be null");
            Objects.requireNonNull(name, "Channel name cannot be null");
            Objects.requireNonNull(url, "Channel url cannot be null");
            Objects.requireNonNull(platform, "Channel platform cannot be null");
            tags = tags != null ? Set.copyOf(tags) : Set.of();
        }

        public UserChannelListItemDto(
                ChannelId id,
                ChannelName name,
                ChannelUrl url,
                Platform platform,
                boolean isFavorite
        ) {
            this(id, name, url, platform, isFavorite, false, null, Set.of());
        }
    }
}
