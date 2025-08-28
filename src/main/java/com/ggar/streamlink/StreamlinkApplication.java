package com.ggar.streamlink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.ggar.streamlink.config.StreamlinkProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(StreamlinkProperties.class)
public class StreamlinkApplication {
    public static void main(String[] args) {
        SpringApplication.run(StreamlinkApplication.class, args);
    }

}
