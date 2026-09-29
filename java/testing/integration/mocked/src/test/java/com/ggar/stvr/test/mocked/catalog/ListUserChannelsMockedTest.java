package com.ggar.stvr.test.mocked.catalog;

import com.ggar.stvr.catalog.api.AddChannelCommandHandler.ChannelDto;
import com.ggar.stvr.catalog.api.ListUserChannelsQueryHandler;
import com.ggar.stvr.catalog.api.ListUserChannelsQueryHandler.UserChannelListItemDto;
import com.ggar.stvr.catalog.api.filter.ChannelFilter;
import com.ggar.stvr.catalog.entities.ChannelId;
import com.ggar.stvr.catalog.entities.ChannelUrl;
import com.ggar.stvr.catalog.implementation.ListUserChannelsQueryHandlerImpl;
import com.ggar.stvr.catalog.persistence.ChannelRepository;
import com.ggar.stvr.identity.entities.UserId;
import com.ggar.stvr.test.contracts.catalog.ListUserChannelsContractTest;
import org.junit.jupiter.api.BeforeEach;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Concrete mocked execution of ListUserChannelsContractTest.
 * Uses ListUserChannelsQueryHandlerImpl backed by an in-memory ChannelRepository.
 */
public class ListUserChannelsMockedTest extends ListUserChannelsContractTest {

    private final Map<UserId, List<UserChannelListItemDto>> storage = new ConcurrentHashMap<>();
    private ListUserChannelsQueryHandler handler;

    @BeforeEach
    void setUp() {
        storage.clear();

        ChannelRepository inMemoryRepository = new ChannelRepository() {
            @Override
            public Flux<UserChannelListItemDto> findUserChannels(UserId userId, ChannelFilter filter) {
                List<UserChannelListItemDto> channels = storage.getOrDefault(userId, List.of());
                return Flux.fromIterable(channels)
                        .filter(ch -> matchesFilter(ch, filter));
            }

            @Override
            public Mono<Boolean> toggleFavorite(UserId userId, ChannelId channelId) {
                return Mono.empty();
            }

            @Override
            public Mono<Boolean> deleteUserChannel(UserId userId, ChannelId channelId) {
                return Mono.empty();
            }

            @Override
            public Mono<Boolean> existsUserChannel(UserId userId, ChannelUrl url) {
                return Mono.just(false);
            }

            @Override
            public Mono<ChannelDto> findChannelByUrl(ChannelUrl url) {
                return Mono.empty();
            }

            @Override
            public Mono<ChannelDto> saveAndAssociate(UserId userId, ChannelDto channel) {
                return Mono.empty();
            }

            @Override
            public Mono<ChannelDto> associateExistingChannel(UserId userId, ChannelId channelId) {
                return Mono.empty();
            }
        };

        this.handler = new ListUserChannelsQueryHandlerImpl(inMemoryRepository);
    }

    private boolean matchesFilter(UserChannelListItemDto ch, ChannelFilter filter) {
        if (filter == null) {
            return true;
        }
        if (!filter.platforms().isEmpty() && !filter.platforms().contains(ch.platform())) {
            return false;
        }
        if (filter.favoritesOnly() && !ch.isFavorite()) {
            return false;
        }
        if (filter.nameQuery() != null && !filter.nameQuery().isBlank()) {
            if (!ch.name().value().toLowerCase().contains(filter.nameQuery().toLowerCase())) {
                return false;
            }
        }
        if (filter.isLive() != null && ch.isLive() != filter.isLive()) {
            return false;
        }
        if (filter.category() != null && !filter.category().equalsIgnoreCase(ch.category())) {
            return false;
        }
        if (!filter.tags().isEmpty() && !ch.tags().containsAll(filter.tags())) {
            return false;
        }
        return true;
    }

    @Override
    protected ListUserChannelsQueryHandler getHandler() {
        return this.handler;
    }

    @Override
    protected void setupChannelsForUser(UserId userId, List<UserChannelListItemDto> channels) {
        storage.put(userId, new ArrayList<>(channels));
    }
}
