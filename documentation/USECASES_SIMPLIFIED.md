# STVR - Simplified Use Cases for TDD Test Suite

This document defines the actionable, testable Use Cases across all bounded contexts. Each use case specifies inputs, primary logic, invariants, error handling, and concrete test scenarios to guide Test-Driven Development (TDD).

---

## 1. `core:catalog` (Channel & Platform Management)

### UC-CAT-01: Add Channel
- **Actor:** Authenticated User
- **Inputs:** `userId: UserId`, `url: ChannelUrl`, `customName: ChannelName?`
- **Primary Flow:**
  1. Validate URL syntax via `ChannelUrl` value object.
  2. Verify that this channel URL is not already added by this user (`DuplicateChannelException`).
  3. Check if channel URL already exists globally in the catalog (i.e. added by another user):
     - If it exists: reuse existing `Channel` entity, establish the `(:User)-[:TRACKS]->(:Channel)` association for this `userId`, and return `ChannelDto`.
     - If it does not exist: resolve channel identity via domain port `ChannelResolverFactory.getResolver(url)`:
       - Identify streaming platform, streamer/channel slug, and default channel name.
  4. If no resolver supports the URL:
     - If user provided `customName`, fallback to platform `CUSTOM` with user's custom name.
     - Otherwise, throw `UnsupportedPlatformException`.
  5. Create and persist `Channel` domain model (`id`, `url`, `platform`, `slug`, `name`) with resolved platform, slug, and name.
  6. Associate channel with user (`(:User)-[:TRACKS]->(:Channel)`).
  7. Return `ChannelDto` (containing resolved channel identity and favorite status).
- **Error Cases & Invariants:**
  - Blank or malformed URL -> throws `InvalidChannelUrlException`.
  - Duplicate URL for same user -> throws `DuplicateChannelException`.
  - Unsupported URL with no custom name fallback -> throws `UnsupportedPlatformException`.
  - Multiple users may track the exact same channel; the global channel entity is shared to avoid redundant polling.
  - Channel resolution is handled via pluggable domain port `ChannelResolver`.
  - Channel identity is modeled purely around stable attributes (`id`, `url`, `platform`, `slug`, `name`), keeping volatile live stream data decoupled in `core:inspection`.
- **TDD Test Scenarios:**
  - `shouldCreateChannelUsingResolvedIdentity()`
  - `shouldUseCustomNameOverrideWhenProvidedByUser()`
  - `shouldResolveAndPersistChannelsAcrossMultiplePlatforms()`
  - `shouldFallbackToCustomPlatformWhenNoResolverSupportsUrlAndCustomNameIsPresent()`
  - `shouldThrowUnsupportedPlatformExceptionWhenNoResolverSupportsUrlAndNoCustomName()`
  - `shouldThrowExceptionWhenUrlIsBlankOrMalformed()`
  - `shouldThrowExceptionWhenChannelAlreadyExistsForUser()`
  - `shouldSaveAndReturnChannelWithCorrectUserAssociation()`
  - `shouldAssociateExistingChannelWhenAddedByAnotherUser()`
  - `shouldCreateAndValidateChannelEntity()`

---

### UC-CAT-02: Toggle Favorite Channel
- **Actor:** Authenticated User
- **Inputs:** `channelId: ChannelId`, `userId: UserId`
- **Primary Flow:**
  1. Find channel association by `channelId` for `userId`.
  2. Toggle `isFavorite` flag (`true <-> false`) on the user-channel relationship (`[:TRACKS]`).
  3. Persist update in storage.
  4. Return updated `ToggleFavoriteResponseDto(channelId, isFavorite)`.
- **Error Cases & Invariants:**
  - Channel not found for user -> throws `ChannelNotFoundException`.
  - Favorite flag is strictly scoped to the user-channel relationship; toggling favorite for one user does not affect other users sharing the same channel.
- **TDD Test Scenarios:**
  - `shouldToggleFavoriteFromFalseToTrue()`
  - `shouldToggleFavoriteFromTrueToFalse()`
  - `shouldToggleFavoriteWithoutAffectingOtherUsersSharingSameChannel()`
  - `shouldThrowNotFoundWhenChannelDoesNotExist()`

