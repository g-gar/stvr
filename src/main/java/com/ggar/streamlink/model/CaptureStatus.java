package com.ggar.streamlink.model;

/**
 * Represents the lifecycle status of a stream capture process.
 */
public enum CaptureStatus {
    /**
     * The capture process is currently active and running.
     */
    RUNNING,
    /**
     * The capture process finished successfully.
     */
    COMPLETED,
    /**
     * The capture process terminated due to an error.
     */
    FAILED,
    /**
     * The capture process was manually stopped by a user.
     */
    STOPPED
}