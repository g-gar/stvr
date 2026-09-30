package com.ggar.stvr.packages.streamlink.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * Details of a stream variant/quality parsed from Streamlink's JSON output.
 *
 * @param type stream protocol type (e.g. "hls", "http", "dash")
 * @param url stream segment/playlist URL
 * @param master master playlist URL (if available)
 * @param headers HTTP headers required to fetch segments
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StreamlinkStreamDetails(
        String type,
        String url,
        String master,
        Map<String, String> headers
) {}
