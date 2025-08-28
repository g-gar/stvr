package com.ggar.streamlink.controller;

import com.ggar.streamlink.model.dto.NewPlatformInput;
import com.ggar.streamlink.model.dto.NewStreamerInput;
import com.ggar.streamlink.model.dto.PlatformDTO;
import com.ggar.streamlink.model.dto.Streamer;
import com.ggar.streamlink.service.StreamerDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class StreamerController {

    private final StreamerDataService streamerDataService;

    @GetMapping("/platforms")
    public Flux<PlatformDTO> getPlatforms() {
        return streamerDataService.getPlatforms();
    }

    @PostMapping("/platforms")
    public Mono<PlatformDTO> addPlatform(@RequestBody NewPlatformInput input) {
        return streamerDataService.addPlatform(input);
    }

    @GetMapping("/platforms/{id}")
    public Mono<PlatformDTO> getPlatformById(@PathVariable String id) {
        return streamerDataService.getPlatformById(id);
    }

    @GetMapping("/platforms/{platformId}/streamers")
    public Flux<Streamer> getStreamersByPlatform(@PathVariable String platformId) {
        return streamerDataService.getStreamersByPlatform(platformId);
    }

    @PostMapping("/streamers")
    public Mono<Streamer> addStreamer(@RequestBody NewStreamerInput input) {
        return streamerDataService.addStreamer(input);
    }

    @GetMapping("/streamers/{id}")
    public Mono<Streamer> getStreamerById(@PathVariable String id) {
        return streamerDataService.getStreamerById(id);
    }

    public record AutoCaptureRequest(boolean enabled, String quality) {}

    @PatchMapping("/streamers/{id}/auto-capture")
    public Mono<Streamer> configureAutoCapture(@PathVariable String id, @RequestBody AutoCaptureRequest request) {
        return streamerDataService.configureAutoCapture(id, request.enabled(), request.quality());
    }
}