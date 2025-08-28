package com.ggar.streamlink.service;

import com.ggar.streamlink.model.dto.NewPlatformInput;
import com.ggar.streamlink.model.dto.NewStreamerInput;
import com.ggar.streamlink.model.dto.PlatformDTO;
import com.ggar.streamlink.model.dto.Streamer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface StreamerDataService {
    Flux<PlatformDTO> getPlatforms();
    Flux<Streamer> getStreamersByPlatform(String platformId);
    Mono<Streamer> addStreamer(NewStreamerInput input);
    Mono<PlatformDTO> addPlatform(NewPlatformInput input);
    Mono<Streamer> getStreamerById(String id);
    Mono<PlatformDTO> getPlatformById(String id);
    Mono<Streamer> configureAutoCapture(String id, boolean enabled, String quality);
}
