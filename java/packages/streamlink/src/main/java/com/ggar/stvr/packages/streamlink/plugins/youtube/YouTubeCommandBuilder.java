package com.ggar.stvr.packages.streamlink.plugins.youtube;

import com.ggar.stvr.packages.streamlink.command.AbstractStreamlinkCommandBuilder;

/**
 * Expressive builder for YouTube-specific Streamlink CLI options.
 */
public class YouTubeCommandBuilder extends AbstractStreamlinkCommandBuilder<YouTubeCommandBuilder> {

    public YouTubeCommandBuilder includeDashManifests() {
        return includeDashManifests(true);
    }

    public YouTubeCommandBuilder includeDashManifests(boolean include) {
        if (include) {
            customFlag("--youtube-include-dash-manifests");
        }
        return self();
    }
}
