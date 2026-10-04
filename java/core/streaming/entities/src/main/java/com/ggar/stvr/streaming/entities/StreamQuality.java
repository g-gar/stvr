package com.ggar.stvr.streaming.entities;

import java.io.Serializable;
import java.util.Objects;

/**
 * Value object representing requested or available video stream quality.
 * Examples: "best", "1080p60", "720p60", "480p", "audio_only".
 */
public record StreamQuality(String value) implements Serializable {

    public static final StreamQuality BEST = new StreamQuality("best");
    public static final StreamQuality WORST = new StreamQuality("worst");
    public static final StreamQuality AUDIO_ONLY = new StreamQuality("audio_only");

    public StreamQuality {
        if (value == null || value.isBlank()) {
            value = "best";
        } else {
            value = value.trim().toLowerCase();
        }
    }

    public static StreamQuality of(String value) {
        return new StreamQuality(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
