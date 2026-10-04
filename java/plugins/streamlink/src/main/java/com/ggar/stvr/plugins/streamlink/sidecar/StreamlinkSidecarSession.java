package com.ggar.stvr.plugins.streamlink.sidecar;

import com.ggar.stvr.packages.streamlink.executor.CommandResult;
import com.ggar.stvr.packages.streamlink.session.StreamlinkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * HTTP-backed implementation of StreamlinkSession connected to STVR's FastAPI sidecar container.
 * Streams video chunks directly from the HTTP response body reactively.
 */
public class StreamlinkSidecarSession implements StreamlinkSession {

    private static final Logger log = LoggerFactory.getLogger(StreamlinkSidecarSession.class);

    private final WebClient webClient;
    private final List<String> command;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final Sinks.Many<String> logsSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.One<CommandResult> resultSink = Sinks.one();

    public StreamlinkSidecarSession(WebClient webClient, List<String> command) {
        this.webClient = Objects.requireNonNull(webClient, "webClient cannot be null");
        this.command = Objects.requireNonNull(command, "command cannot be null");
    }

    @Override
    public Flux<byte[]> data() {
        return webClient.post()
                .uri("/api/v1/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .bodyValue(new StreamlinkSidecarCommandExecutor.ExecuteRequest(command))
                .retrieve()
                .bodyToFlux(DataBuffer.class)
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                })
                .doOnCancel(this::cancel)
                .doOnComplete(() -> {
                    logsSink.tryEmitComplete();
                    resultSink.tryEmitValue(new CommandResult(0, "", "Stream completed"));
                })
                .doOnError(err -> {
                    logsSink.tryEmitNext("Stream error: " + err.getMessage());
                    logsSink.tryEmitError(err);
                    resultSink.tryEmitValue(new CommandResult(1, "", err.getMessage()));
                });
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
        return !cancelled.get();
    }

    @Override
    public void cancel() {
        if (cancelled.compareAndSet(false, true)) {
            log.info("Cancelling sidecar StreamlinkSession for command: {}", command);
            logsSink.tryEmitComplete();
            resultSink.tryEmitValue(new CommandResult(0, "", "Cancelled by user"));
        }
    }
}
