package com.ggar.stvr.packages.streamlink.plugins.youtube;

import com.ggar.stvr.packages.streamlink.exception.StreamlinkPluginException;

import java.util.Locale;

public class YouTubeStreamlinkException extends StreamlinkPluginException {

    public YouTubeStreamlinkException(String message) {
        super("youtube", message);
    }

    public YouTubeStreamlinkException(String message, Locale locale) {
        super("youtube", message, locale);
    }

    public YouTubeStreamlinkException(String message, Throwable cause) {
        super("youtube", message, cause);
    }

    public YouTubeStreamlinkException(String message, Throwable cause, Locale locale) {
        super("youtube", message, cause, locale);
    }
}
