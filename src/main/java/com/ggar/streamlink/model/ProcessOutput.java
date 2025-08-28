package com.ggar.streamlink.model;

import java.io.InputStream;

public record ProcessOutput(
    InputStream stdout,
    InputStream stderr,
    Process process
) {}
