package com.ggar.stvr.packages.streamlink.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Metadata object returned by Streamlink CLI under the 'metadata' key in JSON mode.
 *
 * @param id channel or stream ID
 * @param author streamer or channel name
 * @param category game or category name
 * @param title stream title
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StreamlinkMetadata(
        String id,
        String author,
        String category,
        String title
) {}
