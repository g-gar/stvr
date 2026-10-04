package com.ggar.stvr.streaming.entities;

import com.ggar.stvr.catalog.entities.ChannelId;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite key identifying a unique live broadcast stream in the ingestion hub by channel and quality.
 */
public record StreamKey(ChannelId channelId, StreamQuality quality) implements Serializable {

    public StreamKey {
        Objects.requireNonNull(channelId, "channelId cannot be null");
        quality = quality != null ? quality : StreamQuality.BEST;
    }

    public static StreamKey of(ChannelId channelId, StreamQuality quality) {
        return new StreamKey(channelId, quality);
    }

    public static StreamKey of(ChannelId channelId, String quality) {
        return new StreamKey(channelId, StreamQuality.of(quality));
    }
}
