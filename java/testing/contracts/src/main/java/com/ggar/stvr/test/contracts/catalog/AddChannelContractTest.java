package com.ggar.stvr.test.contracts.catalog;

import com.ggar.stvr.catalog.api.AddChannelCommandHandler;
import com.ggar.stvr.catalog.api.AddChannelCommandHandler.AddChannelCommand;
import com.ggar.stvr.catalog.api.AddChannelCommandHandler.ChannelDto;
import com.ggar.stvr.catalog.api.exception.DuplicateChannelException;
import com.ggar.stvr.catalog.api.exception.InvalidChannelUrlException;
import com.ggar.stvr.catalog.api.exception.UnsupportedPlatformException;
import com.ggar.stvr.catalog.api.inspector.ChannelInspectionResult;
import com.ggar.stvr.catalog.entities.ChannelId;
import com.ggar.stvr.catalog.entities.ChannelName;
import com.ggar.stvr.catalog.entities.ChannelUrl;
import com.ggar.stvr.catalog.entities.Platform;
import com.ggar.stvr.identity.entities.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Generic contract test suite for UC-CAT-01: Add Channel.
 * Can be executed against mocked units, real database integration, or load test suites.
 */
public abstract class AddChannelContractTest {

    protected abstract AddChannelCommandHandler getHandler();

    /**
     * Configures the inspector environment with mock/stub inspection result for the given URL.
     */
    protected abstract void registerInspectorResult(ChannelUrl url, ChannelInspectionResult result);

    /**
     * Verifies that the channel is persisted and associated with the given user in storage.
     */
    protected abstract boolean isChannelAssociatedWithUser(UserId userId, ChannelId channelId);

    /**
     * Checks if the channel exists in storage by its URL.
     */
    protected abstract boolean channelExistsInStorage(ChannelUrl url);

    @Test
    @DisplayName("Scenario: create channel using resolved inspector metadata")
    void shouldCreateChannelUsingResolvedInspectorMetadata() {
        UserId userId = UserId.random();
        ChannelUrl url = ChannelUrl.of("https://twitch.tv/shroud");

        registerInspectorResult(url, ChannelInspectionResult.online(
                Platform.TWITCH,
                "shroud",
                ChannelName.of("shroud"),
                "VALORANT",
                Set.of("1080p60", "720p60", "480p")
        ));

        StepVerifier.create(getHandler().handle(new AddChannelCommand(userId, url)))
                .assertNext(dto -> {
                    assertThat(dto.id()).isNotNull();
                    assertThat(dto.name()).isEqualTo(ChannelName.of("shroud"));
                    assertThat(dto.url()).isEqualTo(url);
                    assertThat(dto.platform()).isEqualTo(Platform.TWITCH);
                    assertThat(dto.isFavorite()).isFalse();
                    assertThat(dto.isLive()).isTrue();
                    assertThat(dto.category()).isEqualTo("VALORANT");
                    assertThat(dto.availableQualities()).containsExactlyInAnyOrder("1080p60", "720p60", "480p");
                    assertThat(isChannelAssociatedWithUser(userId, dto.id())).isTrue();
                })
                .verifyComplete();

        assertThat(channelExistsInStorage(url)).isTrue();
    }

