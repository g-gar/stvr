package com.ggar.streamlink.model.dto;

import jakarta.validation.constraints.NotEmpty;

public record NewStreamerInput(@NotEmpty String platformId, @NotEmpty String name) {}
