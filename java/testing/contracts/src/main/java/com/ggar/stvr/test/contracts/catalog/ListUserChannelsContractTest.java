package com.ggar.stvr.test.contracts.catalog;

import com.ggar.stvr.catalog.api.filter.ChannelFilter;
import com.ggar.stvr.catalog.api.ListUserChannelsQueryHandler;
import com.ggar.stvr.catalog.api.ListUserChannelsQueryHandler.ListUserChannelsQuery;
import com.ggar.stvr.catalog.api.ListUserChannelsQueryHandler.UserChannelListItemDto;
import com.ggar.stvr.catalog.entities.ChannelId;
import com.ggar.stvr.catalog.entities.ChannelName;
import com.ggar.stvr.catalog.entities.ChannelUrl;
import com.ggar.stvr.catalog.entities.Platform;
import com.ggar.stvr.identity.entities.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Generic contract test suite for UC-CAT-03: List User Channels.
 * Can be executed against mocked units, real database integration, or load test suites.
 */
public abstract class ListUserChannelsContractTest {

    protected abstract ListUserChannelsQueryHandler getHandler();

    /**
     * Prepares user channels in the underlying storage or mock environment.
     *
     * @param userId   User identifier
     * @param channels Channels to associate with this user
     */
    protected abstract void setupChannelsForUser(UserId userId, List<UserChannelListItemDto> channels);

