package com.ggar.streamlink.service;

import com.ggar.streamlink.model.CaptureInfo;
import com.ggar.streamlink.model.dto.StreamStatusEvent;
import com.ggar.streamlink.repository.StreamerRepository;
import com.ggar.streamlink.model.dto.StreamerStatusDetail;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class StreamerMonitoringServiceImpl implements StreamerMonitoringService {

    private final StreamlinkService streamlinkService;
    private final StreamerRepository streamerRepository;
    private final CaptureOrchestratorService captureOrchestratorService;
    private final LoggingService loggingService;
    private final Map<Long, StreamStatus> streamerStatuses = new ConcurrentHashMap<>();
    private final Sinks.Many<StreamStatusEvent> statusEventSink = Sinks.many().multicast().onBackpressureBuffer();

    public StreamerMonitoringServiceImpl(
        StreamlinkService streamlinkService,
        StreamerRepository streamerRepository,
        CaptureOrchestratorService captureOrchestratorService,
        LoggingService loggingService
    ) {
        this.streamlinkService = streamlinkService;
        this.streamerRepository = streamerRepository;
        this.captureOrchestratorService = captureOrchestratorService;
        this.loggingService = loggingService;
    }

    @PostConstruct
    public void initializeStreamerStatuses() {
        loggingService.info("Initializing streamer statuses from database...").subscribe();
        streamerRepository.findAll()
            .doOnNext(streamer -> streamerStatuses.put(streamer.getId(), StreamStatus.UNKNOWN))
            .doOnComplete(() -> loggingService.info("Finished initializing " + streamerStatuses.size() + " streamers.").subscribe())
            .subscribe();
    }

    @Override
    // @Scheduled(fixedRate = 60, timeUnit = TimeUnit.SECONDS)
    public void performScheduledCheck() {
        loggingService.info("Performing scheduled check of all streamers...").subscribe();
        checkAllStreamers().subscribe(
            null,
            error -> loggingService.error("Error during scheduled streamer check", error).subscribe(),
            () -> loggingService.info("Scheduled streamer check completed.").subscribe()
        );
    }

    @Override
    public Mono<Void> checkAllStreamers() {
        return streamerRepository.findAll()
            .flatMap(this::checkStreamer)
            .then();
    }

    private Mono<StreamStatus> checkStreamer(com.ggar.streamlink.model.dto.Streamer streamer) {
        return streamlinkService.getStreams(streamer.getUrl())
            .map(streams -> streams.isEmpty() ? StreamStatus.OFFLINE : StreamStatus.ONLINE)
            .doOnNext(newStatus -> {
                StreamStatus oldStatus = streamerStatuses.getOrDefault(streamer.getId(), StreamStatus.UNKNOWN);
                if (newStatus != oldStatus) {
                    loggingService.info("Status change for " + streamer.getName() + ": " + oldStatus + " -> " + newStatus).subscribe();
                    streamerStatuses.put(streamer.getId(), newStatus);
                    statusEventSink.tryEmitNext(new StreamStatusEvent(streamer.getId(), streamer.getName(), oldStatus, newStatus));

                    if (newStatus == StreamStatus.ONLINE && streamer.isAutoCapture()) {
                        captureOrchestratorService.getActiveCaptures()
                            .any(captureInfo -> captureInfo.url().equalsIgnoreCase(streamer.getUrl()))
                            .flatMap(isAlreadyCapturing -> {
                                if (!isAlreadyCapturing) {
                                    loggingService.info("Streamer " + streamer.getName() + " is online and auto-capture is enabled. Starting capture.").subscribe();
                                    return captureOrchestratorService.startCapture(streamer.getUrl(), streamer.getAutoCaptureQuality());
                                } else {
                                    loggingService.debug("Streamer " + streamer.getName() + " is already being captured.").subscribe();
                                    return Mono.empty();
                                }
                            })
                            .subscribe(
                                captureInfo -> loggingService.info("Auto-capture started for streamer " + streamer.getName() + ": " + captureInfo.id()).subscribe(),
                                error -> loggingService.error("Failed to start auto-capture for streamer " + streamer.getName(), error).subscribe()
                            );
                    }
                }
            })
            .onErrorResume(e -> {
                loggingService.warn("Failed to check status for streamer " + streamer.getName() + ": " + e.getMessage() + ". Assuming OFFLINE.").subscribe();
                StreamStatus oldStatus = streamerStatuses.getOrDefault(streamer.getId(), StreamStatus.UNKNOWN);
                if (oldStatus != StreamStatus.OFFLINE) {
                    streamerStatuses.put(streamer.getId(), StreamStatus.OFFLINE);
                    statusEventSink.tryEmitNext(new StreamStatusEvent(streamer.getId(), streamer.getName(), oldStatus, StreamStatus.OFFLINE));
                }
                return Mono.just(StreamStatus.OFFLINE);
            });
    }

    public Mono<StreamStatus> checkStreamer(Long streamerId) {
        return streamerRepository.findById(streamerId)
            .flatMap(this::checkStreamer)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Streamer with ID " + streamerId + " not found.")));
    }

    @Override
    public Flux<StreamerStatusDetail> getAllStreamerStatuses() {
        return streamerRepository.findAll()
                .flatMap(streamer -> {
                    StreamStatus onlineStatus = streamerStatuses.getOrDefault(streamer.getId(), StreamStatus.UNKNOWN);

                    // Check for active capture for this streamer
                    Mono<CaptureInfo> activeCaptureMono = captureOrchestratorService.getActiveCaptures()
                            .filter(captureInfo -> captureInfo.url().equalsIgnoreCase(streamer.getUrl()))
                            .next(); // Take the first one if multiple exist (shouldn't happen for a given URL)

                    return activeCaptureMono
                            .map(captureInfo -> StreamerStatusDetail.builder()
                                    .streamerId(streamer.getId())
                                    .streamerName(streamer.getName())
                                    .streamerUrl(streamer.getUrl())
                                    .platformName(streamer.getPlatform() != null ? streamer.getPlatform().getName() : "N/A")
                                    .platformUrl(streamer.getPlatform() != null ? streamer.getPlatform().getUrl() : "N/A")
                                    .onlineStatus(onlineStatus)
                                    .isCapturing(true)
                                    .captureId(captureInfo.id())
                                    .captureStatus(captureInfo.status())
                                    .build())
                            .switchIfEmpty(Mono.just(StreamerStatusDetail.builder()
                                    .streamerId(streamer.getId())
                                    .streamerName(streamer.getName())
                                    .streamerUrl(streamer.getUrl())
                                    .platformName(streamer.getPlatform() != null ? streamer.getPlatform().getName() : "N/A")
                                    .platformUrl(streamer.getPlatform() != null ? streamer.getPlatform().getUrl() : "N/A")
                                    .onlineStatus(onlineStatus)
                                    .isCapturing(false)
                                    .captureId(null)
                                    .captureStatus(null)
                                    .build()));
                });
    }

    @Override
    public Mono<StreamStatus> getStreamerStatus(Long streamerId) {
        return Mono.justOrEmpty(streamerStatuses.get(streamerId));
    }

    @Override
    public Flux<StreamStatusEvent> getStatusEvents() {
        return statusEventSink.asFlux();
    }
}