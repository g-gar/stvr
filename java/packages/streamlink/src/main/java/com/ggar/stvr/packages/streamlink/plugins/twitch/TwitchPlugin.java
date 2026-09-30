package com.ggar.stvr.packages.streamlink.plugins.twitch;

import com.fasterxml.jackson.databind.JsonNode;
import com.ggar.stvr.packages.streamlink.command.AbstractStreamlinkCommandBuilder;
import com.ggar.stvr.packages.streamlink.plugins.AbstractStreamlinkPlugin;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Strategy implementation for Twitch Streamlink plugin.
 * Encapsulates URL pattern matching, command builder creation, and error handling.
 */
public class TwitchPlugin extends AbstractStreamlinkPlugin {

    private static final Pattern TWITCH_URL_PATTERN = Pattern.compile(
            "^https?://(?:(?:www|go|m)\\.)?twitch\\.tv/.+|^https?://clips\\.twitch\\.tv/.+",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public String getName() {
        return "twitch";
    }

    @Override
    public boolean supportsUrl(String url) {
        return url != null && TWITCH_URL_PATTERN.matcher(url.trim()).find();
    }

    @Override
    public AbstractStreamlinkCommandBuilder<?> createCommandBuilder(String url) {
        return new TwitchCommandBuilder().url(url);
    }

    @Override
    public void handleError(JsonNode rootNode, String errorMsg, String targetUrl, Locale locale) {
        throw new TwitchStreamlinkException(errorMsg, locale);
    }
}
