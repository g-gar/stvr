package com.ggar.stvr.inspection.implementation;

import com.ggar.stvr.catalog.entities.Platform;
import com.ggar.stvr.inspection.api.InspectStreamQueryHandler;
import com.ggar.stvr.inspection.entities.StreamInfo;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Reactive CQRS implementation for stream inspection returning StreamInfo directly.
 */
@Service
public class InspectStreamQueryHandlerImpl implements InspectStreamQueryHandler {

    private final InspectionPipeline pipeline;

    public InspectStreamQueryHandlerImpl(InspectionPipeline pipeline) {
        this.pipeline = Objects.requireNonNull(pipeline, "InspectionPipeline cannot be null");
    }

    @Override
    public Mono<StreamInfo> handle(InspectStreamQuery query) {
        Objects.requireNonNull(query, "InspectStreamQuery cannot be null");

        StreamInfo initial = StreamInfo.builder()
                .channelUrl(query.url())
                .platform(query.platform() != null ? query.platform() : Platform.of("custom"))
                .live(false)
                .build();

        return pipeline.execute(initial);
    }
}
