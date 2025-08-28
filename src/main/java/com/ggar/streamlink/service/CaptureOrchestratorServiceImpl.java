package com.ggar.streamlink.service;

import com.ggar.streamlink.model.CaptureInfo;
import com.ggar.streamlink.model.CaptureStatus;
import com.ggar.streamlink.model.StreamCaptureOutput;
import com.ggar.streamlink.model.StreamOutput;
import com.ggar.streamlink.model.dto.Streamer;
import com.ggar.streamlink.model.exception.CaptureNotFoundException;
import com.ggar.streamlink.repository.CaptureRepository;
import com.ggar.streamlink.repository.StreamerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Service
public class CaptureOrchestratorServiceImpl implements CaptureOrchestratorService {

    private final StreamlinkService streamlinkService;
    private final FileStorageService fileStorageService;
    private final StreamerRepository streamerRepository;
    private final CaptureRepository captureRepository;
    private final LoggingService loggingService;
    private final Path captureBasePath;

    public CaptureOrchestratorServiceImpl(
            StreamlinkService streamlinkService,
            FileStorageService fileStorageService,
            StreamerRepository streamerRepository,
            CaptureRepository captureRepository,
            LoggingService loggingService,
            @Value("${streamlink.capture.path:/tmp/captures}") String capturePath) {
        this.streamlinkService = streamlinkService;
        this.fileStorageService = fileStorageService;
        this.streamerRepository = streamerRepository;
        this.captureRepository = captureRepository;
        this.loggingService = loggingService;
        this.captureBasePath = Paths.get(capturePath);
    }

    /**
     * Internal record to hold the state and the subscription handle of a capture.
     */
    private record Capture(CaptureInfo info, Disposable subscription) {}

