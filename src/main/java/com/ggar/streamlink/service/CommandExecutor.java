package com.ggar.streamlink.service;

import com.ggar.streamlink.model.StreamingProcessOutput;
import reactor.core.publisher.Mono;

/**
 * Interface for a generic command execution service.
 */
public interface CommandExecutor {

    /**
     * Executes a system command asynchronously and returns the entire output once complete.
     *
     * @param command The command and its arguments to execute.
     * @return A {@link Mono} that emits the standard output of the command as a single String.
     *         The mono will emit an error if the command fails (e.g., non-zero exit code).
     */
    Mono<String> execute(String... command);

    /**
     * Executes a system command and streams its standard output while providing access to standard error.
     *
     * @param command The command and its arguments to execute.
     * @return A {@link Mono} that emits a {@link StreamingProcessOutput} containing the stdout as a Flux,
     *         the stderr as an InputStream, and the process object.
     *         The lifecycle of the process is tied to the stdout Flux.
     */
    Mono<StreamingProcessOutput> executeAndStream(String... command);
}
