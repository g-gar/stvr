package com.ggar.stvr.plugins.streamlink;

import com.ggar.stvr.catalog.entities.ChannelName;
import com.ggar.stvr.catalog.entities.ChannelUrl;
import com.ggar.stvr.catalog.entities.Platform;
import com.ggar.stvr.packages.streamlink.StreamlinkClient;
import com.ggar.stvr.packages.streamlink.exception.StreamlinkNoStreamsException;
import com.ggar.stvr.packages.streamlink.exception.StreamlinkPluginNotFoundException;
import com.ggar.stvr.packages.streamlink.model.StreamlinkMetadata;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StreamlinkChannelResolverTest {

    @Mock
    private StreamlinkClient client;

    private StreamlinkChannelResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new StreamlinkChannelResolver(client);
    }

    @Test
    void shouldResolveTwitchLiveChannel() {
        ChannelUrl url = ChannelUrl.of("https://twitch.tv/ibai");
        assertThat(resolver.supports(url)).isTrue();

        StreamlinkMetadata metadata = new StreamlinkMetadata("123", "ibai", "Just Chatting", "Stream Title");
        StreamlinkStreamInfo streamInfo = new StreamlinkStreamInfo("twitch", metadata, Map.of());
        when(client.inspect(url.asString())).thenReturn(Mono.just(streamInfo));

        StepVerifier.create(resolver.resolve(url))
                .assertNext(res -> {
                    assertThat(res.platform()).isEqualTo(Platform.of("twitch"));
                    assertThat(res.slug()).isEqualTo("ibai");
                    assertThat(res.name()).isEqualTo(ChannelName.of("ibai"));
                })
                .verifyComplete();
    }

    @Test
    void shouldResolveKickLiveChannel() {
        ChannelUrl url = ChannelUrl.of("https://kick.com/xqc");
        assertThat(resolver.supports(url)).isTrue();

        StreamlinkMetadata metadata = new StreamlinkMetadata("456", "xqc", "Gaming", "Kick Stream");
        StreamlinkStreamInfo streamInfo = new StreamlinkStreamInfo("kick", metadata, Map.of());
        when(client.inspect(url.asString())).thenReturn(Mono.just(streamInfo));

        StepVerifier.create(resolver.resolve(url))
                .assertNext(res -> {
                    assertThat(res.platform()).isEqualTo(Platform.of("kick"));
                    assertThat(res.slug()).isEqualTo("xqc");
                    assertThat(res.name()).isEqualTo(ChannelName.of("xqc"));
                })
                .verifyComplete();
    }

    @Test
    void shouldResolveOfflineChannelWhenStreamlinkRecognizesPlatform() {
        ChannelUrl url = ChannelUrl.of("https://www.youtube.com/@LofiGirl");
        assertThat(resolver.supports(url)).isTrue();

        when(client.inspect(url.asString()))
                .thenReturn(Mono.error(new StreamlinkNoStreamsException(url.asString(), "youtube", "No playable streams found")));

        StepVerifier.create(resolver.resolve(url))
                .assertNext(res -> {
                    assertThat(res.platform()).isEqualTo(Platform.of("youtube"));
                    assertThat(res.slug()).isEqualTo("LofiGirl");
                    assertThat(res.name()).isEqualTo(ChannelName.of("LofiGirl"));
                })
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenStreamlinkCannotHandleUrl() {
        ChannelUrl url = ChannelUrl.of("https://unknown-service.com/channel/abc");
        assertThat(resolver.supports(url)).isTrue();

        when(client.inspect(url.asString()))
                .thenReturn(Mono.error(new StreamlinkPluginNotFoundException(url.asString(), "No plugin can handle URL")));

        StepVerifier.create(resolver.resolve(url))
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyOnUnexpectedError() {
        ChannelUrl url = ChannelUrl.of("https://twitch.tv/network-error");

        when(client.inspect(url.asString()))
                .thenReturn(Mono.error(new RuntimeException("Connection refused")));

        StepVerifier.create(resolver.resolve(url))
                .verifyComplete();
    }

    @Test
    void shouldResolveArbitraryPlatformSuccessfully() {
        ChannelUrl url = ChannelUrl.of("https://dailymotion.com/video/x123");
        StreamlinkMetadata metadata = new StreamlinkMetadata("999", "news_channel", "News", "Live News");
        StreamlinkStreamInfo streamInfo = new StreamlinkStreamInfo("dailymotion", metadata, Map.of());

        when(client.inspect(url.asString())).thenReturn(Mono.just(streamInfo));

        StepVerifier.create(resolver.resolve(url))
                .assertNext(res -> {
                    assertThat(res.platform()).isEqualTo(Platform.of("dailymotion"));
                    assertThat(res.platform().value()).isEqualTo("dailymotion");
                    assertThat(res.slug()).isEqualTo("news_channel");
                    assertThat(res.name()).isEqualTo(ChannelName.of("news_channel"));
                })
                .verifyComplete();
    }
}
