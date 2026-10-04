package com.ggar.stvr.plugins.streamlink;

import com.ggar.stvr.catalog.entities.ChannelName;
import com.ggar.stvr.catalog.entities.ChannelUrl;
import com.ggar.stvr.catalog.entities.Platform;
import com.ggar.stvr.inspection.api.resolver.ChannelResolution;
import com.ggar.stvr.inspection.api.resolver.ChannelResolver;
import com.ggar.stvr.packages.streamlink.StreamlinkClient;
import com.ggar.stvr.packages.streamlink.exception.StreamlinkNoStreamsException;
import com.ggar.stvr.packages.streamlink.exception.StreamlinkPluginNotFoundException;
import com.ggar.stvr.packages.streamlink.model.StreamlinkStreamInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Objects;

/**
 * ChannelResolver implementation that delegates platform and channel identity resolution
 * directly to the containerized Streamlink binary via {@link StreamlinkClient}.
 * Eliminates custom manual regex parsing by querying Streamlink's native extractor plugins.
 */
public class StreamlinkChannelResolver implements ChannelResolver {

    private static final Logger log = LoggerFactory.getLogger(StreamlinkChannelResolver.class);

    private final StreamlinkClient client;

    public StreamlinkChannelResolver(StreamlinkClient client) {
        this.client = Objects.requireNonNull(client, "StreamlinkClient cannot be null");
    }

    @Override
    public boolean supports(ChannelUrl url) {
        return url != null && url.value() != null;
    }

    @Override
    public Mono<ChannelResolution> resolve(ChannelUrl url) {
        Objects.requireNonNull(url, "url cannot be null");
        String urlString = url.asString();

        log.debug("Resolving channel identity for URL '{}' via Streamlink", urlString);

        return client.inspect(urlString)
                .map(streamInfo -> buildResolutionFromStreamInfo(url, streamInfo))
                .onErrorResume(StreamlinkNoStreamsException.class, ex -> {
                    // Channel is offline, but Streamlink matched the platform
                    log.debug("Channel is offline for URL '{}', resolving platform from Streamlink plugin: {}",
                            urlString, ex.getPluginName());
                    String pluginName = ex.getPluginName();
                    if (pluginName != null && !pluginName.isBlank()) {
                        Platform platform = Platform.of(pluginName);
                        String slug = extractSlugFromUrl(url.value());
                        return Mono.just(new ChannelResolution(platform, slug, ChannelName.of(slug)));
                    }
                    return Mono.empty();
                })
                .onErrorResume(StreamlinkPluginNotFoundException.class, ex -> {
                    // No plugin in Streamlink can handle this URL -> not supported
                    log.debug("Streamlink cannot handle URL '{}': {}", urlString, ex.getMessage());
                    return Mono.empty();
                })
                .onErrorResume(ex -> {
                    log.warn("Unexpected error resolving channel URL '{}' via Streamlink: {}", urlString, ex.getMessage());
                    return Mono.empty();
                });
    }

    private ChannelResolution buildResolutionFromStreamInfo(ChannelUrl url, StreamlinkStreamInfo streamInfo) {
        Platform platform = Platform.of(streamInfo.plugin());
        String author = streamInfo.metadata() != null && streamInfo.metadata().author() != null
                && !streamInfo.metadata().author().isBlank()
                ? streamInfo.metadata().author()
                : extractSlugFromUrl(url.value());

        String slug = author.trim();
        ChannelName name = ChannelName.of(author);
        return new ChannelResolution(platform, slug, name);
    }

    private String extractSlugFromUrl(URI uri) {
        if (uri == null || uri.getPath() == null || uri.getPath().isBlank()) {
            return "unknown";
        }
        String path = uri.getPath().replaceAll("^/+|/+$", "");
        if (path.contains("/")) {
            String[] parts = path.split("/");
            for (String part : parts) {
                if (!part.equalsIgnoreCase("live") && !part.equalsIgnoreCase("c") && !part.equalsIgnoreCase("channel")) {
                    return part.replaceFirst("^@", "");
                }
            }
        }
        return path.replaceFirst("^@", "");
    }
}
