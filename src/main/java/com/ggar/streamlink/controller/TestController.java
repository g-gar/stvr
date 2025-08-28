package com.ggar.streamlink.controller;

import com.ggar.streamlink.service.CaptureOrchestratorService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ggar.streamlink.service.StreamlinkService;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/test")
@AllArgsConstructor
public class TestController {

    private final StreamlinkService streamlinkService;
    private final CaptureOrchestratorService captureOrchestratorService;

    @GetMapping(value = "/capture-debug", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> captureStreamDebug(@RequestParam String url, @RequestParam String quality) {
        return streamlinkService.captureStream(url, quality)
            .flatMapMany(output -> {
                Flux<String> videoFlux = output.videoStream()
                    .map(dataBuffer -> "VIDEO DATA CHUNK: " + dataBuffer.readableByteCount() + " bytes\n");

                Flux<String> metadataFlux = output.metadataStream()
                    .map(metadata -> "METADATA: " + metadata.toString() + "\n");

                return Flux.merge(videoFlux, metadataFlux);
            });
    }

    @PostMapping(value = "/start-capture")
    public ResponseEntity<Void> startCapture(@RequestParam String url, @RequestParam String quality) {
        captureOrchestratorService.startCapture(url, quality);
        return ResponseEntity.accepted().build();
    }
}
