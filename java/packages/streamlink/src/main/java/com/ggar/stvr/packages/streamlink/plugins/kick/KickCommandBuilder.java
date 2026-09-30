package com.ggar.stvr.packages.streamlink.plugins.kick;

import com.ggar.stvr.packages.streamlink.command.AbstractStreamlinkCommandBuilder;

/**
 * Expressive builder for Kick-specific Streamlink CLI options.
 */
public class KickCommandBuilder extends AbstractStreamlinkCommandBuilder<KickCommandBuilder> {

    public KickCommandBuilder lowLatency() {
        return lowLatency(true);
    }

    public KickCommandBuilder lowLatency(boolean lowLatency) {
        if (lowLatency) {
            customFlag("--kick-low-latency");
        }
        return self();
    }
}
