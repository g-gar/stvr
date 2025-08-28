package com.ggar.streamlink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import java.util.Map;

/**
 * Represents the details of a specific stream quality.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@With
public class StreamDetails {

    /**
     * The quality of the stream (e.g., "720p", "1080p60").
     * This value is derived from the key in the 'streams' map of the streamlink output.
     */
    private String quality;

    /**
     * The type of the stream (e.g., hls, dash).
     */
    private String type;

    /**
     * The direct URL to the media stream.
     */
    private String url;

    /**
     * HTTP headers required to access the stream URL.
     */
    private Map<String, String> headers;
}
