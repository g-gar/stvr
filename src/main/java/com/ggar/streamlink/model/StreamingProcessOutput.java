package com.ggar.streamlink.model;

import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;

import java.io.InputStream;

public record StreamingProcessOutput(
    Flux<DataBuffer> stdout,
    InputStream stderr,
    Process process
) {}
