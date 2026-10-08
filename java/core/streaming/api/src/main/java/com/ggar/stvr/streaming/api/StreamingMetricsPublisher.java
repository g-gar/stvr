package com.ggar.stvr.streaming.api;

import com.ggar.stvr.catalog.entities.ChannelId;
import com.ggar.stvr.identity.entities.UserId;

/**
 * SPI Port for publishing streaming-related metrics, telemetry, and quota events (UC-STR-01).
 * Implementations forward these signals to Prometheus/Micrometer, domain event buses, or quota services.
 */
public interface StreamingMetricsPublisher {

    /**
     * Emits a metric or domain event when a visitor joins/subscribes to a channel's live stream.
     *
     * @param channelId target channel
     * @param userId user opening the stream (may be null for anonymous previews)
     */
    void emitVisitorIncreaseMetric(ChannelId channelId, UserId userId);

    /**
     * Emits a metric or domain event when a visitor disconnects from a channel's live stream.
     *
     * @param channelId target channel
     * @param userId user closing the stream (may be null for anonymous previews)
     */
    void emitVisitorDecreaseMetric(ChannelId channelId, UserId userId);

    /**
     * Emits a metric or accumulates bandwidth consumption towards the channel/user quota.
     *
     * @param channelId target channel
     * @param userId user consuming the bytes (may be null for anonymous previews)
     * @param bytes byte chunk size transferred
     */
    void emitQuotaMetric(ChannelId channelId, UserId userId, long bytes);
}
