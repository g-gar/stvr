package com.ggar.stvr.plugins.streamlink;

import com.ggar.stvr.catalog.entities.ChannelUrl;
import com.ggar.stvr.catalog.entities.Platform;
import com.ggar.stvr.inspection.api.InspectionChain;
import com.ggar.stvr.inspection.entities.StreamInfo;
import com.ggar.stvr.packages.streamlink.StreamlinkClient;
import com.ggar.stvr.packages.streamlink.exception.StreamlinkNoStreamsException;
import com.ggar.stvr.packages.streamlink.model.StreamlinkMetadata;
import com.ggar.stvr.packages.streamlink.model.StreamlinkStreamDetails;
import com.ggar.stvr.packages.streamlink.model.StreamlinkStreamInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StreamlinkInspectionPluginTest {

    @Mock
    private StreamlinkClient client;

    private StreamlinkInspectionPlugin plugin;
    private final InspectionChain identityChain = Mono::just;

    @BeforeEach
    void setUp() {
        plugin = new StreamlinkInspectionPlugin(client);
    }

    @Test
    void shouldEnrichStreamInfoWhenStreamIsLive() {
        ChannelUrl url = ChannelUrl.of("https://twitch.tv/ibai");
        StreamInfo initial = StreamInfo.builder()
                .channelUrl(url)
                .platform(Platform.of("twitch"))
                .build();

        StreamlinkMetadata metadata = new StreamlinkMetadata("123", "ibai", "Just Chatting", "Charlando");
        StreamlinkStreamDetails details = new StreamlinkStreamDetails("hls", "https://stream.url", null, Map.of());
        StreamlinkStreamInfo streamlinkResult = new StreamlinkStreamInfo("twitch", metadata, Map.of("1080p60", details, "720p", details));

        when(client.inspect(anyString())).thenReturn(Mono.just(streamlinkResult));

        StepVerifier.create(plugin.inspect(initial, identityChain))
                .assertNext(enriched -> {
                    assertThat(enriched.isLive()).isTrue();
                    assertThat(enriched.getTitle()).isEqualTo("Charlando");
                    assertThat(enriched.getCategory()).isEqualTo("Just Chatting");
                    assertThat(enriched.getAvailableQualities()).containsExactlyInAnyOrder("1080p60", "720p");
                })
                .verifyComplete();
    }

    @Test
    void shouldSetLiveFalseWhenNoPlayableStreamsException() {
        ChannelUrl url = ChannelUrl.of("https://twitch.tv/offline_channel");
        StreamInfo initial = StreamInfo.builder()
                .channelUrl(url)
                .platform(Platform.of("twitch"))
                .live(true)
                .build();

        when(client.inspect(anyString()))
                .thenReturn(Mono.error(new StreamlinkNoStreamsException(url.asString(), "No playable streams")));

        StepVerifier.create(plugin.inspect(initial, identityChain))
                .assertNext(enriched -> {
                    assertThat(enriched.isLive()).isFalse();
                })
                .verifyComplete();
    }

    @Test
    void shouldSetLiveFalseAndProceedWhenUnexpectedError() {
        ChannelUrl url = ChannelUrl.of("https://twitch.tv/broken");
        StreamInfo initial = StreamInfo.builder()
                .channelUrl(url)
                .platform(Platform.of("twitch"))
                .live(true)
                .build();

        when(client.inspect(anyString()))
                .thenReturn(Mono.error(new RuntimeException("Network timeout")));

        StepVerifier.create(plugin.inspect(initial, identityChain))
                .assertNext(enriched -> {
                    assertThat(enriched.isLive()).isFalse();
                })
                .verifyComplete();
    }
}
