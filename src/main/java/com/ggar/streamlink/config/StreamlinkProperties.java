package com.ggar.streamlink.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "streamlink")
public class StreamlinkProperties {

    /**
     * The command-line template to execute streamlink to get stream metadata.
     * Use ${url} as a placeholder for the stream URL.
     */
    private String commandTemplate;

    /**
     * The command-line template to execute streamlink to capture a stream.
     * Use ${url} for the stream URL and ${quality} for the desired quality.
     */
    private String streamCaptureTemplate;

}