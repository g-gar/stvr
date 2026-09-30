package com.ggar.stvr.packages.streamlink.exception;

import com.ggar.stvr.packages.streamlink.i18n.StreamlinkMessages;
import lombok.Getter;

import java.util.Locale;

/**
 * Thrown when the Streamlink CLI command exits with a non-zero exit code.
 */
@Getter
public class StreamlinkExecutionException extends StreamlinkException {

    private final int exitCode;
    private final String stdout;
    private final String stderr;

    public StreamlinkExecutionException(int exitCode, String stdout, String stderr) {
        this(exitCode, stdout, stderr, Locale.ENGLISH);
    }

    public StreamlinkExecutionException(int exitCode, String stdout, String stderr, Locale locale) {
        super(StreamlinkMessages.get("error.execution_failed", locale, exitCode, stderr != null && !stderr.isBlank() ? stderr : stdout));
        this.exitCode = exitCode;
        this.stdout = stdout;
        this.stderr = stderr;
    }

    public StreamlinkExecutionException(String message, Throwable cause) {
        super(message, cause);
        this.exitCode = -1;
        this.stdout = "";
        this.stderr = message;
    }
}
