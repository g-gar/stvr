package com.ggar.stvr.packages.streamlink.plugins.kick;

import com.fasterxml.jackson.databind.JsonNode;
import com.ggar.stvr.packages.streamlink.command.AbstractStreamlinkCommandBuilder;
import com.ggar.stvr.packages.streamlink.plugins.AbstractStreamlinkPlugin;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Strategy implementation for Kick Streamlink plugin.
 * Encapsulates URL pattern matching, command builder creation, and error handling.
 */
public class KickPlugin extends AbstractStreamlinkPlugin {

    private static final Pattern KICK_URL_PATTERN = Pattern.compile(
            "^https?://(?:\\w+\\.)?kick\\.com/.+",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public String getName() {
        return "kick";
    }

    @Override
    public boolean supportsUrl(String url) {
        return url != null && KICK_URL_PATTERN.matcher(url.trim()).find();
    }

    @Override
    public AbstractStreamlinkCommandBuilder<?> createCommandBuilder(String url) {
        return new KickCommandBuilder().url(url);
    }

    @Override
    public void handleError(JsonNode rootNode, String errorMsg, String targetUrl, Locale locale) {
        throw new KickStreamlinkException(errorMsg, locale);
    }
}
