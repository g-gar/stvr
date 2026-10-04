package com.ggar.stvr.inspection.entities;

import com.ggar.stvr.catalog.entities.ChannelUrl;
import com.ggar.stvr.catalog.entities.GenericPlatformMetadata;
import com.ggar.stvr.catalog.entities.Platform;
import com.ggar.stvr.catalog.entities.PlatformMetadata;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Ephemeral real-time stream information and telemetry produced by stream inspection.
 */
@Getter
@ToString
@EqualsAndHashCode
public class StreamInfo {

    private final ChannelUrl channelUrl;
    private final Platform platform;
    private final boolean live;
    private final String title;
    private final String category;
    private final List<String> tags;
    private final List<String> availableQualities;
    private final Instant startedAt;
    private final PlatformMetadata platformMetadata;

    @Builder(toBuilder = true)
    public StreamInfo(
            ChannelUrl channelUrl,
            Platform platform,
            boolean live,
            String title,
            String category,
            List<String> tags,
            List<String> availableQualities,
            Instant startedAt,
            PlatformMetadata platformMetadata
    ) {
        this.channelUrl = Objects.requireNonNull(channelUrl, "channelUrl cannot be null");
        this.platform = platform != null ? platform : Platform.of("custom");
        this.live = live;
        this.title = title != null ? title : "";
        this.category = category != null ? category : "";
        this.tags = tags != null ? List.copyOf(tags) : Collections.emptyList();
        this.availableQualities = availableQualities != null ? List.copyOf(availableQualities) : Collections.emptyList();
        this.startedAt = startedAt;
        this.platformMetadata = platformMetadata != null ? platformMetadata : GenericPlatformMetadata.empty();
    }

    public static StreamInfo offline(ChannelUrl channelUrl, Platform platform) {
        return StreamInfo.builder()
                .channelUrl(channelUrl)
                .platform(platform)
                .live(false)
                .build();
    }

    /**
     * Returns the metadata as a key-value map for serialization, persistence, and generic clients.
     *
     * @return map of metadata attributes
     */
    public Map<String, Object> getMetadata() {
        return platformMetadata.asMap();
    }

    /**
     * Retrieves the platform-specific stream metadata safely typed to the requested class.
     *
     * @param type target metadata class
     * @param <T> metadata type extending PlatformMetadata
     * @return Optional containing the typed metadata if matching, or empty
     */
    public <T extends PlatformMetadata> Optional<T> getMetadata(Class<T> type) {
        if (type != null && type.isInstance(platformMetadata)) {
            return Optional.of(type.cast(platformMetadata));
        }
        return Optional.empty();
    }

    public static class StreamInfoBuilder {
        public StreamInfoBuilder metadata(Map<String, Object> metadata) {
            this.platformMetadata = PlatformMetadata.of(metadata);
            return this;
        }
    }
}

