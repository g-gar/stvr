package com.ggar.stvr.packages.streamlink.exception;

import com.ggar.stvr.packages.streamlink.i18n.StreamlinkMessages;
import lombok.Getter;

import java.util.Locale;

/**
 * Thrown when Streamlink reports that no streams are available for a given URL (e.g. channel is offline).
 */
@Getter
public class StreamlinkNoStreamsException extends StreamlinkException {

    private final String url;
    private final String pluginName;
    private final String rawMessage;

    public StreamlinkNoStreamsException(String url, String rawMessage) {
        this(url, null, rawMessage, Locale.ENGLISH);
    }

    public StreamlinkNoStreamsException(String url, String rawMessage, Locale locale) {
        this(url, null, rawMessage, locale);
    }

    public StreamlinkNoStreamsException(String url, String pluginName, String rawMessage) {
        this(url, pluginName, rawMessage, Locale.ENGLISH);
    }

    public StreamlinkNoStreamsException(String url, String pluginName, String rawMessage, Locale locale) {
        super(StreamlinkMessages.get("error.no_streams", locale, url != null ? url : "", rawMessage));
        this.url = url;
        this.pluginName = pluginName;
        this.rawMessage = rawMessage;
    }
}
