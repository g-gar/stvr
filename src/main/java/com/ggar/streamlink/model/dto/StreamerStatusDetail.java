package com.ggar.streamlink.model.dto;

import com.ggar.streamlink.model.CaptureStatus;
import com.ggar.streamlink.service.StreamStatus;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class StreamerStatusDetail {
    Long streamerId;
    String streamerName;
    String streamerUrl;
    String platformName;
    String platformUrl;
    StreamStatus onlineStatus;
    boolean isCapturing;
    UUID captureId;
    CaptureStatus captureStatus;
}