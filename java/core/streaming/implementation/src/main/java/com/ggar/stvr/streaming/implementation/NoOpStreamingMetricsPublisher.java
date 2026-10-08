package com.ggar.stvr.streaming.implementation;

import com.ggar.stvr.catalog.entities.ChannelId;
import com.ggar.stvr.identity.entities.UserId;
import com.ggar.stvr.streaming.api.StreamingMetricsPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default fallback implementation of {@link StreamingMetricsPublisher} that logs metrics without external side-effects.
 * Replaced when a full metrics/quota engine bean is provided.
 */
@Component
public class NoOpStreamingMetricsPublisher implements StreamingMetricsPublisher {

    private static final Logger log = LoggerFactory.getLogger(NoOpStreamingMetricsPublisher.class);

    @Override
    public void emitVisitorIncreaseMetric(ChannelId channelId, UserId userId) {
        log.debug("Metric: Visitor joined stream for channel {} (user: {})",
                channelId.value(), userId != null ? userId.value() : "anonymous");
    }

    @Override
    public void emitVisitorDecreaseMetric(ChannelId channelId, UserId userId) {
        log.debug("Metric: Visitor left stream for channel {} (user: {})",
                channelId.value(), userId != null ? userId.value() : "anonymous");
    }

    @Override
    public void emitQuotaMetric(ChannelId channelId, UserId userId, long bytes) {
        log.trace("Metric: Transferred {} bytes for channel {} (user: {})",
                bytes, channelId.value(), userId != null ? userId.value() : "anonymous");
    }
}
