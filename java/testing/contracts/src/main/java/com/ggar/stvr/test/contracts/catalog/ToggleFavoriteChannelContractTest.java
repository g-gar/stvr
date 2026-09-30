package com.ggar.stvr.test.contracts.catalog;

import com.ggar.stvr.catalog.api.exception.ChannelNotFoundException;
import com.ggar.stvr.catalog.api.ToggleFavoriteChannelCommandHandler;
import com.ggar.stvr.catalog.api.ToggleFavoriteChannelCommandHandler.ToggleFavoriteChannelCommand;
import com.ggar.stvr.catalog.entities.ChannelId;
import com.ggar.stvr.identity.entities.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Generic contract test suite for UC-CAT-02: Toggle Favorite Channel.
 * Can be executed against mocked units, real database integration, or load test suites.
 */
public abstract class ToggleFavoriteChannelContractTest {

    protected abstract ToggleFavoriteChannelCommandHandler getHandler();

    /**
     * Prepares a channel with the given initial favorite status for the user.
     *
     * @param userId The user ID.
     * @param channelId The channel ID.
     * @param initialFavorite The initial favorite flag value.
     */
    protected abstract void setupChannelWithFavoriteStatus(UserId userId, ChannelId channelId, boolean initialFavorite);

    /**
     * Verifies that the channel in storage reflects the expected favorite flag.
     *
     * @param userId The user ID.
     * @param channelId The channel ID.
     * @param expectedFavorite Expected favorite status in storage.
     */
    protected abstract void verifyFavoriteStatusInStorage(UserId userId, ChannelId channelId, boolean expectedFavorite);

    @Test
    @DisplayName("Scenario: toggle favorite status from false to true")
    void shouldToggleFavoriteFromFalseToTrue() {
        UserId userId = UserId.random();
        ChannelId channelId = ChannelId.random();

        setupChannelWithFavoriteStatus(userId, channelId, false);

        StepVerifier.create(getHandler().handle(new ToggleFavoriteChannelCommand(userId, channelId)))
                .assertNext(response -> {
                    assertThat(response.channelId()).isEqualTo(channelId);
                    assertThat(response.isFavorite()).isTrue();
                })
                .verifyComplete();

        verifyFavoriteStatusInStorage(userId, channelId, true);
    }

    @Test
    @DisplayName("Scenario: toggle favorite status from true to false")
    void shouldToggleFavoriteFromTrueToFalse() {
        UserId userId = UserId.random();
        ChannelId channelId = ChannelId.random();

        setupChannelWithFavoriteStatus(userId, channelId, true);

        StepVerifier.create(getHandler().handle(new ToggleFavoriteChannelCommand(userId, channelId)))
                .assertNext(response -> {
                    assertThat(response.channelId()).isEqualTo(channelId);
                    assertThat(response.isFavorite()).isFalse();
                })
                .verifyComplete();

        verifyFavoriteStatusInStorage(userId, channelId, false);
    }

    @Test
    @DisplayName("Scenario: toggle favorite for one user without affecting other users sharing same channel")
    void shouldToggleFavoriteWithoutAffectingOtherUsersSharingSameChannel() {
        UserId userA = UserId.random();
        UserId userB = UserId.random();
        ChannelId sharedChannelId = ChannelId.random();

        setupChannelWithFavoriteStatus(userA, sharedChannelId, false);
        setupChannelWithFavoriteStatus(userB, sharedChannelId, false);

        StepVerifier.create(getHandler().handle(new ToggleFavoriteChannelCommand(userA, sharedChannelId)))
                .assertNext(response -> {
                    assertThat(response.channelId()).isEqualTo(sharedChannelId);
                    assertThat(response.isFavorite()).isTrue();
                })
                .verifyComplete();

        verifyFavoriteStatusInStorage(userA, sharedChannelId, true);
        verifyFavoriteStatusInStorage(userB, sharedChannelId, false);
    }

    @Test
    @DisplayName("Scenario: throw ChannelNotFoundException when channel does not exist for user")
    void shouldThrowNotFoundWhenChannelDoesNotExist() {
        UserId userId = UserId.random();
        ChannelId nonExistentChannelId = ChannelId.random();

        StepVerifier.create(getHandler().handle(new ToggleFavoriteChannelCommand(userId, nonExistentChannelId)))
                .expectErrorMatches(throwable ->
                        throwable instanceof ChannelNotFoundException &&
                        ((ChannelNotFoundException) throwable).getChannelId().equals(nonExistentChannelId) &&
                        ((ChannelNotFoundException) throwable).getUserId().equals(userId)
                )
                .verify();
    }
}
