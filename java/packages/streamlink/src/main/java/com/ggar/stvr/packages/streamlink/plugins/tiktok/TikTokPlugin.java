package com.ggar.stvr.packages.streamlink.plugins.tiktok;

import com.fasterxml.jackson.databind.JsonNode;
import com.ggar.stvr.packages.streamlink.command.AbstractStreamlinkCommandBuilder;
import com.ggar.stvr.packages.streamlink.plugins.AbstractStreamlinkPlugin;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Strategy implementation for TikTok Streamlink plugin.
 * Encapsulates URL pattern matching, command builder creation, and error handling.
 */
public class TikTokPlugin extends AbstractStreamlinkPlugin {

    private static final Pattern TIKTOK_URL_PATTERN = Pattern.compile(
            "^https?://(?:www\\.)?tiktok\\.com/.+",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public String getName() {
        return "tiktok";
    }

    @Override
    public boolean supportsUrl(String url) {
        return url != null && TIKTOK_URL_PATTERN.matcher(url.trim()).find();
    }

    @Override
    public AbstractStreamlinkCommandBuilder<?> createCommandBuilder(String url) {
        return new TikTokCommandBuilder().url(url);
    }

    @Override
    public void handleError(JsonNode rootNode, String errorMsg, String targetUrl, Locale locale) {
        throw new TikTokStreamlinkException(errorMsg, locale);
    }
}
