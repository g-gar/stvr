package com.ggar.streamlink.service.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ggar.streamlink.model.StreamDetails;
import com.ggar.streamlink.model.StreamInfo;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Collectors;

@Component
public class StreamInfoParserImpl implements StreamInfoParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public StreamInfoParserImpl() {
        // Configure ObjectMapper to ignore unknown properties for robustness.
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public Mono<StreamInfo> parse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            StreamInfo streamInfo = objectMapper.treeToValue(root, StreamInfo.class);

            // The stream quality is the key of the map, so we create new StreamDetails objects
            // with the quality field populated using a wither method.
            if (streamInfo != null && streamInfo.getStreams() != null) {
                Map<String, StreamDetails> updatedStreams = streamInfo.getStreams().entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> entry.getValue().withQuality(entry.getKey())
                        ));
                streamInfo.setStreams(updatedStreams);
            }

            return Mono.justOrEmpty(streamInfo);
        } catch (JsonProcessingException e) {
            return Mono.error(new RuntimeException("Failed to parse streamlink JSON output", e));
        }
    }
}
