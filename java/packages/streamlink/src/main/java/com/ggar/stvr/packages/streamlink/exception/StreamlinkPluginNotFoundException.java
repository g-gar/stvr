package com.ggar.stvr.packages.streamlink.exception;

import com.ggar.stvr.packages.streamlink.i18n.StreamlinkMessages;
import lombok.Getter;

import java.util.Locale;

/**
 * Thrown when Streamlink does not recognize or support the URL (no plugin found).
 */
@Getter
public class StreamlinkPluginNotFoundException extends StreamlinkException {

    private final String url;
    private final String rawMessage;

    public StreamlinkPluginNotFoundException(String url, String rawMessage) {
        this(url, rawMessage, Locale.ENGLISH);
    }

    public StreamlinkPluginNotFoundException(String url, String rawMessage, Locale locale) {
        super(StreamlinkMessages.get("error.plugin_not_found", locale, url != null ? url : "", rawMessage));
        this.url = url;
        this.rawMessage = rawMessage;
    }
}
