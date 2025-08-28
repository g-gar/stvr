package com.ggar.streamlink.model;

import org.springframework.core.io.buffer.DataBuffer;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
/**
 * A record to hold the two distinct output streams from a streamlink process.
 * @param videoStream The raw video data from stdout.
 * @param metadataStream The parsed metadata/log lines from stderr.
 * @param exitCode A Mono that completes with the exit code of the process.
 */
public record StreamCaptureOutput(
    Flux<DataBuffer> videoStream,
    Flux<StreamOutput> metadataStream,
    Mono<Integer> exitCode
) {}