    @Test
    @DisplayName("Scenario: return all channels for user sorted by favorite status (true first) then name")
    void shouldReturnAllChannelsForUser() {
        UserId userId = UserId.random();

        // Non-favorite 'Zeta'
        UserChannelListItemDto chZeta = new UserChannelListItemDto(
                ChannelId.random(),
                ChannelName.of("Zeta Channel"),
                ChannelUrl.of("https://twitch.tv/zeta"),
                Platform.of("twitch"),
                false
        );
        // Favorite 'Alpha'
        UserChannelListItemDto chAlphaFav = new UserChannelListItemDto(
                ChannelId.random(),
                ChannelName.of("Alpha Streamer"),
                ChannelUrl.of("https://youtube.com/@alpha"),
                Platform.of("youtube"),
                true
        );
        // Favorite 'Beta'
        UserChannelListItemDto chBetaFav = new UserChannelListItemDto(
                ChannelId.random(),
                ChannelName.of("Beta Player"),
                ChannelUrl.of("https://kick.com/beta"),
                Platform.of("kick"),
                true
        );
        // Non-favorite 'Delta'
        UserChannelListItemDto chDelta = new UserChannelListItemDto(
                ChannelId.random(),
                ChannelName.of("Delta Gaming"),
                ChannelUrl.of("https://twitch.tv/delta"),
                Platform.of("twitch"),
                false
        );

        // Another user's channel to ensure strict isolation
        UserId otherUserId = UserId.random();
        UserChannelListItemDto otherUserCh = new UserChannelListItemDto(
                ChannelId.random(),
                ChannelName.of("Other User Channel"),
                ChannelUrl.of("https://twitch.tv/other"),
                Platform.of("twitch"),
                true
        );

        // Arrange
        setupChannelsForUser(userId, List.of(chZeta, chAlphaFav, chBetaFav, chDelta));
        setupChannelsForUser(otherUserId, List.of(otherUserCh));

        // Act & Assert
        // Expected sort order:
        // 1. Favorites first: Alpha Streamer, Beta Player
        // 2. Non-favorites: Delta Gaming, Zeta Channel
        StepVerifier.create(getHandler().handle(new ListUserChannelsQuery(userId)))
                .assertNext(ch -> {
                    assertThat(ch.id()).isEqualTo(chAlphaFav.id());
                    assertThat(ch.name()).isEqualTo(ChannelName.of("Alpha Streamer"));
                    assertThat(ch.isFavorite()).isTrue();
                })
                .assertNext(ch -> {
                    assertThat(ch.id()).isEqualTo(chBetaFav.id());
                    assertThat(ch.name()).isEqualTo(ChannelName.of("Beta Player"));
                    assertThat(ch.isFavorite()).isTrue();
                })
                .assertNext(ch -> {
                    assertThat(ch.id()).isEqualTo(chDelta.id());
                    assertThat(ch.name()).isEqualTo(ChannelName.of("Delta Gaming"));
                    assertThat(ch.isFavorite()).isFalse();
                })
                .assertNext(ch -> {
                    assertThat(ch.id()).isEqualTo(chZeta.id());
                    assertThat(ch.name()).isEqualTo(ChannelName.of("Zeta Channel"));
                    assertThat(ch.isFavorite()).isFalse();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Scenario: filter channels by platform when platform filter is specified")
    void shouldFilterByPlatformWhenSpecified() {
        UserId userId = UserId.random();

        UserChannelListItemDto twitchCh = new UserChannelListItemDto(
                ChannelId.random(),
                ChannelName.of("Twitch Guy"),
                ChannelUrl.of("https://twitch.tv/guy"),
                Platform.of("twitch"),
                false
        );
        UserChannelListItemDto youtubeCh = new UserChannelListItemDto(
                ChannelId.random(),
                ChannelName.of("YouTube Star"),
                ChannelUrl.of("https://youtube.com/@star"),
                Platform.of("youtube"),
                true
        );
        UserChannelListItemDto kickCh = new UserChannelListItemDto(
                ChannelId.random(),
                ChannelName.of("Kick Pro"),
                ChannelUrl.of("https://kick.com/pro"),
                Platform.of("kick"),
                false
        );

        // Arrange
        setupChannelsForUser(userId, List.of(twitchCh, youtubeCh, kickCh));

        // Act & Assert (Filter only YOUTUBE)
        ChannelFilter filter = ChannelFilter.builder()
                .platform(Platform.of("youtube"))
                .build();

        StepVerifier.create(getHandler().handle(new ListUserChannelsQuery(userId, filter)))
                .assertNext(ch -> {
                    assertThat(ch.id()).isEqualTo(youtubeCh.id());
                    assertThat(ch.platform()).isEqualTo(Platform.of("youtube"));
                    assertThat(ch.name()).isEqualTo(ChannelName.of("YouTube Star"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Scenario: return only favorite channels when favoritesOnly is true in filter")
    void shouldFilterOnlyFavoritesWhenRequested() {
        UserId userId = UserId.random();

        UserChannelListItemDto fav1 = new UserChannelListItemDto(
                ChannelId.random(),
                ChannelName.of("Fav A"),
                ChannelUrl.of("https://twitch.tv/fava"),
                Platform.of("twitch"),
                true
        );
        UserChannelListItemDto nonFav = new UserChannelListItemDto(
                ChannelId.random(),
                ChannelName.of("Normal B"),
                ChannelUrl.of("https://twitch.tv/normalb"),
                Platform.of("twitch"),
                false
        );
        UserChannelListItemDto fav2 = new UserChannelListItemDto(
                ChannelId.random(),
                ChannelName.of("Fav C"),
                ChannelUrl.of("https://youtube.com/@favc"),
                Platform.of("youtube"),
                true
        );

        // Arrange
        setupChannelsForUser(userId, List.of(fav1, nonFav, fav2));

        // Act & Assert (favoritesOnly = true)
        ChannelFilter filter = ChannelFilter.builder()
                .favoritesOnly(true)
                .build();

        StepVerifier.create(getHandler().handle(new ListUserChannelsQuery(userId, filter)))
                .assertNext(ch -> {
                    assertThat(ch.id()).isEqualTo(fav1.id());
                    assertThat(ch.isFavorite()).isTrue();
                })
                .assertNext(ch -> {
                    assertThat(ch.id()).isEqualTo(fav2.id());
                    assertThat(ch.isFavorite()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Scenario: filter channels by search name query")
    void shouldFilterByNameQueryWhenSpecified() {
        UserId userId = UserId.random();

        UserChannelListItemDto streamer1 = new UserChannelListItemDto(
                ChannelId.random(),
                ChannelName.of("Ibai Llanos"),
                ChannelUrl.of("https://twitch.tv/ibai"),
                Platform.of("twitch"),
                true
        );
        UserChannelListItemDto streamer2 = new UserChannelListItemDto(
                ChannelId.random(),
                ChannelName.of("AuronPlay"),
                ChannelUrl.of("https://twitch.tv/auronplay"),
                Platform.of("twitch"),
                false
        );

        setupChannelsForUser(userId, List.of(streamer1, streamer2));

        ChannelFilter filter = ChannelFilter.builder()
                .nameQuery("ibai")
                .build();

        StepVerifier.create(getHandler().handle(new ListUserChannelsQuery(userId, filter)))
                .assertNext(ch -> assertThat(ch.name()).isEqualTo(ChannelName.of("Ibai Llanos")))
                .verifyComplete();
    }
}
