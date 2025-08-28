package com.ggar.streamlink.controller;

import com.ggar.streamlink.model.CaptureInfo;
import com.ggar.streamlink.model.exception.CaptureNotFoundException;
import com.ggar.streamlink.service.CaptureOrchestratorService;
import com.ggar.streamlink.service.FileStorageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/captures")
@RequiredArgsConstructor
public class StreamlinkController {

    private final CaptureOrchestratorService orchestratorService;
    private final FileStorageService fileStorageService;

    /**
     * DTO for the start capture request.
     */
    public record StartCaptureRequest(@NotEmpty String url, @NotEmpty String quality) {}

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<CaptureInfo> startCapture(@Valid @RequestBody StartCaptureRequest request) {
        log.info("Received request to start capture for url: {}", request.url());
        return orchestratorService.startCapture(request.url(), request.quality());
    }

    @GetMapping("/active")
    public Flux<CaptureInfo> getActiveCaptures() {
        log.info("Received request to list active captures");
        return orchestratorService.getActiveCaptures();
    }

    @GetMapping("/{id}")
    public Mono<CaptureInfo> getCaptureById(@PathVariable UUID id) {
        log.info("Received request to get capture by id: {}", id);
        return orchestratorService.getCapture(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> stopCapture(@PathVariable UUID id) {
        log.info("Received request to stop capture: {}", id);
        return orchestratorService.stopCapture(id);
    }

    @GetMapping("/completed")
    public Flux<String> listCompletedCaptures() {
        log.info("Received request to list completed captures");
        return fileStorageService.listCompletedFiles()
            .map(path -> path.getFileName().toString());
    }

    @GetMapping(value = "/{id}/stream", produces = "video/mp2t")
    public Mono<Resource> streamCapture(@PathVariable UUID id) {
        log.info("Request to stream capture by id: {}", id);
        return orchestratorService.getCapture(id)
            .map(captureInfo -> captureInfo.destination().getFileName().toString())
            .flatMap(fileStorageService::loadFileAsResourceWhenReady);
    }

    /**
     * Handles cases where a capture is not found.
     * The @ResponseStatus on the exception class handles this automatically,
     * but this handler provides more explicit logging.
     */
    @ExceptionHandler(CaptureNotFoundException.class)
    public ResponseEntity<String> handleCaptureNotFound(CaptureNotFoundException ex) {
        log.warn("Capture operation failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    /**
     * Handles illegal state exceptions, e.g., trying to stop a capture that is not running.
     * Returns a 409 Conflict status.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalState(IllegalStateException ex) {
        log.warn("Illegal state for capture operation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }
}