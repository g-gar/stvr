package com.ggar.stvr.plugins.streamlink;

import com.ggar.stvr.inspection.api.InspectionChain;
import com.ggar.stvr.inspection.api.InspectionPlugin;
import com.ggar.stvr.inspection.entities.StreamInfo;
import com.ggar.stvr.packages.streamlink.StreamlinkClient;
import com.ggar.stvr.packages.streamlink.exception.StreamlinkNoStreamsException;
import com.ggar.stvr.packages.streamlink.model.StreamlinkStreamInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * InspectionPlugin implementation using Streamlink CLI to probe live stream telemetry and qualities.
 */
public class StreamlinkInspectionPlugin implements InspectionPlugin {

    private static final Logger log = LoggerFactory.getLogger(StreamlinkInspectionPlugin.class);
    private static final String PLUGIN_ID = "streamlink";

    private final StreamlinkClient client;
    private final int order;

    public StreamlinkInspectionPlugin(StreamlinkClient client) {
        this(client, 0);
    }

    public StreamlinkInspectionPlugin(StreamlinkClient client, int order) {
        this.client = Objects.requireNonNull(client, "StreamlinkClient cannot be null");
        this.order = order;
    }

    @Override
    public String getId() {
        return PLUGIN_ID;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public Mono<StreamInfo> inspect(StreamInfo streamInfo, InspectionChain chain) {
        Objects.requireNonNull(streamInfo, "streamInfo cannot be null");
        Objects.requireNonNull(chain, "chain cannot be null");

        String urlString = streamInfo.getChannelUrl().asString();
        log.debug("Probing stream URL '{}' with Streamlink", urlString);

        return client.inspect(urlString)
                .flatMap(inspectionResult -> {
                    StreamInfo enriched = enrichWithStreamlink(streamInfo, inspectionResult);
                    return chain.proceed(enriched);
                })
                .onErrorResume(StreamlinkNoStreamsException.class, ex -> {
                    log.debug("Streamlink detected channel is offline for URL: {}", urlString);
                    StreamInfo offlineInfo = streamInfo.toBuilder()
                            .live(false)
                            .build();
                    return chain.proceed(offlineInfo);
                })
                .onErrorResume(ex -> {
                    log.warn("Streamlink inspection failed for URL '{}': {}", urlString, ex.getMessage());
                    // On unhandled inspection error, proceed with current state so downstream plugins can run
                    return chain.proceed(streamInfo.toBuilder().live(false).build());
                });
    }

    private StreamInfo enrichWithStreamlink(StreamInfo base, StreamlinkStreamInfo result) {
        String title = (base.getTitle() != null && !base.getTitle().isBlank())
                ? base.getTitle()
                : (result.metadata() != null && result.metadata().title() != null ? result.metadata().title() : base.getTitle());

        String category = (base.getCategory() != null && !base.getCategory().isBlank())
                ? base.getCategory()
                : (result.metadata() != null && result.metadata().category() != null ? result.metadata().category() : base.getCategory());

        List<String> qualities = result.streams() != null
                ? new ArrayList<>(result.streams().keySet())
                : List.of();

        return base.toBuilder()
                .live(result.hasStreams())
                .title(title)
                .category(category)
                .availableQualities(qualities)
                .build();
    }
}
