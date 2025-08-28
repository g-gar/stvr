package com.ggar.streamlink.service.parser;

import com.ggar.streamlink.model.StreamOutput;
import reactor.core.publisher.Flux;

import java.io.InputStream;

public interface StreamDataParser {
    Flux<StreamOutput> parse(InputStream inputStream);
}
