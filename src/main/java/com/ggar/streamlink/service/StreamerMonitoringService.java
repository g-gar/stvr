package com.ggar.streamlink.service;

import com.ggar.streamlink.model.dto.StreamerStatusDetail;
import com.ggar.streamlink.model.dto.StreamStatusEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface StreamerMonitoringService {
    void performScheduledCheck();
    Mono<Void> checkAllStreamers();
    Flux<StreamerStatusDetail> getAllStreamerStatuses();
    Mono<StreamStatus> getStreamerStatus(Long streamerId);
    Flux<StreamStatusEvent> getStatusEvents();
}