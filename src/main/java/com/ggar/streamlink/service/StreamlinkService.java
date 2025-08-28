package com.ggar.streamlink.service;

import com.ggar.streamlink.model.StreamCaptureOutput;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * A service to interact with the streamlink command-line tool.
 */
public interface StreamlinkService {

    /**
     * Checks for available streams for a given URL.
     * @return A Mono emitting a map of available qualities to their URLs. An empty map indicates the stream is offline or not found.
     */
    Mono<Map<String, String>> getStreams(String url);

    /**
     * Starts capturing a stream and returns its raw video and metadata output streams.
     */
    Mono<StreamCaptureOutput> captureStream(String url, String quality);
}