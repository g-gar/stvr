package com.ggar.stvr.packages.streamlink.executor;

import com.ggar.stvr.packages.streamlink.exception.StreamlinkExecutionException;
import com.ggar.stvr.packages.streamlink.i18n.StreamlinkMessages;
import com.ggar.stvr.packages.streamlink.session.LocalProcessStreamlinkSession;
import com.ggar.stvr.packages.streamlink.session.StreamlinkSession;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Standard implementation of {@link CommandExecutor} that runs commands
 * as local operating system processes using {@link ProcessBuilder}.
 */
@Slf4j
public class LocalProcessExecutor implements CommandExecutor {

    private static final int BUFFER_SIZE = 8192;

    @Override
    public Mono<CommandResult> execute(List<String> command) {
        return Mono.<CommandResult>create(sink -> {
            try {
                log.debug("Spawning local process: {}", String.join(" ", command));
                long startTime = System.currentTimeMillis();

                ProcessBuilder pb = new ProcessBuilder(command);
                Process process = pb.start();

                sink.onCancel(() -> {
                    if (process.isAlive()) {
                        log.debug("Process execution cancelled by downstream subscriber, terminating: {}", command.get(0));
                        process.destroyForcibly();
                    }
                });

                // Read stdout and stderr concurrently to prevent pipe buffer deadlock
                CompletableFuture<String> stdoutFuture = CompletableFuture.supplyAsync(() -> readStream(process.getInputStream()));
                CompletableFuture<String> stderrFuture = CompletableFuture.supplyAsync(() -> readStream(process.getErrorStream()));

                int exitCode = process.waitFor();
                long elapsed = System.currentTimeMillis() - startTime;
                String stdout = stdoutFuture.join();
                String stderr = stderrFuture.join();

                log.debug("Local process finished with exitCode={} in {}ms: {}", exitCode, elapsed, command.get(0));
                sink.success(new CommandResult(exitCode, stdout, stderr));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Process interrupted: {}", command.get(0), e);
                sink.error(e);
            } catch (Exception e) {
                log.error("Failed to execute process: {}", command.get(0), e);
                sink.error(e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public StreamlinkSession openSession(List<String> command) {
        try {
            log.info("Starting local process StreamlinkSession: {}", String.join(" ", command));
            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            return new LocalProcessStreamlinkSession(process, command, BUFFER_SIZE);
        } catch (Exception e) {
            log.error("Failed to start process for StreamlinkSession: {}", command.get(0), e);
            throw new StreamlinkExecutionException(
                    StreamlinkMessages.get("error.process_spawn_failed", command.get(0), e.getMessage()), e);
        }
    }

    @Override
    public Flux<byte[]> stream(List<String> command) {
        return openSession(command).data();
    }

    private String readStream(InputStream is) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            byte[] buf = new byte[BUFFER_SIZE];
            int n;
            while ((n = is.read(buf)) != -1) {
                os.write(buf, 0, n);
            }
            return os.toString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }
}
