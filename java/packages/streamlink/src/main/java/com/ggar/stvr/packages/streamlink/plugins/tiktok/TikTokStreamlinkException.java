package com.ggar.stvr.packages.streamlink.plugins.tiktok;

import com.ggar.stvr.packages.streamlink.exception.StreamlinkPluginException;

import java.util.Locale;

public class TikTokStreamlinkException extends StreamlinkPluginException {

    public TikTokStreamlinkException(String message) {
        super("tiktok", message);
    }

    public TikTokStreamlinkException(String message, Locale locale) {
        super("tiktok", message, locale);
    }

    public TikTokStreamlinkException(String message, Throwable cause) {
        super("tiktok", message, cause);
    }

    public TikTokStreamlinkException(String message, Throwable cause, Locale locale) {
        super("tiktok", message, cause, locale);
    }
}
