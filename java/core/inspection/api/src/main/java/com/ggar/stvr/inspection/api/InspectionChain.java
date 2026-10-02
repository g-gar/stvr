package com.ggar.stvr.inspection.api;

import com.ggar.stvr.inspection.entities.StreamInfo;
import reactor.core.publisher.Mono;

/**
 * Functional callback interface representing the next step in the stream inspection chain.
 */
@FunctionalInterface
public interface InspectionChain {

    /**
     * Proceeds with the next plugin in the chain.
     *
     * @param streamInfo the current stream info state
     * @return Mono emitting the resulting StreamInfo after downstream processing
     */
    Mono<StreamInfo> proceed(StreamInfo streamInfo);
}
