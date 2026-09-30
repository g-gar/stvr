package com.ggar.stvr.packages.streamlink.plugins.tiktok;

import com.ggar.stvr.packages.streamlink.command.AbstractStreamlinkCommandBuilder;

/**
 * Expressive builder for TikTok-specific Streamlink CLI options.
 */
public class TikTokCommandBuilder extends AbstractStreamlinkCommandBuilder<TikTokCommandBuilder> {

    public TikTokCommandBuilder appVersion(String version) {
        return customOption("--tiktok-app-version", version);
    }
}
