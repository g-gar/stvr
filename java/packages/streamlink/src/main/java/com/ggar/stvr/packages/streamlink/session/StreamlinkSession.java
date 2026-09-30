package com.ggar.stvr.packages.streamlink.session;

import com.ggar.stvr.packages.streamlink.executor.CommandResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Represents an active streaming session with a running Streamlink process.
 * Provides concurrent reactive pipelines for video data (stdout) and log events (stderr),
 * process lifecycle monitoring, and explicit on-demand cancellation.
 */
public interface StreamlinkSession extends AutoCloseable {

    /**
     * Reactive stream of raw video bytes read continuously from stdout.
     *
     * @return Flux emitting byte array chunks as they arrive from Streamlink
     */
    Flux<byte[]> data();

    /**
     * Reactive stream of log lines, diagnostics, and status messages read continuously from stderr.
     * Multiple subscribers can observe this stream concurrently (e.g. logger, notification service, UI).
     *
     * @return Flux emitting stderr lines in real-time
     */
    Flux<String> logs();

    /**
     * Completes when the underlying Streamlink process terminates, emitting
     * the final {@link CommandResult} containing exitCode, stdout and stderr summary.
     *
     * @return Mono emitting the process completion result
     */
    Mono<CommandResult> result();

    /**
     * Checks if the underlying process is still actively running.
     *
     * @return true if running, false if terminated or cancelled
     */
    boolean isAlive();

    /**
     * Gracefully stops the streaming session by terminating the underlying process.
     * Signals SIGTERM, falls back to forced termination if needed, and completes the reactive streams.
     */
    void cancel();

    @Override
    default void close() {
        cancel();
    }
}
