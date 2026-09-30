package com.ggar.stvr.packages.streamlink.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * Top-level inspection model representing the result of {@code streamlink --json <url>}.
 *
 * @param plugin name of the resolved Streamlink plugin (e.g. "twitch", "kick", "youtube")
 * @param metadata stream metadata including author, title, and category
 * @param streams available stream qualities mapped to their technical stream details
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StreamlinkInspection(
        String plugin,
        StreamlinkMetadata metadata,
        Map<String, StreamlinkStreamDetails> streams
) {
    public boolean hasStreams() {
        return streams != null && !streams.isEmpty();
    }
}
