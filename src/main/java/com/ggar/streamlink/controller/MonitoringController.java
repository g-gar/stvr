package com.ggar.streamlink.controller;

import com.ggar.streamlink.model.dto.StreamStatusEvent;
import com.ggar.streamlink.model.dto.StreamerStatusDetail;
import com.ggar.streamlink.service.StreamerMonitoringService;
import com.ggar.streamlink.service.StreamStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/monitoring")
@RequiredArgsConstructor
public class MonitoringController {

    private final StreamerMonitoringService monitoringService;

    @GetMapping("/status")
    public Flux<StreamerStatusDetail> getAllStatuses() {
        return monitoringService.getAllStreamerStatuses();
    }

    @GetMapping(path = "/status/updates", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<StreamStatusEvent>> getStatusUpdates() {
        // The actual events from the monitoring service
        Flux<ServerSentEvent<StreamStatusEvent>> events = monitoringService.getStatusEvents()
            .map(event -> ServerSentEvent.<StreamStatusEvent>builder()
                .id(String.valueOf(System.currentTimeMillis())) // Use a timestamp or event-specific ID
                .event("status-update")
                .data(event)
                .build());

        // A heartbeat to keep the connection alive if there are no events
        Flux<ServerSentEvent<StreamStatusEvent>> heartbeat = Flux.interval(Duration.ofSeconds(15))
            .map(i -> ServerSentEvent.<StreamStatusEvent>builder()
                .comment("keep-alive")
                .build());

        return Flux.merge(events, heartbeat);
    }
}