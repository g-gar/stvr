package com.ggar.stvr.test.mocked.catalog;

import com.ggar.stvr.catalog.api.AddChannelCommandHandler;
import com.ggar.stvr.catalog.api.AddChannelCommandHandler.ChannelDto;
import com.ggar.stvr.catalog.api.ListUserChannelsQueryHandler;
import com.ggar.stvr.catalog.api.filter.ChannelFilter;
import com.ggar.stvr.catalog.api.inspector.ChannelInspectionResult;
import com.ggar.stvr.catalog.api.inspector.ChannelInspector;
import com.ggar.stvr.catalog.api.inspector.CompositeChannelInspectorFactory;
import com.ggar.stvr.catalog.entities.ChannelId;
import com.ggar.stvr.catalog.entities.ChannelUrl;
import com.ggar.stvr.catalog.implementation.AddChannelCommandHandlerImpl;
import com.ggar.stvr.catalog.persistence.ChannelRepository;
import com.ggar.stvr.identity.entities.UserId;
import com.ggar.stvr.test.contracts.catalog.AddChannelContractTest;
import org.junit.jupiter.api.BeforeEach;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Concrete mocked execution of AddChannelContractTest.
 * Uses AddChannelCommandHandlerImpl backed by in-memory ChannelRepository and dynamic MockChannelInspector.
 */
public class AddChannelMockedTest extends AddChannelContractTest {

    private record UserChannelKey(UserId userId, ChannelId channelId) {}

    private final Map<ChannelUrl, ChannelDto> globalChannels = new ConcurrentHashMap<>();
    private final Map<ChannelId, ChannelDto> channelsById = new ConcurrentHashMap<>();
    private final Set<UserChannelKey> userChannels = ConcurrentHashMap.newKeySet();
    private final Map<ChannelUrl, ChannelInspectionResult> mockInspectors = new ConcurrentHashMap<>();

    private AddChannelCommandHandler handler;

    @BeforeEach
    void setUp() {
        globalChannels.clear();
        channelsById.clear();
        userChannels.clear();
        mockInspectors.clear();

        ChannelInspector mockInspector = new ChannelInspector() {
            @Override
            public boolean supports(ChannelUrl url) {
                return mockInspectors.containsKey(url);
            }

            @Override
            public Mono<ChannelInspectionResult> inspect(ChannelUrl url) {
                return Mono.justOrEmpty(mockInspectors.get(url));
            }
        };

        ChannelRepository inMemoryRepository = new ChannelRepository() {
            @Override
            public Flux<ListUserChannelsQueryHandler.UserChannelListItemDto> findUserChannels(UserId userId, ChannelFilter filter) {
                return Flux.empty();
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
                ChannelDto channel = globalChannels.get(url);
                if (channel == null) {
                    return Mono.just(false);
                }
                return Mono.just(userChannels.contains(new UserChannelKey(userId, channel.id())));
            }

            @Override
            public Mono<ChannelDto> findChannelByUrl(ChannelUrl url) {
                return Mono.justOrEmpty(globalChannels.get(url));
            }

            @Override
            public Mono<ChannelDto> saveAndAssociate(UserId userId, ChannelDto channel) {
                globalChannels.put(channel.url(), channel);
                channelsById.put(channel.id(), channel);
                userChannels.add(new UserChannelKey(userId, channel.id()));
                return Mono.just(channel);
            }

            @Override
            public Mono<ChannelDto> associateExistingChannel(UserId userId, ChannelId channelId) {
                userChannels.add(new UserChannelKey(userId, channelId));
                return Mono.justOrEmpty(channelsById.get(channelId));
            }
        };

        this.handler = new AddChannelCommandHandlerImpl(
                inMemoryRepository,
                CompositeChannelInspectorFactory.of(mockInspector)
        );
    }

    @Override
    protected AddChannelCommandHandler getHandler() {
        return handler;
    }

    @Override
    protected void registerInspectorResult(ChannelUrl url, ChannelInspectionResult result) {
        mockInspectors.put(url, result);
    }

    @Override
    protected boolean isChannelAssociatedWithUser(UserId userId, ChannelId channelId) {
        return userChannels.contains(new UserChannelKey(userId, channelId));
    }

    @Override
    protected boolean channelExistsInStorage(ChannelUrl url) {
        return globalChannels.containsKey(url);
    }
}
