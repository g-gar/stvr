package com.ggar.stvr.plugins.streamlink;

import com.ggar.stvr.catalog.entities.ChannelUrl;
import com.ggar.stvr.packages.streamlink.StreamlinkClient;
import com.ggar.stvr.packages.streamlink.session.StreamlinkSession;
import com.ggar.stvr.streaming.api.LiveStreamConnection;
import com.ggar.stvr.streaming.api.LiveStreamProvider;
import com.ggar.stvr.streaming.entities.StreamQuality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Implementation of LiveStreamProvider utilizing Streamlink CLI or sidecar to capture video streams.
 */
public class StreamlinkLiveStreamProvider implements LiveStreamProvider {

    private static final Logger log = LoggerFactory.getLogger(StreamlinkLiveStreamProvider.class);

    private final StreamlinkClient client;

    public StreamlinkLiveStreamProvider(StreamlinkClient client) {
        this.client = Objects.requireNonNull(client, "StreamlinkClient cannot be null");
    }

    @Override
    public boolean supports(ChannelUrl url) {
        return url != null && url.asString() != null && !url.asString().isBlank();
    }

    @Override
    public Mono<LiveStreamConnection> openStream(ChannelUrl url, StreamQuality quality) {
        Objects.requireNonNull(url, "ChannelUrl cannot be null");
        String targetQuality = quality != null ? quality.value() : "best";
        String urlString = url.asString();

        log.debug("Opening live stream with Streamlink for URL '{}' at quality '{}'", urlString, targetQuality);

        return Mono.fromCallable(() -> {
            StreamlinkSession session = client.openSession(urlString, targetQuality);
            return new StreamlinkLiveStreamConnection(session);
        });
    }
}
