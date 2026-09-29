package com.ggar.stvr.test.mocked.catalog;

import com.ggar.stvr.catalog.api.AddChannelCommandHandler.ChannelDto;
import com.ggar.stvr.catalog.api.ListUserChannelsQueryHandler.UserChannelListItemDto;
import com.ggar.stvr.catalog.api.ToggleFavoriteChannelCommandHandler;
import com.ggar.stvr.catalog.api.filter.ChannelFilter;
import com.ggar.stvr.catalog.entities.ChannelId;
import com.ggar.stvr.catalog.entities.ChannelUrl;
import com.ggar.stvr.catalog.implementation.ToggleFavoriteChannelCommandHandlerImpl;
import com.ggar.stvr.catalog.persistence.ChannelRepository;
import com.ggar.stvr.identity.entities.UserId;
import com.ggar.stvr.test.contracts.catalog.ToggleFavoriteChannelContractTest;
import org.junit.jupiter.api.BeforeEach;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concrete mocked execution of ToggleFavoriteChannelContractTest.
 * Uses ToggleFavoriteChannelCommandHandlerImpl backed by an in-memory ChannelRepository.
 */
public class ToggleFavoriteChannelMockedTest extends ToggleFavoriteChannelContractTest {

    private record UserChannelKey(UserId userId, ChannelId channelId) {}

    private final Map<UserChannelKey, Boolean> storage = new ConcurrentHashMap<>();
    private ToggleFavoriteChannelCommandHandler handler;

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
                UserChannelKey key = new UserChannelKey(userId, channelId);
                if (!storage.containsKey(key)) {
                    return Mono.empty();
                }
                boolean newStatus = !storage.get(key);
                storage.put(key, newStatus);
                return Mono.just(newStatus);
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

        this.handler = new ToggleFavoriteChannelCommandHandlerImpl(inMemoryRepository);
    }

    @Override
    protected ToggleFavoriteChannelCommandHandler getHandler() {
        return this.handler;
    }

    @Override
    protected void setupChannelWithFavoriteStatus(UserId userId, ChannelId channelId, boolean initialFavorite) {
        storage.put(new UserChannelKey(userId, channelId), initialFavorite);
    }

    @Override
    protected void verifyFavoriteStatusInStorage(UserId userId, ChannelId channelId, boolean expectedFavorite) {
        Boolean stored = storage.get(new UserChannelKey(userId, channelId));
        assertThat(stored).isNotNull();
        assertThat(stored).isEqualTo(expectedFavorite);
    }
}