---

### UC-CAT-03: List User Channels
- **Actor:** Authenticated User
- **Inputs:** `userId: UserId`, `filter: ChannelFilter?`
- **Primary Flow:**
  1. Query channels for user applying filter criteria (platforms, favorite status, name substring, live status, category, tags).
  2. Return reactive `Flux<UserChannelListItemDto>` sorted by favorite status (true first) then case-insensitive name.
- **TDD Test Scenarios:**
  - `shouldReturnAllChannelsForUser()`
  - `shouldFilterByPlatformWhenSpecified()`
  - `shouldFilterOnlyFavoritesWhenRequested()`
  - `shouldFilterByNameQueryWhenSpecified()`
  - `shouldFilterByLiveStatusWhenSpecified()`

---

### UC-CAT-04: Remove Channel
- **Actor:** Authenticated User
- **Inputs:** `channelId: ChannelId`, `userId: UserId`
- **Primary Flow:**
  1. Verify user association with `channelId`.
  2. Remove relationship `(:User)-[:TRACKS]->(:Channel)` between user and channel (`ChannelNotFoundException` if association does not exist).
  3. Ensure other users sharing the same channel retain their full access and relationship unaffected.
  4. If no users track the channel, channel is marked as orphaned / unmonitored.
- **Error Cases & Invariants:**
  - Channel not found for user -> throws `ChannelNotFoundException`.
  - Deleting a channel only detaches the requesting user; other users' catalog entries are never touched.
- **TDD Test Scenarios:**
  - `shouldRemoveChannelSuccessfully()`
  - `shouldRemoveChannelWithoutAffectingOtherUsersSharingSameChannel()`
  - `shouldThrowNotFoundWhenDeletingNonExistentChannel()`

---

## 2. `core:presence` (Live Presence & Online Status via Streamlink)

### UC-PRES-01: Check Channel Online Status
- **Actor:** System / User on demand
- **Inputs:** `channelId: String`
- **Primary Flow:**
  1. Retrieve channel by `channelId`.
  2. Invoke `StreamlinkInspector.inspect(channel.url)` asynchronously (`docker exec -i streamlink streamlink --json <url>`).
  3. Parse Streamlink JSON response:
     - If `streams` dictionary is non-empty:
       - Status = `ONLINE`.
       - Record available stream qualities (`1080p60`, `720p`, etc.) and stream title.
     - If `error` indicates "No playable streams found":
       - Status = `OFFLINE`.
  4. Compare with last known status (`ONLINE`, `OFFLINE`, `UNKNOWN`).
  5. If status changed:
     - Update presence state in memory and database.
     - Emit `ChannelStatusChangedEvent` on Reactor Event Bus.
- **TDD Test Scenarios:**
  - `shouldDetectOnlineStatusAndQualitiesWhenStreamsAreReturned()`
  - `shouldDetectOfflineStatusWhenNoStreamsFoundErrorReturned()`
  - `shouldEmitStatusChangedEventWhenStatusTransitionsFromOfflineToOnline()`
  - `shouldEmitStatusChangedEventWhenStatusTransitionsFromOnlineToOffline()`
  - `shouldNotEmitEventWhenStatusRemainsUnchanged()`
  - `shouldHandleStreamlinkInspectorErrorsGracefullyWithoutThrowing()`

---

### UC-PRES-02: Poll Favorite Channels Presence (Periodic Job)
- **Actor:** Scheduled Timer
- **Primary Flow:**
  1. Fetch all distinct active channels marked as favorites.
  2. Query presence concurrently via `StreamlinkInspector.inspect(url)` with bounded concurrency (`Flux.flatMap(..., concurrencyLimit)`).
  3. Emit status change events for any state transitions.
- **TDD Test Scenarios:**
  - `shouldPollAllFavoriteChannelsConcurrently()`
  - `shouldHandleStreamlinkTimeoutsGracefullyWithoutAbortingJob()`

---

## 3. `core:streaming` (Live Stream Ingestion & Multicast Hub)

