package com.ggar.streamlink.model;

/**
 * Represents a structured piece of output from the streamlink process's stderr.
 * This is a sealed interface, meaning only permitted classes can implement it.
 * This allows for type-safe handling of different kinds of log messages.
 */
public sealed interface StreamOutput {
    /**
     * The original, raw log line.
     * @return The raw string.
     */
    String rawLine();

    /**
     * Represents an informational message from a plugin.
     * e.g., "[plugin.twitch][info] Found matching plugin twitch for URL..."
     */
    record PluginOutput(String plugin, String level, String message, String rawLine) implements StreamOutput {}

    /**
     * Represents a download progress update.
     * e.g., "[download] 10.5% of 1.23GiB at 4.56MiB/s ETA 01:23:45"
     */
    record ProgressOutput(double percentage, String size, String speed, String eta, String rawLine) implements StreamOutput {}

    /**
     * Represents an error message.
     * e.g., "[cli][error] No playable streams found..."
     */
    record ErrorOutput(String component, String level, String message, String rawLine) implements StreamOutput {}

    /**
     * Represents a generic or unclassified log line that doesn't match other patterns.
     */
    record GenericOutput(String rawLine) implements StreamOutput {}

}
