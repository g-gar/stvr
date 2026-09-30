package com.ggar.stvr.packages.streamlink.exception;

import com.ggar.stvr.packages.streamlink.i18n.StreamlinkMessages;
import lombok.Getter;

import java.util.Locale;

/**
 * Base exception for errors originating from or related to a specific Streamlink plugin.
 */
@Getter
public class StreamlinkPluginException extends StreamlinkException {

    private final String pluginName;

    public StreamlinkPluginException(String pluginName, String message) {
        this(pluginName, message, Locale.ENGLISH);
    }

    public StreamlinkPluginException(String pluginName, String message, Locale locale) {
        super(resolveMessage(pluginName, message, locale));
        this.pluginName = pluginName;
    }

    public StreamlinkPluginException(String pluginName, String message, Throwable cause) {
        this(pluginName, message, cause, Locale.ENGLISH);
    }

    public StreamlinkPluginException(String pluginName, String message, Throwable cause, Locale locale) {
        super(resolveMessage(pluginName, message, locale), cause);
        this.pluginName = pluginName;
    }

    private static String resolveMessage(String pluginName, String message, Locale locale) {
        String key = "error.plugin." + (pluginName != null ? pluginName.toLowerCase() : "generic");
        String formatted = StreamlinkMessages.get(key, locale, message != null ? message : "", pluginName);
        if (formatted.equals(key)) {
            return String.format("[%s] %s", pluginName, message);
        }
        return formatted;
    }
}
