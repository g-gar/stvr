package com.ggar.stvr.catalog.api.inspector;

import com.ggar.stvr.catalog.entities.ChannelUrl;
import reactor.core.publisher.Mono;

/**
 * Domain port for inspecting stream URLs and extracting platform metadata and live status.
 * Implementations may use external CLI tools (e.g. Streamlink, yt-dlp), direct platform REST APIs,
 * or custom network probes.
 */
public interface ChannelInspector {

    /**
     * Determines whether this inspector supports inspecting the provided channel URL.
     *
     * @param url The channel URL.
     * @return true if supported, false otherwise.
     */
    boolean supports(ChannelUrl url);

    /**
     * Asynchronously inspects the channel URL and returns resolved metadata.
     *
     * @param url The channel URL to inspect.
     * @return Mono emitting the inspection result, or Mono.empty() if no streams/plugin resolved.
     */
    Mono<ChannelInspectionResult> inspect(ChannelUrl url);
}
