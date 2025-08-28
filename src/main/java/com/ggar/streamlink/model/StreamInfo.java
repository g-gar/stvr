package com.ggar.streamlink.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Map;

/**
 * Represents the full output of a `streamlink --json` command.
 * It contains metadata and a list of available streams.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StreamInfo {
    /**
     * The name of the streamlink plugin that handled the URL.
     */
    private String plugin;

    /**
     * Metadata about the stream (e.g., author, category, title).
     */
    private Map<String, String> metadata;

    /**
     * A map of available streams, where the key is the quality name
     * (e.g., "720p", "1080p60") and the value contains the stream details.
     * The set of keys of this map represents the available qualities.
     */
    private Map<String, StreamDetails> streams;
}
