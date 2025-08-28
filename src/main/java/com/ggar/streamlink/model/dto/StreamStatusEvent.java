package com.ggar.streamlink.model.dto;

import com.ggar.streamlink.service.StreamStatus;

public record StreamStatusEvent(
    Long streamerId,
    String streamerName,
    StreamStatus oldStatus,
    StreamStatus newStatus
) {}