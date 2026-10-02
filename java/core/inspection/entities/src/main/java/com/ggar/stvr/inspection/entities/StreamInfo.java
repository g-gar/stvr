package com.ggar.stvr.inspection.entities;

import com.ggar.stvr.catalog.entities.ChannelUrl;
import com.ggar.stvr.catalog.entities.Platform;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    private final Map<String, Object> metadata;

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
            Map<String, Object> metadata
    ) {
        this.channelUrl = Objects.requireNonNull(channelUrl, "channelUrl cannot be null");
        this.platform = platform != null ? platform : Platform.CUSTOM;
        this.live = live;
        this.title = title != null ? title : "";
        this.category = category != null ? category : "";
        this.tags = tags != null ? List.copyOf(tags) : Collections.emptyList();
        this.availableQualities = availableQualities != null ? List.copyOf(availableQualities) : Collections.emptyList();
        this.startedAt = startedAt;
        this.metadata = metadata != null ? Map.copyOf(metadata) : Collections.emptyMap();
    }

    public static StreamInfo offline(ChannelUrl channelUrl, Platform platform) {
        return StreamInfo.builder()
                .channelUrl(channelUrl)
                .platform(platform)
                .live(false)
                .build();
    }
}
