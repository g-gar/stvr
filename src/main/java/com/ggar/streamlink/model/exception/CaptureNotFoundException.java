package com.ggar.streamlink.model.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class CaptureNotFoundException extends RuntimeException {
    public CaptureNotFoundException(UUID id) { super("Capture with ID " + id + " not found."); }
}