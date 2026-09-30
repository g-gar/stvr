package com.ggar.stvr.packages.streamlink.plugins.kick;

import com.ggar.stvr.packages.streamlink.exception.StreamlinkPluginException;

import java.util.Locale;

public class KickStreamlinkException extends StreamlinkPluginException {

    public KickStreamlinkException(String message) {
        super("kick", message);
    }

    public KickStreamlinkException(String message, Locale locale) {
        super("kick", message, locale);
    }

    public KickStreamlinkException(String message, Throwable cause) {
        super("kick", message, cause);
    }

    public KickStreamlinkException(String message, Throwable cause, Locale locale) {
        super("kick", message, cause, locale);
    }
}
