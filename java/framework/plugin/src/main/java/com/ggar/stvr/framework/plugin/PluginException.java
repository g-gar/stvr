package com.ggar.stvr.framework.plugin;

/**
 * Exception thrown when an error occurs during plugin discovery, loading, or execution.
 */
public class PluginException extends RuntimeException {

    public PluginException(String message) {
        super(message);
    }

    public PluginException(String message, Throwable cause) {
        super(message, cause);
    }
}