### UC-STR-01: Open Live Preview Stream
- **Actor:** Authenticated User
- **Inputs:** `channelId: String`, `quality: String`, `userId: String`
- **Primary Flow:**
  1. Key stream ingestion by `(channelId, quality)`.
  2. Look up existing in-memory `ChannelStreamHub(channelId, quality)`.
  3. If hub does NOT exist:
     - Invoke `StreamlinkRunner` to start stream ingestion.
     - Initialize `ChannelStreamHub` with `Sinks.many().multicast()`.
  4. Register user's HTTP connection as an active `LivePreviewSink`.
  5. Return continuous `Flux<DataBuffer>` to browser HTML5 player.
  6. Emit telemetry event: `VIEWER_JOINED(channelId, quality, userId)`.
- **Error Cases & Invariants:**
  - Stream offline or invalid quality -> throws `StreamUnavailableException`.
- **TDD Test Scenarios:**
  - `shouldStartStreamlinkWhenNoActiveHubExists()`
  - `shouldReuseExistingHubWhenSecondUserRequestsSameStreamAndQuality()`
  - `shouldCreateIndependentHubsWhenDifferentQualitiesRequested()`
  - `shouldStreamByteBuffersToSubscriber()`
  - `shouldThrowExceptionWhenOriginStreamFails()`

---

### UC-STR-02: Close Live Preview Stream
- **Actor:** User disconnect / Channel zapping
- **Inputs:** `channelId: String`, `quality: String`, `userId: String`
- **Primary Flow:**
  1. Detach user's `LivePreviewSink` from `ChannelStreamHub`.
  2. Decrement active viewer count.
  3. Emit telemetry event: `VIEWER_LEFT(channelId, quality, userId)`.
  4. Evaluate Lifecycle Rule:
     - If `activeViewers == 0` AND `recordingActive == false`:
       - Trigger grace period timer (5 seconds).
       - Terminate Streamlink process cleanly if no new viewer attaches.
- **TDD Test Scenarios:**
  - `shouldDecrementViewerCountWhenUserDisconnects()`
  - `shouldKeepHubAliveWhenOtherViewersRemain()`
  - `shouldKeepHubAliveWhenNoViewersRemainButRecordingIsActive()`
  - `shouldShutdownHubWhenNoViewersAndNoRecordingAfterGracePeriod()`

---

## 4. `features:recorder` (Background & On-Demand Recording)

### UC-REC-01: Start Recording
- **Actor:** Authenticated User
- **Inputs:** `channelId: String`, `quality: String`, `profileId: String`, `userId: String`
- **Primary Flow:**
  1. Verify channel exists and user has permission.
  2. Ensure `ChannelStreamHub(channelId, quality)` is active (spins it up if not currently watched).
  3. Create `RecordingSession` domain entity with `status = RECORDING`.
  4. Resolve FFmpeg profile parameters (e.g. `nvenc-gpu` or `passthrough`).
  5. Attach `FfmpegRecorderSink` to the hub.
  6. Launch FFmpeg process (local or remote GPU worker), piping stream to destination `/recordings/{userId}/{fileName}.mkv`.
  7. Persist recording session in Neo4j with `[:OWNS]` relation to user.
  8. Emit `RECORDING_STARTED` event via SSE.
- **Error Cases:**
  - Channel not found -> throws `ChannelNotFoundException`.
  - Stream offline -> throws `StreamUnavailableException`.
- **TDD Test Scenarios:**
  - `shouldStartRecordingSessionAndAttachSinkToHub()`
  - `shouldLaunchFfmpegWithConfiguredProfileParameters()`
  - `shouldPersistRecordingNodeWithUserOwnership()`
  - `shouldEmitRecordingStartedEvent()`

---

### UC-REC-02: Stop Recording
- **Actor:** Authenticated User (Owner)
- **Inputs:** `recordingSessionId: String`, `userId: String`
- **Primary Flow:**
  1. Find recording session; verify `userId` is the owner.
  2. Detach `FfmpegRecorderSink` from stream hub.
  3. Close FFmpeg input stream / send graceful termination signal.
  4. Await FFmpeg process completion (exit code 0 ensures valid Matroska index).
  5. Compute final file size and duration.
  6. Update `RecordingSession` to `status = FINISHED`.
  7. Create and persist `VideoFile` node in Neo4j.
  8. Emit `RECORDING_STOPPED` event via SSE.
  9. Evaluate stream hub lifecycle (if viewers == 0, shut down Streamlink).
