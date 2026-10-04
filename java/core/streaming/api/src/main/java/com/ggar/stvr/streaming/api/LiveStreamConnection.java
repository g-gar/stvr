package com.ggar.stvr.streaming.api;

import reactor.core.publisher.Flux;

/**
 * Handle representing an active raw video stream connection opened from a platform provider.
 */
public interface LiveStreamConnection {

    /**
     * Continuous reactive flux of video/audio chunks (typically MPEG-TS).
     *
     * @return flux of byte chunks
     */
    Flux<byte[]> data();

    /**
     * Checks if the underlying streaming process or network connection is active.
     *
     * @return true if alive, false otherwise
     */
    boolean isAlive();

    /**
     * Cancels or closes the stream connection cleanly, terminating origin processes.
     */
    void cancel();
}
