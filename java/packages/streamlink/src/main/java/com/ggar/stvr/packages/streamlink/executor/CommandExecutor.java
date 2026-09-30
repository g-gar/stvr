package com.ggar.stvr.packages.streamlink.executor;

import com.ggar.stvr.packages.streamlink.session.StreamlinkSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Port/Interface for executing Streamlink commands.
 * Consuming projects or modules inject their preferred execution strategy
 * (e.g. host OS process, Docker container runner, remote worker, or mock in tests).
 */
public interface CommandExecutor {

    /**
     * Executes a command to completion, returning the exit code, stdout, and stderr.
     *
     * @param command list of command arguments (e.g. ["streamlink", "--json", "https://..."])
     * @return Mono emitting the CommandResult
     */
    Mono<CommandResult> execute(List<String> command);

    /**
     * Opens an interactive streaming session providing concurrent video data (stdout),
     * real-time log messages (stderr), process monitoring, and cancellation.
     *
     * @param command list of command arguments (e.g. ["streamlink", "https://...", "best", "-O"])
     * @return active StreamlinkSession
     */
    StreamlinkSession openSession(List<String> command);

    /**
     * Executes a command and streams its standard output as a reactive byte stream.
     * Default implementation delegates to {@link #openSession(List)} and returns its {@link StreamlinkSession#data()}.
     *
     * @param command list of command arguments (e.g. ["streamlink", "https://...", "best", "-O"])
     * @return Flux of byte arrays emitted as data arrives
     */
    default Flux<byte[]> stream(List<String> command) {
        return openSession(command).data();
    }
}
