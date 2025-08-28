package com.ggar.streamlink.model;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * An immutable data record representing the state of a stream capture.
 *
 * @param id A unique identifier for the capture.
 * @param url The source URL of the stream.
 * @param quality The selected quality of the stream.
 * @param destination The file path where the capture is being saved.
 * @param status The current lifecycle status of the capture.
 * @param startTime The timestamp when the capture was initiated.
 * @param endTime The timestamp when the capture finished (completed, failed, or stopped). Null if still running.
 * @param error An error message if the capture failed. Null otherwise.
 */
public record CaptureInfo(
    UUID id,
    String url,
    String quality,
    Path destination,
    CaptureStatus status,
    LocalDateTime startTime,
    LocalDateTime endTime,
    String error
) {
    /**
     * Creates a new CaptureInfo instance with an updated status.
     * @param newStatus The new status.
     * @return A new, updated CaptureInfo instance.
     */
    public CaptureInfo withStatus(CaptureStatus newStatus) {
        return new CaptureInfo(this.id, this.url, this.quality, this.destination, newStatus, this.startTime, this.endTime, this.error);
    }

    /**
     * Creates a new CaptureInfo instance with an updated end time.
     * @param newEndTime The new end time.
     * @return A new, updated CaptureInfo instance.
     */
    public CaptureInfo withEndTime(LocalDateTime newEndTime) {
        return new CaptureInfo(this.id, this.url, this.quality, this.destination, this.status, this.startTime, newEndTime, this.error);
    }

    /**
     * Creates a new CaptureInfo instance with an updated error message.
     * @param newError The new error message.
     * @return A new, updated CaptureInfo instance.
     */
    public CaptureInfo withError(String newError) {
        return new CaptureInfo(this.id, this.url, this.quality, this.destination, this.status, this.startTime, this.endTime, newError);
    }
}