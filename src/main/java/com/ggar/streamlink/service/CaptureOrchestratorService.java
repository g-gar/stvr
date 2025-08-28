package com.ggar.streamlink.service;

import com.ggar.streamlink.model.CaptureInfo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Orchestrates and manages the lifecycle of stream captures.
 */
public interface CaptureOrchestratorService {

    /**
     * Starts the full stream capture and persistence pipeline.
     *
     * @param url The public URL of the video stream.
     * @param quality The desired stream quality.
     * @return A Mono emitting information about the capture task that has been started.
     */
    Mono<CaptureInfo> startCapture(String url, String quality);

    /**
     * Retrieves a list of all currently active captures.
     *
     * @return A Flux emitting information for each active capture.
     */
    Flux<CaptureInfo> getActiveCaptures();

    /**
     * Retrieves information about a specific capture, whether active or not.
     *
     * @param id The UUID of the capture to look for.
     * @return A Mono emitting the capture information. It will signal an error
     *         if no capture with the given ID is found.
     */
    Mono<CaptureInfo> getCapture(UUID id);

    /**
     * Stops a specific, active capture.
     *
     * @param id The UUID of the capture to stop.
     * @return A Mono<Void> that completes when the capture has been successfully stopped.
     *         It will signal an error if no capture with the given ID is found.
     */
    Mono<Void> stopCapture(UUID id);
}
