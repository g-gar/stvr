package com.ggar.stvr.packages.streamlink.exception;

import com.ggar.stvr.packages.streamlink.i18n.StreamlinkMessages;
import lombok.Getter;

import java.util.Locale;

/**
 * Thrown when output from Streamlink cannot be parsed as expected.
 */
@Getter
public class StreamlinkParseException extends StreamlinkException {

    private final String rawOutput;

    public StreamlinkParseException(String rawOutput, Throwable cause) {
        this(rawOutput, cause, Locale.ENGLISH);
    }

    public StreamlinkParseException(String rawOutput, Throwable cause, Locale locale) {
        super(StreamlinkMessages.get("error.parse_failed", locale, rawOutput), cause);
        this.rawOutput = rawOutput;
    }
}
