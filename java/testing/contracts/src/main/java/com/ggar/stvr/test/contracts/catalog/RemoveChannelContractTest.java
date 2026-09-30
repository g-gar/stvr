package com.ggar.stvr.test.contracts.catalog;

import com.ggar.stvr.catalog.api.RemoveChannelCommandHandler;
import com.ggar.stvr.catalog.api.RemoveChannelCommandHandler.RemoveChannelCommand;
import com.ggar.stvr.catalog.api.exception.ChannelNotFoundException;
import com.ggar.stvr.catalog.entities.ChannelId;
import com.ggar.stvr.identity.entities.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Generic contract test suite for UC-CAT-04: Remove Channel.
 * Can be executed against mocked units, real database integration, or load test suites.
 */
public abstract class RemoveChannelContractTest {

    protected abstract RemoveChannelCommandHandler getHandler();

    /**
     * Associates a channel with a given user in storage.
     *
     * @param userId The user ID.
     * @param channelId The channel ID.
     */
    protected abstract void setupChannelForUser(UserId userId, ChannelId channelId);

    /**
     * Checks if the channel association exists for the user in storage.
     *
     * @param userId The user ID.
     * @param channelId The channel ID.
     * @return true if channel is present, false otherwise.
     */
    protected abstract boolean isChannelAssociatedWithUser(UserId userId, ChannelId channelId);

    @Test
    @DisplayName("Scenario: remove channel successfully while maintaining user isolation")
    void shouldRemoveChannelSuccessfully() {
        UserId userId = UserId.random();
        ChannelId ch1 = ChannelId.random();
        ChannelId ch2 = ChannelId.random();

        UserId otherUserId = UserId.random();
        ChannelId otherUserCh = ChannelId.random();

        setupChannelForUser(userId, ch1);
        setupChannelForUser(userId, ch2);
        setupChannelForUser(otherUserId, otherUserCh);

        StepVerifier.create(getHandler().handle(new RemoveChannelCommand(userId, ch1)))
                .verifyComplete();

        assertThat(isChannelAssociatedWithUser(userId, ch1)).isFalse();
        assertThat(isChannelAssociatedWithUser(userId, ch2)).isTrue();
        assertThat(isChannelAssociatedWithUser(otherUserId, otherUserCh)).isTrue();
    }

    @Test
    @DisplayName("Scenario: remove channel from user catalog without affecting other users sharing the same channel")
    void shouldRemoveChannelWithoutAffectingOtherUsersSharingSameChannel() {
        UserId userA = UserId.random();
        UserId userB = UserId.random();
        ChannelId sharedChannelId = ChannelId.random();

        setupChannelForUser(userA, sharedChannelId);
        setupChannelForUser(userB, sharedChannelId);

        StepVerifier.create(getHandler().handle(new RemoveChannelCommand(userA, sharedChannelId)))
                .verifyComplete();

        // User A no longer has it
        assertThat(isChannelAssociatedWithUser(userA, sharedChannelId)).isFalse();
        // User B STILL has it
        assertThat(isChannelAssociatedWithUser(userB, sharedChannelId)).isTrue();
    }

    @Test
    @DisplayName("Scenario: throw ChannelNotFoundException when deleting non-existent channel")
    void shouldThrowNotFoundWhenDeletingNonExistentChannel() {
        UserId userId = UserId.random();
        ChannelId nonExistentChannelId = ChannelId.random();

        StepVerifier.create(getHandler().handle(new RemoveChannelCommand(userId, nonExistentChannelId)))
                .expectErrorMatches(throwable ->
                        throwable instanceof ChannelNotFoundException &&
                        ((ChannelNotFoundException) throwable).getChannelId().equals(nonExistentChannelId) &&
                        ((ChannelNotFoundException) throwable).getUserId().equals(userId)
                )
                .verify();
    }
}