    @Test
    @DisplayName("Scenario: use custom name override when provided by user")
    void shouldUseCustomNameOverrideWhenProvidedByUser() {
        UserId userId = UserId.random();
        ChannelUrl url = ChannelUrl.of("https://twitch.tv/tarik");
        ChannelName customName = ChannelName.of("My Favorite Tarik");

        registerInspectorResult(url, ChannelInspectionResult.offline(
                Platform.TWITCH,
                "tarik",
                ChannelName.of("tarik")
        ));

        StepVerifier.create(getHandler().handle(new AddChannelCommand(userId, url, customName)))
                .assertNext(dto -> {
                    assertThat(dto.name()).isEqualTo(customName);
                    assertThat(dto.platform()).isEqualTo(Platform.TWITCH);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Scenario: capture initial live status and qualities when streaming")
    void shouldCaptureInitialLiveStatusAndQualitiesWhenChannelIsCurrentlyStreaming() {
        UserId userId = UserId.random();
        ChannelUrl url = ChannelUrl.of("https://youtube.com/live/ch1");

        registerInspectorResult(url, ChannelInspectionResult.online(
                Platform.YOUTUBE,
                "ch1",
                ChannelName.of("YT Stream"),
                "Live News",
                Set.of("1080p", "720p")
        ));

        StepVerifier.create(getHandler().handle(new AddChannelCommand(userId, url)))
                .assertNext(dto -> {
                    assertThat(dto.isLive()).isTrue();
                    assertThat(dto.category()).isEqualTo("Live News");
                    assertThat(dto.availableQualities()).contains("1080p", "720p");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Scenario: capture initial offline status when inspector reports offline")
    void shouldCaptureInitialOfflineStatusWhenInspectorReportsOffline() {
        UserId userId = UserId.random();
        ChannelUrl url = ChannelUrl.of("https://kick.com/xqc");

        registerInspectorResult(url, ChannelInspectionResult.offline(
                Platform.KICK,
                "xqc",
                ChannelName.of("xQc")
        ));

        StepVerifier.create(getHandler().handle(new AddChannelCommand(userId, url)))
                .assertNext(dto -> {
                    assertThat(dto.isLive()).isFalse();
                    assertThat(dto.category()).isNull();
                    assertThat(dto.availableQualities()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Scenario: fallback to custom platform when no inspector supports URL and custom name is present")
    void shouldFallbackToCustomPlatformWhenNoInspectorSupportsUrlAndCustomNameIsPresent() {
        UserId userId = UserId.random();
        ChannelUrl url = ChannelUrl.of("https://local.lan/live/cam.m3u8");
        ChannelName customName = ChannelName.of("Security Cam");

        // No inspector result registered -> unsupported URL

        StepVerifier.create(getHandler().handle(new AddChannelCommand(userId, url, customName)))
                .assertNext(dto -> {
                    assertThat(dto.platform()).isEqualTo(Platform.CUSTOM);
                    assertThat(dto.name()).isEqualTo(customName);
                    assertThat(dto.isLive()).isFalse();
                    assertThat(isChannelAssociatedWithUser(userId, dto.id())).isTrue();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Scenario: throw UnsupportedPlatformException when no inspector supports URL and no custom name")
    void shouldThrowUnsupportedPlatformExceptionWhenNoInspectorSupportsUrlAndNoCustomName() {
        UserId userId = UserId.random();
        ChannelUrl url = ChannelUrl.of("https://unsupported.service.com/stream");

        // No inspector result registered and no custom name

        StepVerifier.create(getHandler().handle(new AddChannelCommand(userId, url)))
                .expectErrorMatches(throwable ->
                        throwable instanceof UnsupportedPlatformException &&
                        ((UnsupportedPlatformException) throwable).getUrl().equals(url)
                )
                .verify();
    }

    @Test
    @DisplayName("Scenario: throw InvalidChannelUrlException when URL is blank or malformed")
    void shouldThrowExceptionWhenUrlIsBlankOrMalformed() {
        UserId userId = UserId.random();

        assertThatThrownBy(() -> AddChannelCommand.of(userId, ""))
                .isInstanceOf(InvalidChannelUrlException.class);

        assertThatThrownBy(() -> AddChannelCommand.of(userId, "   "))
                .isInstanceOf(InvalidChannelUrlException.class);

        assertThatThrownBy(() -> AddChannelCommand.of(userId, "http://"))
                .isInstanceOf(InvalidChannelUrlException.class);
    }

    @Test
    @DisplayName("Scenario: throw DuplicateChannelException when channel already exists for user")
    void shouldThrowExceptionWhenChannelAlreadyExistsForUser() {
        UserId userId = UserId.random();
        ChannelUrl url = ChannelUrl.of("https://twitch.tv/duplicate_streamer");

        registerInspectorResult(url, ChannelInspectionResult.offline(
                Platform.TWITCH,
                "duplicate_streamer",
                ChannelName.of("Duplicate")
        ));

        // First add succeeds
        StepVerifier.create(getHandler().handle(new AddChannelCommand(userId, url)))
                .expectNextCount(1)
                .verifyComplete();

        // Second add by same user fails
        StepVerifier.create(getHandler().handle(new AddChannelCommand(userId, url)))
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

        registerInspectorResult(url, ChannelInspectionResult.offline(
                Platform.TWITCH,
                "only_for_user_a",
                ChannelName.of("User A Channel")
        ));

        StepVerifier.create(getHandler().handle(new AddChannelCommand(userA, url)))
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

        registerInspectorResult(sharedUrl, ChannelInspectionResult.online(
                Platform.TWITCH,
                "ibai",
                ChannelName.of("Ibai"),
                "Charlando",
                Set.of("1080p60")
        ));

        // User A adds the channel
        ChannelDto[] userADto = new ChannelDto[1];
        StepVerifier.create(getHandler().handle(new AddChannelCommand(userA, sharedUrl)))
                .consumeNextWith(dto -> userADto[0] = dto)
                .verifyComplete();

        // User B adds the SAME channel URL
        StepVerifier.create(getHandler().handle(new AddChannelCommand(userB, sharedUrl)))
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
