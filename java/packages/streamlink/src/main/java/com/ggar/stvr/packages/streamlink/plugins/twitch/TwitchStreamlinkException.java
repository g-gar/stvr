package com.ggar.stvr.packages.streamlink.plugins.twitch;

import com.ggar.stvr.packages.streamlink.exception.StreamlinkPluginException;

import java.util.Locale;

public class TwitchStreamlinkException extends StreamlinkPluginException {

    public TwitchStreamlinkException(String message) {
        super("twitch", message);
    }

    public TwitchStreamlinkException(String message, Locale locale) {
        super("twitch", message, locale);
    }

    public TwitchStreamlinkException(String message, Throwable cause) {
        super("twitch", message, cause);
    }

    public TwitchStreamlinkException(String message, Throwable cause, Locale locale) {
        super("twitch", message, cause, locale);
    }
}