- **Error Cases:**
  - Session not found -> throws `RecordingSessionNotFoundException`.
  - User is not the owner -> throws `AccessDeniedException`.
- **TDD Test Scenarios:**
  - `shouldStopRecordingCleanlyAndPersistVideoFile()`
  - `shouldThrowAccessDeniedWhenUserDoesNotOwnRecording()`
  - `shouldTriggerStreamHubEvaluationAfterRecordingStops()`

---

### UC-REC-03: Delete Recording
- **Actor:** Authenticated User (Owner)
- **Inputs:** `videoId: String`, `userId: String`
- **Primary Flow:**
  1. Find `VideoFile` in Neo4j; verify user has `[:OWNS]` relationship.
  2. Delete physical file from disk (`Files.deleteIfExists(path)`).
  3. Delete `VideoFile` node and relationships from Neo4j.
  4. Return success confirmation.
- **Error Cases:**
  - Video not found -> throws `VideoNotFoundException`.
  - User is not the owner -> throws `AccessDeniedException`.
- **TDD Test Scenarios:**
  - `shouldDeletePhysicalFileAndGraphNodeWhenUserIsOwner()`
  - `shouldThrowAccessDeniedWhenUserDoesNotOwnVideo()`
  - `shouldHandleMissingPhysicalFileGracefullyWhileDeletingNode()`

---

## 5. `features:media` (Historical Video Streaming & Media Library)

### UC-MED-01: List User Recorded Videos
- **Actor:** Authenticated User
- **Inputs:** `userId: String`, `page: Int`, `size: Int`
- **Primary Flow:**
  1. Query Neo4j for videos owned by `userId`: `(u:User {id: userId})-[:OWNS]->(v:VideoFile)`.
  2. Return paginated `Flux<VideoItemDto>` sorted by creation date descending.
- **TDD Test Scenarios:**
  - `shouldReturnVideosOwnedByUserOnly()`
  - `shouldApplyPaginationCorrectly()`

---

### UC-MED-02: Stream Recorded Video with HTTP Range (206 Partial Content)
- **Actor:** Authenticated User
- **Inputs:** `videoId: String`, `userId: String`, `rangeHeader: String?`
- **Primary Flow:**
  1. Verify video ownership.
  2. Resolve file path on disk.
  3. If `Range` header is present (e.g. `bytes=1048576-`):
     - Calculate start byte, end byte, and content length.
     - Return HTTP `206 Partial Content` with `Content-Range`, `Accept-Ranges: bytes`.
  4. If `Range` header is absent:
     - Return HTTP `200 OK` with full stream.
- **Error Cases:**
  - Out of bounds range -> return HTTP `416 Range Not Satisfiable`.
- **TDD Test Scenarios:**
  - `shouldServePartialByteRangeWhenRangeHeaderProvided()`
  - `shouldReturn416WhenRangeHeaderIsInvalid()`
  - `shouldThrowAccessDeniedWhenUserDoesNotOwnVideo()`

---

## 6. `core:identity` (User Accounts & Authentication)

### UC-AUTH-01: Register User
- **Actor:** Anonymous / Admin
- **Inputs:** `username: String`, `password: String`, `role: Role`
- **Primary Flow:**
  1. Validate username syntax (3-30 chars, alphanumeric).
  2. Validate password strength (min 8 chars).
  3. Ensure username does not already exist.
  4. Hash password with BCrypt (cost 12).
  5. Persist `UserNode` in Neo4j.
  6. Return `UserDto` (without password hash).
- **TDD Test Scenarios:**
  - `shouldRegisterUserSuccessfullyWithHashedPassword()`
  - `shouldThrowExceptionWhenUsernameAlreadyTaken()`
  - `shouldRejectWeakPassword()`

---

