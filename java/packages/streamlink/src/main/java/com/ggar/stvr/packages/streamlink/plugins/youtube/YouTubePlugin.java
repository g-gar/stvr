package com.ggar.stvr.packages.streamlink.plugins.youtube;

import com.fasterxml.jackson.databind.JsonNode;
import com.ggar.stvr.packages.streamlink.command.AbstractStreamlinkCommandBuilder;
import com.ggar.stvr.packages.streamlink.plugins.AbstractStreamlinkPlugin;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Strategy implementation for YouTube Streamlink plugin.
 * Encapsulates URL pattern matching, command builder creation, and error handling.
 */
public class YouTubePlugin extends AbstractStreamlinkPlugin {

    private static final Pattern YOUTUBE_URL_PATTERN = Pattern.compile(
            "^https?://(?:(?:www|m)\\.)?(?:youtube\\.com|youtu\\.be)/.+",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public String getName() {
        return "youtube";
    }

    @Override
    public boolean supportsUrl(String url) {
        return url != null && YOUTUBE_URL_PATTERN.matcher(url.trim()).find();
    }

    @Override
    public AbstractStreamlinkCommandBuilder<?> createCommandBuilder(String url) {
        return new YouTubeCommandBuilder().url(url);
    }

    @Override
    public void handleError(JsonNode rootNode, String errorMsg, String targetUrl, Locale locale) {
        throw new YouTubeStreamlinkException(errorMsg, locale);
    }
}
