package com.ggar.stvr.catalog.implementation;

import com.ggar.stvr.catalog.api.ListUserChannelsQueryHandler;
import com.ggar.stvr.catalog.persistence.ChannelRepository;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Objects;

/**
 * Implementation of the query handler for listing a user's channels.
 * Fetches user channels matching filter criteria from ChannelRepository
 * and guarantees sort order (favorites first, then case-insensitive alphabetical by channel name).
 */
@Service
public class ListUserChannelsQueryHandlerImpl implements ListUserChannelsQueryHandler {

    private static final Comparator<UserChannelListItemDto> DEFAULT_SORT =
            Comparator.comparing(UserChannelListItemDto::isFavorite)
                    .reversed()
                    .thenComparing(dto -> dto.name().value(), String.CASE_INSENSITIVE_ORDER);

    private final ChannelRepository channelRepository;

    public ListUserChannelsQueryHandlerImpl(ChannelRepository channelRepository) {
        this.channelRepository = Objects.requireNonNull(channelRepository, "ChannelRepository cannot be null");
    }

    @Override
    public Publisher<UserChannelListItemDto> handle(ListUserChannelsQuery query) {
        Objects.requireNonNull(query, "Query cannot be null");
        return channelRepository.findUserChannels(query.userId(), query.filter())
                .sort(DEFAULT_SORT);
    }
}
