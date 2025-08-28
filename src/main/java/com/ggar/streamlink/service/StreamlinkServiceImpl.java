package com.ggar.streamlink.service;

import com.ggar.streamlink.config.StreamlinkProperties;
import com.ggar.streamlink.model.StreamCaptureOutput;
import com.ggar.streamlink.model.StreamOutput;
import com.ggar.streamlink.service.parser.StreamDataParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamlinkServiceImpl implements StreamlinkService {

    private final StreamDataParser streamDataParser;
    private final StreamlinkProperties streamlinkProperties;

    @Override
    public Mono<Map<String, String>> getStreams(String url) {
        return Mono.fromCallable(() -> {
                log.debug("Checking available streams for URL: {}", url);
                String command = streamlinkProperties.getCommandTemplate().replace("${url}", url);
                ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", "-c", command);
                Process process = processBuilder.start();

                // In a real application, you'd use a proper JSON library (like Jackson)
                // to parse this output into a structured object.
                String jsonOutput = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                int exitCode = process.waitFor();

                if (exitCode != 0) {
                    String errorOutput = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                    log.error("Streamlink process failed for URL {} with exit code {}: {}", url, exitCode, errorOutput);
                    // Return an empty map to signify no streams found or an error.
                    return Map.<String, String>of();
                }

                // The monitoring service only cares if the map is empty or not.
                // A simple check for the presence of streams is sufficient.
                // If the "streams" object is not empty, the streamer is online.
                if (jsonOutput.contains("\"streams\": {}\"") || jsonOutput.contains("\"error\":")) {
                    log.debug("No playable streams found for URL: {}", url);
                    return Map.<String, String>of();
                } else {
                    log.debug("Streams found for URL: {}", url);
                    // We return a non-empty map. The content doesn't matter for the check.
                    return Map.of("best", "online");
                }
            })
            .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<StreamCaptureOutput> captureStream(String url, String quality) {
        return Mono.fromCallable(() -> {
                log.info("Starting streamlink process for URL: {} with quality: {}", url, quality);
                String command = streamlinkProperties.getStreamCaptureTemplate()
                    .replace("${url}", url)
                    .replace("${quality}", quality);
                ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", "-c", command);
                return processBuilder.start();
            })
            .map(process -> {
                Flux<DataBuffer> videoStream = toFlux(process.getInputStream()).share();
                Flux<StreamOutput> metadataStream = streamDataParser.parse(process.getErrorStream()).share();

                // Ensure the process is terminated when the streams are cancelled or completed
                return new StreamCaptureOutput(
                    videoStream.doOnTerminate(process::destroy),
                    metadataStream
                    , Mono.fromCallable(process::waitFor)
                        .subscribeOn(Schedulers.boundedElastic())
                );
            })
            .subscribeOn(Schedulers.boundedElastic());
    }

    private Flux<DataBuffer> toFlux(InputStream inputStream) {
        return DataBufferUtils.readInputStream(
            () -> inputStream,
            DefaultDataBufferFactory.sharedInstance,
            4096 // Buffer size
        );
    }
}
