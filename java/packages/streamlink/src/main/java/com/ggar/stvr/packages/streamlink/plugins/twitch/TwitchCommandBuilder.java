package com.ggar.stvr.packages.streamlink.plugins.twitch;

import com.ggar.stvr.packages.streamlink.command.AbstractStreamlinkCommandBuilder;

/**
 * Expressive builder for Twitch-specific Streamlink CLI options.
 */
public class TwitchCommandBuilder extends AbstractStreamlinkCommandBuilder<TwitchCommandBuilder> {

    public TwitchCommandBuilder disableAds() {
        return disableAds(true);
    }

    public TwitchCommandBuilder disableAds(boolean disable) {
        if (disable) {
            customFlag("--twitch-disable-ads");
        }
        return self();
    }

    public TwitchCommandBuilder lowLatency() {
        return lowLatency(true);
    }

    public TwitchCommandBuilder lowLatency(boolean lowLatency) {
        if (lowLatency) {
            customFlag("--twitch-low-latency");
        }
        return self();
    }

    public TwitchCommandBuilder disableHosting() {
        return disableHosting(true);
    }

    public TwitchCommandBuilder disableHosting(boolean disable) {
        if (disable) {
            customFlag("--twitch-disable-hosting");
        }
        return self();
    }

    public TwitchCommandBuilder disableReruns() {
        return disableReruns(true);
    }

    public TwitchCommandBuilder disableReruns(boolean disable) {
        if (disable) {
            customFlag("--twitch-disable-reruns");
        }
        return self();
    }

    public TwitchCommandBuilder supportedCodecs(String... codecs) {
        if (codecs != null && codecs.length > 0) {
            customOption("--twitch-supported-codecs", String.join(",", codecs));
        }
        return self();
    }

    public TwitchCommandBuilder apiHeader(String key, String value) {
        return customOption("--twitch-api-header", key + "=" + value);
    }

    public TwitchCommandBuilder accessTokenParam(String key, String value) {
        return customOption("--twitch-access-token-param", key + "=" + value);
    }

    public TwitchCommandBuilder forceClientIntegrity() {
        return forceClientIntegrity(true);
    }

    public TwitchCommandBuilder forceClientIntegrity(boolean force) {
        if (force) {
            customFlag("--twitch-force-client-integrity");
        }
        return self();
    }

    public TwitchCommandBuilder purgeClientIntegrity() {
        return purgeClientIntegrity(true);
    }

    public TwitchCommandBuilder purgeClientIntegrity(boolean purge) {
        if (purge) {
            customFlag("--twitch-purge-client-integrity");
        }
        return self();
    }
}
