package com.ggar.stvr.streaming.entities;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Snapshot of the runtime state and metrics for an active stream hub on the STVR server.
 * Note: {@code connectedViewers} represents the local users connected to this STVR server's
 * preview hub, distinct from the global viewer count on the external platform (Twitch, Kick, etc.).
 */
@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ActiveStreamSnapshot implements Serializable {

    @EqualsAndHashCode.Include
    private final StreamKey streamKey;
    private final Instant startedAt;
    private final int connectedViewers;
    private final boolean active;

    @Builder(toBuilder = true)
    public ActiveStreamSnapshot(
            StreamKey streamKey,
            Instant startedAt,
            int connectedViewers,
            boolean active
    ) {
        this.streamKey = Objects.requireNonNull(streamKey, "streamKey cannot be null");
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt cannot be null");
        this.connectedViewers = Math.max(0, connectedViewers);
        this.active = active;
    }
}
