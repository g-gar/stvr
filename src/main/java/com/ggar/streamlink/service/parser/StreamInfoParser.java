package com.ggar.streamlink.service.parser;

import com.ggar.streamlink.model.StreamInfo;
import reactor.core.publisher.Mono;

/**
 * Interface for a parser that converts a JSON string into a StreamInfo object.
 */
public interface StreamInfoParser {

    /**
     * Parses a JSON string from streamlink into a StreamInfo object.
     *
     * @param json The JSON string to parse.
     * @return A {@link Mono} that emits the parsed {@link StreamInfo} object.
     *         The mono will emit an error if parsing fails.
     */
    Mono<StreamInfo> parse(String json);
}
