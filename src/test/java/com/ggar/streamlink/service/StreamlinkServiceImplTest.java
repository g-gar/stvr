package com.ggar.streamlink.service;

import com.ggar.streamlink.model.StreamInfo;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.buffer.DataBuffer;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.ByteBuffer;

/**
 * Integration test for StreamlinkServiceImpl.
 * <p>
 * IMPORTANT: This test requires the 'streamlink' command-line tool to be installed
 * and accessible in the system's PATH. It also requires a live stream URL to be provided.
 * </p>
 */
@SpringBootTest
public class StreamlinkServiceImplTest {

    @Autowired
    private StreamlinkService streamlinkService;

    /**
     * Test case for fetching stream information from a live URL.
     * <p>
     * 1. Replace the placeholder URL with a valid, currently live stream URL.
     * 2. Enable the test by removing the @Disabled annotation.
     * 3. Run the test.
     * </p>
     */
    // @Test
    // // @Disabled("Requires a live stream URL and manual execution.")
    // public void getStreamInfo_withLiveUrl_returnsStreamInfo() {
    //     // --- !!! IMPORTANT !!! ---
    //     // Replace this with a URL of a stream that is currently live.
    //     // For example: "https://www.twitch.tv/yourfavoritestreamer"
    //     String liveStreamUrl = "url";

    //     Mono<StreamInfo> streamInfoMono = streamlinkService.getStreamInfo(liveStreamUrl);

    //     StepVerifier.create(streamInfoMono)
    //             .assertNext(streamInfo -> {
    //                 System.out.println("Received StreamInfo: " + streamInfo);
    //                 assertNotNull(streamInfo, "StreamInfo should not be null");
    //                 assertNotNull(streamInfo.getPlugin(), "Plugin name should not be null");
    //                 assertNotNull(streamInfo.getStreams(), "Streams map should not be null");
    //                 assertFalse(streamInfo.getStreams().isEmpty(), "Streams map should not be empty");

    //                 // You can add more specific assertions here
    //                 // For example, check for a specific quality
    //                 System.out.println("Successfully fetched stream info for plugin: " + streamInfo.getPlugin());
    //                 System.out.println("Available qualities: " + streamInfo.getStreams().keySet());
    //             })
    //             .verifyComplete();
    // }

    /**
     * Test case for capturing a stream from a live URL.
     * <p>
     * 1. Replace the placeholder URL with a valid, currently live stream URL.
     * 2. Replace the quality with a valid quality for the stream.
     * 3. Enable the test by removing the @Disabled annotation.
     * 4. Run the test.
     * </p>
     */
    // @Test
    // @Disabled("Requires a live stream URL and manual execution.")
    // public void captureStream_withLiveUrl_returnsStreamData() {
    //     // --- !!! IMPORTANT !!! ---
    //     // Replace this with a URL of a stream that is currently live.
    //     // For example: "https://www.twitch.tv/yourfavoritestreamer"
    //     String liveStreamUrl = "url";
    //     // Replace this with a quality that is available for the stream.
    //     String quality = "best";

    //     Flux<DataBuffer> streamFlux = streamlinkService.captureStream(liveStreamUrl, quality);

    //     StepVerifier.create(streamFlux.log())
    //             .expectNextCount(10) // Just check for a few chunks of data
    //             .thenCancel()
    //             .verify();
    // }
}
