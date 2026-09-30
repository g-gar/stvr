package com.ggar.stvr.packages.streamlink.session;

import com.ggar.stvr.packages.streamlink.executor.CommandResult;
import com.ggar.stvr.packages.streamlink.i18n.StreamlinkMessages;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Operating system process-backed implementation of {@link StreamlinkSession}.
 * Concurrently drains stdout (video data) and stderr (diagnostics and lifecycle logs)
 * to prevent operating system pipe deadlocks and provide real-time reactive streams.
 */
@Slf4j
public class LocalProcessStreamlinkSession implements StreamlinkSession {

    private final Process process;
    private final List<String> command;
    private final int bufferSize;

    private final Sinks.Many<byte[]> dataSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<String> logsSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.One<CommandResult> resultSink = Sinks.one();

    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final StringBuilder stderrSummary = new StringBuilder();

    public LocalProcessStreamlinkSession(Process process, List<String> command, int bufferSize) {
        this.process = Objects.requireNonNull(process, StreamlinkMessages.get("error.null_arg", "process"));
        this.command = Objects.requireNonNull(command, StreamlinkMessages.get("error.null_arg", "command"));
        this.bufferSize = bufferSize > 0 ? bufferSize : 8192;

        startPumps();
    }

    private void startPumps() {
        // Pump 1: Concurrently stream stdout bytes
        Schedulers.boundedElastic().schedule(() -> {
            byte[] buffer = new byte[bufferSize];
            try (InputStream is = process.getInputStream()) {
                int n;
                while (!cancelled.get() && (n = is.read(buffer)) != -1) {
                    byte[] chunk = new byte[n];
                    System.arraycopy(buffer, 0, chunk, 0, n);
                    dataSink.tryEmitNext(chunk);
                }
                dataSink.tryEmitComplete();
            } catch (IOException e) {
                if (!cancelled.get()) {
                    log.warn("Error reading stdout from process pid={}: {}", process.pid(), e.getMessage());
                    dataSink.tryEmitError(e);
                } else {
                    dataSink.tryEmitComplete();
                }
            }
        });

        // Pump 2: Concurrently drain and stream stderr lines
        Schedulers.boundedElastic().schedule(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while (!cancelled.get() && (line = reader.readLine()) != null) {
                    stderrSummary.append(line).append(System.lineSeparator());
                    logsSink.tryEmitNext(line);
                }
                logsSink.tryEmitComplete();
            } catch (IOException e) {
                if (!cancelled.get()) {
                    log.warn("Error reading stderr from process pid={}: {}", process.pid(), e.getMessage());
                    logsSink.tryEmitError(e);
                } else {
                    logsSink.tryEmitComplete();
                }
            }
        });

        // Pump 3: Monitor process termination and complete result Mono
        process.onExit().thenAccept(p -> Schedulers.boundedElastic().schedule(() -> {
            int exitCode = p.exitValue();
            log.debug("Streamlink process pid={} finished with exitCode={}", p.pid(), exitCode);

            dataSink.tryEmitComplete();
            logsSink.tryEmitComplete();

            CommandResult cmdResult = new CommandResult(exitCode, "", stderrSummary.toString());
            resultSink.tryEmitValue(cmdResult);
        }));
    }

    @Override
    public Flux<byte[]> data() {
        return dataSink.asFlux().doOnCancel(this::cancel);
    }

    @Override
    public Flux<String> logs() {
        return logsSink.asFlux();
    }

    @Override
    public Mono<CommandResult> result() {
        return resultSink.asMono();
    }

    @Override
    public boolean isAlive() {
        return process.isAlive() && !cancelled.get();
    }

    @Override
    public void cancel() {
        if (cancelled.compareAndSet(false, true)) {
            if (process.isAlive()) {
                log.info("Cancelling StreamlinkSession, destroying process pid={}", process.pid());
                process.destroy();

                Schedulers.boundedElastic().schedule(() -> {
                    if (process.isAlive()) {
                        log.warn("Process pid={} did not exit after grace period, killing forcibly", process.pid());
                        process.destroyForcibly();
                    }
                }, 2, TimeUnit.SECONDS);
            }

            dataSink.tryEmitComplete();
            logsSink.tryEmitComplete();
        }
    }
}
