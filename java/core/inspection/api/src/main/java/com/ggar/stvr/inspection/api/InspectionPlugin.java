package com.ggar.stvr.inspection.api;

import com.ggar.stvr.inspection.entities.StreamInfo;
import reactor.core.publisher.Mono;

/**
 * Service Provider Interface (SPI) for channel stream inspection plugins.
 * Plugins inspect live streaming channels (via Streamlink, external APIs, etc.)
 * and enrich the StreamInfo model.
 */
public interface InspectionPlugin {

    /**
     * Unique identifier for this plugin (e.g., "streamlink", "twitch-helix", "yt-dlp").
     */
    String getId();

    /**
     * Checks whether this plugin can handle the given stream info.
     *
     * @param streamInfo current stream info
     * @return true if this plugin should execute for this stream info
     */
    default boolean supports(StreamInfo streamInfo) {
        return true;
    }

    /**
     * Default execution priority when not explicitly specified in configuration.
     * Lower values execute earlier in the chain.
     */
    default int getOrder() {
        return 0;
    }

    /**
     * Executes the inspection logic and either proceeds along the chain or completes the flow.
     *
     * @param streamInfo the current stream info state
     * @param chain the pipeline chain to call when ready to proceed
     * @return Mono of the enriched StreamInfo
     */
    Mono<StreamInfo> inspect(StreamInfo streamInfo, InspectionChain chain);
}
