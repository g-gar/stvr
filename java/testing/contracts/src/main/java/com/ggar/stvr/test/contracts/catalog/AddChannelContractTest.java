package com.ggar.stvr.test.contracts.catalog;

import com.ggar.stvr.catalog.api.AddChannelCommandHandler;
import com.ggar.stvr.catalog.api.AddChannelCommandHandler.AddChannelCommand;
import com.ggar.stvr.catalog.api.AddChannelCommandHandler.ChannelDto;
import com.ggar.stvr.catalog.api.exception.DuplicateChannelException;
import com.ggar.stvr.catalog.entities.ChannelId;
import com.ggar.stvr.catalog.entities.ChannelName;
import com.ggar.stvr.catalog.entities.ChannelUrl;
import com.ggar.stvr.catalog.entities.Platform;
import com.ggar.stvr.identity.entities.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract test suite defining the expected atomic behavior of AddChannelCommandHandler.
 */
public abstract class AddChannelContractTest {

    protected abstract AddChannelCommandHandler getHandler();

    /**
     * Verifies that the channel is persisted and associated with the given user in storage.
     */
    protected abstract boolean isChannelAssociatedWithUser(UserId userId, ChannelId channelId);

    /**
     * Checks if the channel exists in storage by its URL.
     */
    protected abstract boolean channelExistsInStorage(ChannelUrl url);

    @Test
    @DisplayName("Scenario: create channel using atomic AddChannelCommand")
    void shouldCreateChannelUsingAtomicCommand() {
        UserId userId = UserId.random();
        ChannelUrl url = ChannelUrl.of("https://twitch.tv/shroud");
        ChannelName name = ChannelName.of("shroud");

        AddChannelCommand command = new AddChannelCommand(
                userId,
                url,
                Platform.TWITCH,
                "shroud",
                name
        );

        StepVerifier.create(getHandler().handle(command))
                .assertNext(dto -> {
                    assertThat(dto.id()).isNotNull();
                    assertThat(dto.name()).isEqualTo(name);
                    assertThat(dto.url()).isEqualTo(url);
                    assertThat(dto.platform()).isEqualTo(Platform.TWITCH);
                    assertThat(dto.isFavorite()).isFalse();
                    assertThat(isChannelAssociatedWithUser(userId, dto.id())).isTrue();
                })
                .verifyComplete();

        assertThat(channelExistsInStorage(url)).isTrue();
    }

    @Test
    @DisplayName("Scenario: persist channels across multiple platforms")
    void shouldPersistChannelsAcrossMultiplePlatforms() {
        UserId userId = UserId.random();
        ChannelUrl ytUrl = ChannelUrl.of("https://youtube.com/live/ch1");
        ChannelUrl kickUrl = ChannelUrl.of("https://kick.com/xqc");

        AddChannelCommand ytCommand = new AddChannelCommand(
                userId,
                ytUrl,
                Platform.YOUTUBE,
                "ch1",
                ChannelName.of("YT Stream")
        );
        AddChannelCommand kickCommand = new AddChannelCommand(
                userId,
                kickUrl,
                Platform.KICK,
                "xqc",
                ChannelName.of("xQc")
        );

        StepVerifier.create(getHandler().handle(ytCommand))
                .assertNext(dto -> {
                    assertThat(dto.platform()).isEqualTo(Platform.YOUTUBE);
                    assertThat(dto.name()).isEqualTo(ChannelName.of("YT Stream"));
                })
                .verifyComplete();

        StepVerifier.create(getHandler().handle(kickCommand))
                .assertNext(dto -> {
                    assertThat(dto.platform()).isEqualTo(Platform.KICK);
                    assertThat(dto.name()).isEqualTo(ChannelName.of("xQc"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Scenario: throw NullPointerException when required fields are missing")
    void shouldThrowExceptionWhenRequiredFieldsAreMissing() {
        UserId userId = UserId.random();
        ChannelUrl url = ChannelUrl.of("https://twitch.tv/ninja");
        ChannelName name = ChannelName.of("Ninja");

        assertThatThrownBy(() -> new AddChannelCommand(null, url, Platform.TWITCH, "ninja", name))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new AddChannelCommand(userId, null, Platform.TWITCH, "ninja", name))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new AddChannelCommand(userId, url, null, "ninja", name))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new AddChannelCommand(userId, url, Platform.TWITCH, "ninja", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Scenario: throw DuplicateChannelException when channel already exists for user")
    void shouldThrowExceptionWhenChannelAlreadyExistsForUser() {
        UserId userId = UserId.random();
        ChannelUrl url = ChannelUrl.of("https://twitch.tv/duplicate_streamer");
        ChannelName name = ChannelName.of("Duplicate");

        AddChannelCommand command = new AddChannelCommand(
                userId,
                url,
                Platform.TWITCH,
                "duplicate_streamer",
                name
        );

        // First add succeeds
        StepVerifier.create(getHandler().handle(command))
                .expectNextCount(1)
                .verifyComplete();

        // Second add by same user fails
        StepVerifier.create(getHandler().handle(command))
                .expectErrorMatches(throwable ->
                        throwable instanceof DuplicateChannelException &&
                        ((DuplicateChannelException) throwable).getUserId().equals(userId) &&
                        ((DuplicateChannelException) throwable).getUrl().equals(url)
                )
                .verify();
    }

    @Test
    @DisplayName("Scenario: save and return channel with correct user association")
    void shouldSaveAndReturnChannelWithCorrectUserAssociation() {
        UserId userA = UserId.random();
        UserId userB = UserId.random();
        ChannelUrl url = ChannelUrl.of("https://twitch.tv/only_for_user_a");
        ChannelName name = ChannelName.of("User A Channel");

        AddChannelCommand commandA = new AddChannelCommand(
                userA,
                url,
                Platform.TWITCH,
                "only_for_user_a",
                name
        );

        StepVerifier.create(getHandler().handle(commandA))
                .assertNext(dto -> {
                    assertThat(isChannelAssociatedWithUser(userA, dto.id())).isTrue();
                    assertThat(isChannelAssociatedWithUser(userB, dto.id())).isFalse();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Scenario: associate existing channel when added by another user")
    void shouldAssociateExistingChannelWhenAddedByAnotherUser() {
        UserId userA = UserId.random();
        UserId userB = UserId.random();
        ChannelUrl sharedUrl = ChannelUrl.of("https://twitch.tv/ibai");
        ChannelName name = ChannelName.of("Ibai");

        AddChannelCommand commandA = new AddChannelCommand(
                userA,
                sharedUrl,
                Platform.TWITCH,
                "ibai",
                name
        );
        AddChannelCommand commandB = new AddChannelCommand(
                userB,
                sharedUrl,
                Platform.TWITCH,
                "ibai",
                name
        );

        // User A adds the channel
        ChannelDto[] userADto = new ChannelDto[1];
        StepVerifier.create(getHandler().handle(commandA))
                .consumeNextWith(dto -> userADto[0] = dto)
                .verifyComplete();

        // User B adds the SAME channel URL
        StepVerifier.create(getHandler().handle(commandB))
                .assertNext(dto -> {
                    // Reuses the exact same channel entity ID
                    assertThat(dto.id()).isEqualTo(userADto[0].id());
                    assertThat(dto.url()).isEqualTo(sharedUrl);
                })
                .verifyComplete();

        // Both users are associated with this channel
        assertThat(isChannelAssociatedWithUser(userA, userADto[0].id())).isTrue();
        assertThat(isChannelAssociatedWithUser(userB, userADto[0].id())).isTrue();
    }
}
