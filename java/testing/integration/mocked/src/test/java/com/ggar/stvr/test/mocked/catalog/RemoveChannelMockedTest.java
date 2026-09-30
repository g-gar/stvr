package com.ggar.stvr.test.mocked.catalog;

import com.ggar.stvr.catalog.api.AddChannelCommandHandler.ChannelDto;
import com.ggar.stvr.catalog.api.ListUserChannelsQueryHandler.UserChannelListItemDto;
import com.ggar.stvr.catalog.api.RemoveChannelCommandHandler;
import com.ggar.stvr.catalog.api.filter.ChannelFilter;
import com.ggar.stvr.catalog.entities.ChannelId;
import com.ggar.stvr.catalog.entities.ChannelUrl;
import com.ggar.stvr.catalog.implementation.RemoveChannelCommandHandlerImpl;
import com.ggar.stvr.catalog.persistence.ChannelRepository;
import com.ggar.stvr.identity.entities.UserId;
import com.ggar.stvr.test.contracts.catalog.RemoveChannelContractTest;
import org.junit.jupiter.api.BeforeEach;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Concrete mocked execution of RemoveChannelContractTest.
 * Uses RemoveChannelCommandHandlerImpl backed by an in-memory ChannelRepository.
 */
public class RemoveChannelMockedTest extends RemoveChannelContractTest {

    private record UserChannelKey(UserId userId, ChannelId channelId) {}

    private final Set<UserChannelKey> storage = ConcurrentHashMap.newKeySet();
    private RemoveChannelCommandHandler handler;

    @BeforeEach
    void setUp() {
        storage.clear();

        ChannelRepository inMemoryRepository = new ChannelRepository() {
            @Override
            public Flux<UserChannelListItemDto> findUserChannels(
                    UserId userId, ChannelFilter filter) {
                return Flux.empty();
            }

            @Override
            public Mono<Boolean> toggleFavorite(UserId userId, ChannelId channelId) {
                return Mono.empty();
            }

            @Override
            public Mono<Boolean> deleteUserChannel(UserId userId, ChannelId channelId) {
                UserChannelKey key = new UserChannelKey(userId, channelId);
                if (!storage.contains(key)) {
                    return Mono.empty();
                }
                storage.remove(key);
                return Mono.just(true);
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

        this.handler = new RemoveChannelCommandHandlerImpl(inMemoryRepository);
    }

    @Override
    protected RemoveChannelCommandHandler getHandler() {
        return this.handler;
    }

    @Override
    protected void setupChannelForUser(UserId userId, ChannelId channelId) {
        storage.add(new UserChannelKey(userId, channelId));
    }

    @Override
    protected boolean isChannelAssociatedWithUser(UserId userId, ChannelId channelId) {
        return storage.contains(new UserChannelKey(userId, channelId));
    }
}