### UC-AUTH-02: Authenticate User (Login)
- **Actor:** User
- **Inputs:** `username: String`, `password: String`
- **Primary Flow:**
  1. Find user by username.
  2. Verify password against stored BCrypt hash.
  3. If valid, issue JWT / security context.
  4. Return `AuthTokenResponse`.
- **Error Cases:**
  - Invalid username or password -> throws `BadCredentialsException`.
- **TDD Test Scenarios:**
  - `shouldAuthenticateAndGenerateTokenWhenCredentialsAreValid()`
  - `shouldRejectLoginWhenPasswordIsIncorrect()`
  - `shouldRejectLoginWhenUserDoesNotExist()`

---

### UC-AUTH-03: Revoke Token (Logout)
- **Actor:** Authenticated User
- **Inputs:** `token: String`
- **Primary Flow:**
  1. Validate token structure, signature, and extract `jti` (JWT ID) and expiration.
  2. Register `jti` in token revocation repository until token expiration.
  3. Subsequent validations of this token must fail.
  4. Return confirmation (boolean or empty completion).
- **Error Cases:**
  - Blank, malformed or invalid token signature -> throws `InvalidTokenException`.
- **TDD Test Scenarios:**
  - `shouldRevokeTokenSuccessfullyAndRejectSubsequentValidations()`
  - `shouldRejectRevocationWhenTokenIsBlankOrMalformed()`
  - `shouldHandleAlreadyExpiredOrInvalidTokenGracefully()`

---


## 7. `packages:streamlink` (Streamlink JSON Inspector & Docker Process Runner)

### UC-SL-01: Parse Streamlink JSON Inspection
- **Actor:** System (`StreamlinkInspector`)
- **Inputs:** `jsonOutput: String`, `exitCode: Int`
- **Primary Flow:**
  1. Inspect JSON output from `streamlink --json <url>`.
  2. If `streams` object is present:
     - Map `"plugin"` to `Platform` (e.g., `twitch` -> `TWITCH`, `youtube` -> `YOUTUBE`, `kick` -> `KICK`).
     - Extract `metadata` (`author`, `title`, `category`, `id`).
     - Extract available qualities list (e.g. `["1080p60", "720p60", "480p", "worst", "best"]`).
     - Return `StreamlinkInspectionResult.Live(...)`.
  3. If `error` field indicates "No playable streams found":
     - Return `StreamlinkInspectionResult.Offline(error)`.
  4. If `error` field indicates "No plugin can handle URL":
     - Return `StreamlinkInspectionResult.UnsupportedPlatform(error)`.
- **TDD Test Scenarios:**
  - `shouldParseSuccessfulLiveStreamlinkJsonWithMetadataAndQualities()`
  - `shouldParseOfflineStreamlinkJsonWhenNoPlayableStreamsFound()`
  - `shouldParseUnsupportedPlatformJsonWhenNoPluginCanHandleUrl()`
  - `shouldThrowInvalidStreamlinkJsonExceptionWhenOutputIsCorrupted()`

---

### UC-SL-02: Execute Streamlink Process in Docker
- **Actor:** System (`DockerStreamlinkRunner`)
- **Inputs:** `containerName: String = "streamlink"`, `commandArgs: List<String>`
- **Primary Flow:**
  1. Construct process execution args: `docker exec -i <containerName> streamlink <args>`.
  2. For `--json` inspection:
     - Run non-blocking command, capture stdout and exit code.
     - Return reactive `Mono<StreamlinkInspectionResult>`.
  3. For live stream ingestion (`--stdout`):
     - Pipe stdout reactive byte buffer stream (`Flux<DataBuffer>`).
     - Monitor stderr for errors and log warnings.
     - Support reactive cancellation to cleanly terminate process on downstream unsubscribe.
- **TDD Test Scenarios:**
  - `shouldBuildCorrectDockerExecArgumentsForJsonInspection()`
  - `shouldBuildCorrectDockerExecArgumentsForStdoutIngestion()`
  - `shouldPipeProcessStdoutAsReactiveDataBuffers()`
  - `shouldTerminateDockerProcessGracefullyOnFluxCancellation()`

