package com.ggar.stvr.packages.streamlink.command;

import com.ggar.stvr.packages.streamlink.i18n.StreamlinkMessages;
import com.ggar.stvr.packages.streamlink.plugins.StreamlinkPluginRegistry;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Immutable specification of a Streamlink command ready for execution.
 */
@Getter
@Builder(toBuilder = true)
public class StreamlinkCommand {

    @Builder.Default
    private final String binary = "streamlink";

    private final String url;
    private final String quality;
    private final boolean json;
    private final boolean stdout;
    private final String outputPath;

    private final Integer retryStreams;
    private final Integer retryOpen;
    private final Integer streamSegmentThreads;
    private final String httpProxy;
    private final String httpsProxy;

    @Singular
    private final Map<String, String> httpHeaders;

    @Singular
    private final List<String> customFlags;

    @Singular
    private final Map<String, List<String>> customOptions;

    /**
     * Automatically resolves the streaming platform from the URL by delegating to registered
     * plugin strategies and returns the platform-specific builder.
     *
     * @param url target stream URL
     * @param <B> builder type (e.g. TwitchCommandBuilder, KickCommandBuilder, etc.)
     * @return platform-specific command builder initialized with the URL
     */
    @SuppressWarnings("unchecked")
    public static <B extends AbstractStreamlinkCommandBuilder<B>> B fromUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException(StreamlinkMessages.get("error.url_required"));
        }
        return (B) StreamlinkPluginRegistry.defaultRegistry().findForUrl(url).createCommandBuilder(url);
    }

    /**
     * Resolves the streaming platform from the URL and verifies that it matches the expected builder class.
     *
     * @param url target stream URL
     * @param expectedBuilderClass expected builder class (e.g. TwitchCommandBuilder.class)
     * @param <T> builder type
     * @return typed command builder
     * @throws IllegalArgumentException if the URL resolves to a different platform
     */
    public static <T extends AbstractStreamlinkCommandBuilder<T>> T fromUrl(String url, Class<T> expectedBuilderClass) {
        AbstractStreamlinkCommandBuilder<?> builder = fromUrl(url);
        if (!expectedBuilderClass.isInstance(builder)) {
            throw new IllegalArgumentException(StreamlinkMessages.get("error.url_builder_mismatch",
                    url, builder.getClass().getSimpleName(), expectedBuilderClass.getSimpleName()));
        }
        return expectedBuilderClass.cast(builder);
    }

    /**
     * Creates an immutable StreamlinkCommand by automatically resolving the platform from the URL.
     *
     * @param url target stream URL
     * @return StreamlinkCommand ready for inspection or streaming
     */
    public static StreamlinkCommand of(String url) {
        return fromUrl(url).build();
    }

    /**
     * Creates an immutable StreamlinkCommand with a specific quality by automatically resolving the platform from the URL.
     *
     * @param url target stream URL
     * @param quality stream quality (e.g. "best", "1080p60", "audio_only")
     * @return StreamlinkCommand ready for execution
     */
    public static StreamlinkCommand of(String url, String quality) {
        return fromUrl(url).quality(quality).build();
    }

    /**
     * Converts this command specification into an ordered list of CLI arguments.
     *
     * @return List of command arguments starting with the binary name.
     */
    public List<String> toArgs() {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException(StreamlinkMessages.get("error.url_required"));
        }

        List<String> args = new ArrayList<>();
        args.add(binary);

        // Flags
        if (json) {
            args.add("--json");
        }
        if (stdout) {
            args.add("-O");
        }
        if (outputPath != null && !outputPath.isBlank()) {
            args.add("-o");
            args.add(outputPath);
        }

        // Connection / Retry options
        if (retryStreams != null && retryStreams > 0) {
            args.add("--retry-streams");
            args.add(String.valueOf(retryStreams));
        }
        if (retryOpen != null && retryOpen > 0) {
            args.add("--retry-open");
            args.add(String.valueOf(retryOpen));
        }
        if (streamSegmentThreads != null && streamSegmentThreads > 0) {
            args.add("--stream-segment-threads");
            args.add(String.valueOf(streamSegmentThreads));
        }
        if (httpProxy != null && !httpProxy.isBlank()) {
            args.add("--http-proxy");
            args.add(httpProxy);
        }
        if (httpsProxy != null && !httpsProxy.isBlank()) {
            args.add("--https-proxy");
            args.add(httpsProxy);
        }

        // HTTP Headers
        if (httpHeaders != null) {
            for (Map.Entry<String, String> header : httpHeaders.entrySet()) {
                args.add("--http-header");
                args.add(header.getKey() + "=" + header.getValue());
            }
        }

        // Custom plugin/general flags
        if (customFlags != null) {
            args.addAll(customFlags);
        }

        // Custom plugin/general options (repeated or single)
        if (customOptions != null) {
            for (Map.Entry<String, List<String>> option : customOptions.entrySet()) {
                for (String val : option.getValue()) {
                    args.add(option.getKey());
                    args.add(val);
                }
            }
        }

        // Positional argument: URL
        args.add(url);

        // Positional argument: quality (if specified)
        if (quality != null && !quality.isBlank()) {
            args.add(quality);
        }

        return Collections.unmodifiableList(args);
    }
}