    private final Map<UUID, Capture> captures = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public Mono<CaptureInfo> startCapture(String url, String quality) {
        return streamerRepository.findByUrl(url)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("No streamer found for URL: " + url)))
            .flatMap(streamer -> {
                UUID id = UUID.randomUUID();
                Path destination = generateFilePath(id);

                com.ggar.streamlink.model.dto.Capture captureEntity = new com.ggar.streamlink.model.dto.Capture();
                captureEntity.setFilename(destination.getFileName().toString());
                captureEntity.setStartTime(Instant.now());
                captureEntity.setStreamer(streamer);

                return captureRepository.save(captureEntity)
                    .flatMap(savedCapture -> {
                        if (streamer.getCaptures() == null) {
                            streamer.setCaptures(new ArrayList<>());
                        }
                        streamer.getCaptures().add(savedCapture);
                        return streamerRepository.save(streamer);
                    })
                    .map(savedStreamer -> {
                        CaptureInfo initialCaptureInfo = new CaptureInfo(id, url, quality, destination, CaptureStatus.RUNNING, LocalDateTime.now(), null, null);
                        Sinks.One<CaptureInfo> captureInitiatedSignal = Sinks.one();
                        final Disposable.Swap disposable = Disposables.swap();

                        return streamlinkService.captureStream(url, quality)
                            .flatMap(output -> {
                                Flux<DataBuffer> videoStream = output.videoStream();
                                Flux<StreamOutput> metadataStream = output.metadataStream();

                                Mono<StreamOutput.ErrorOutput> fatalErrorSignal = metadataStream
                                    .ofType(StreamOutput.ErrorOutput.class)
                                    .filter(e -> !isNormalStreamEnd(e.message()))
                                    .doOnNext(e -> loggingService.warn("Received fatalErrorSignal for " + destination + ": " + e.message()).subscribe())
                                    .next()
                                    .cache();

                                Mono<Void> saveOperation = fileStorageService.saveStream(
                                    videoStream.takeUntilOther(fatalErrorSignal),
                                    destination
                                );

                                // Monitor for the first data chunk to confirm initiation
                                Mono<Void> monitorStreamStart = videoStream
                                    .next() // Wait for the first DataBuffer
                                    .doOnNext(dataBuffer -> {
                                        // First data buffer received, capture is truly initiated
                                        captures.put(id, new Capture(initialCaptureInfo, disposable));
                                        captureInitiatedSignal.tryEmitValue(initialCaptureInfo);
                                        loggingService.info("Capture " + id + " for url " + url + " initiated and data flowing.").subscribe();
                                    })
                                    .doOnError(error -> {
                                        // Error before first data, capture failed to initiate
                                        captureInitiatedSignal.tryEmitError(new RuntimeException("Failed to receive initial stream data: " + error.getMessage(), error)); // Propagate error to initiationMono
                                        loggingService.error("Capture " + id + " for url " + url + " failed to initiate: " + error.getMessage(), error).subscribe();
                                    })
                                    .switchIfEmpty(Mono.defer(() -> {
                                        // This path means videoStream.next() completed without emitting anything.
                                        // We emit an error to the initiation signal and log a warning.
                                        return loggingService.warn("Capture " + id + " for url " + url + " started but emitted no data.")
                                            .then(Mono.error(new IllegalStateException("Stream started but emitted no data."))); // Propagate error in main chain
                                    }))
                                    .then(); // Convert Flux to Mono<Void>

                                // Define the full capture lifecycle, including saving and error handling
                                Mono<Void> fullCaptureLifecycle = Mono.when(monitorStreamStart, saveOperation, output.exitCode().then())
                                    .then(fatalErrorSignal.hasElement()) // Check if a fatal error occurred
                                    .flatMap(hasFatalError -> {
                                        if (hasFatalError) {
                                            return fatalErrorSignal.flatMap(error ->
                                                loggingService.warn("Capture stopped due to a fatal stream error: '" + error.message() + "'. Deleting partial file: " + destination)
                                                    .then(fileStorageService.deleteFile(destination))
                                                    .then(Mono.<Void>error(new RuntimeException("Stream capture failed: " + error.message())))
                                            );
                                        } else {
                                            return loggingService.info("Capture completed successfully (stream ended by source). File saved at: " + destination)
                                                .then(Mono.empty());
                                        }
                                    })
                                    .doOnSuccess(v -> updateCaptureInfo(id, info -> info.withStatus(CaptureStatus.COMPLETED).withEndTime(LocalDateTime.now())))
                                    .doOnError(error -> updateCaptureInfo(id, info -> info.withStatus(CaptureStatus.FAILED).withEndTime(LocalDateTime.now()).withError(error.getMessage())))
                                    .doOnCancel(() -> updateCaptureInfo(id, info -> info.withStatus(CaptureStatus.STOPPED).withEndTime(LocalDateTime.now())))
                                    .doFinally(signalType -> captures.remove(id))
                                    ;

                                // Subscribe to the full capture lifecycle to start it in the background
                                disposable.replace(fullCaptureLifecycle.subscribe());

                                // Return the Mono from the sink. This Mono will complete when the first data chunk is received,
                                // or error if the stream fails to produce data.
                                return captureInitiatedSignal.asMono();
                            });
                    })
                    .flatMap(Function.identity());
            });
    }

    @Override
    public Flux<CaptureInfo> getActiveCaptures() {
        return Flux.fromIterable(captures.values())
            .map(Capture::info)
            .filter(info -> info.status() == CaptureStatus.RUNNING);
    }

    @Override
    public Mono<CaptureInfo> getCapture(UUID id) {
        return Mono.justOrEmpty(captures.get(id))
            .map(Capture::info)
            .switchIfEmpty(Mono.error(new CaptureNotFoundException(id)));
    }

    @Override
    public Mono<Void> stopCapture(UUID id) {
        return getCapture(id)
            .doOnNext(info -> {
                if (info.status() != CaptureStatus.RUNNING) {
                    throw new IllegalStateException("Capture " + id + " is not in RUNNING state. Current status: " + info.status());
                }
                captures.get(id).subscription().dispose();
            })
            .then();
    }

    private Path generateFilePath(UUID id) {
        String filename = String.format("%s.ts", id);
        return captureBasePath.resolve(filename);
    }

    private boolean isNormalStreamEnd(String errorMessage) {
        String lowerCaseError = errorMessage.toLowerCase();
        return lowerCaseError.contains("stream ended") ||
               lowerCaseError.contains("closing stream") ||
               lowerCaseError.contains("pipe copy aborted");
    }

    private void updateCaptureInfo(UUID id, Function<CaptureInfo, CaptureInfo> updater) {
        captures.computeIfPresent(id, (key, existingCapture) -> {
            CaptureInfo newInfo = updater.apply(existingCapture.info());
            loggingService.debug("Updating capture " + id + " status to " + newInfo.status()).subscribe();
            return new Capture(newInfo, existingCapture.subscription());
        });
    }
}