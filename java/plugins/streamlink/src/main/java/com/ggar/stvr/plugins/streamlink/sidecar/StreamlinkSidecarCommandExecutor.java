package com.ggar.stvr.plugins.streamlink.sidecar;

import com.ggar.stvr.packages.streamlink.executor.CommandExecutor;
import com.ggar.stvr.packages.streamlink.executor.CommandResult;
import com.ggar.stvr.packages.streamlink.session.StreamlinkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

/**
 * CommandExecutor implementation communicating with STVR's Streamlink FastAPI sidecar service over HTTP.
 * Executes CLI commands remotely inside the container and streams live video chunks.
 */
public class StreamlinkSidecarCommandExecutor implements CommandExecutor {

    private static final Logger log = LoggerFactory.getLogger(StreamlinkSidecarCommandExecutor.class);

    private final WebClient webClient;
    private final String baseUrl;

    public StreamlinkSidecarCommandExecutor(String baseUrl) {
        this(WebClient.builder()
                .baseUrl(Objects.requireNonNull(baseUrl, "baseUrl cannot be null"))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build(), baseUrl);
    }

    public StreamlinkSidecarCommandExecutor(WebClient webClient, String baseUrl) {
        this.webClient = Objects.requireNonNull(webClient, "webClient cannot be null");
        this.baseUrl = baseUrl != null ? baseUrl : "";
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    @Override
    public Mono<CommandResult> execute(List<String> command) {
        Objects.requireNonNull(command, "command cannot be null");
        log.debug("Sending execute request to Streamlink HTTP sidecar at '{}': {}", baseUrl, command);

        ExecuteRequest request = new ExecuteRequest(command);

        return webClient.post()
                .uri("/api/v1/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ExecuteResponse.class)
                .map(res -> new CommandResult(res.exitCode(), res.stdout(), res.stderr()))
                .onErrorResume(ex -> {
                    log.error("Failed to execute command via Streamlink sidecar at '{}': {}", baseUrl, ex.getMessage());
                    return Mono.just(new CommandResult(1, "", "Streamlink HTTP sidecar error: " + ex.getMessage()));
                });
    }

    @Override
    public StreamlinkSession openSession(List<String> command) {
        Objects.requireNonNull(command, "command cannot be null");
        return new StreamlinkSidecarSession(webClient, command);
    }

    public record ExecuteRequest(List<String> command) {}

    public record ExecuteResponse(int exitCode, String stdout, String stderr) {}
}
