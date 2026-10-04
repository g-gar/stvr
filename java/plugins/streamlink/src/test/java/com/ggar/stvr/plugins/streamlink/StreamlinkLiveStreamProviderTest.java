package com.ggar.stvr.plugins.streamlink;

import com.ggar.stvr.catalog.entities.ChannelUrl;
import com.ggar.stvr.packages.streamlink.StreamlinkClient;
import com.ggar.stvr.packages.streamlink.session.StreamlinkSession;
import com.ggar.stvr.streaming.api.LiveStreamConnection;
import com.ggar.stvr.streaming.entities.StreamQuality;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StreamlinkLiveStreamProviderTest {

    @Mock
    private StreamlinkClient client;

    @Mock
    private StreamlinkSession session;

    private StreamlinkLiveStreamProvider provider;

    @BeforeEach
    void setUp() {
        provider = new StreamlinkLiveStreamProvider(client);
    }

    @Test
    @DisplayName("Should check support for channel URL")
    void shouldCheckSupportForUrl() {
        assertThat(provider.supports(ChannelUrl.of("https://twitch.tv/streamer"))).isTrue();
        assertThat(provider.supports(ChannelUrl.of("https://youtube.com/@channel"))).isTrue();
        assertThat(provider.supports(null)).isFalse();
    }

    @Test
    @DisplayName("Should open live stream and return wrapped LiveStreamConnection")
    void shouldOpenLiveStreamAndReturnConnection() {
        ChannelUrl url = ChannelUrl.of("https://twitch.tv/streamer");
        StreamQuality quality = StreamQuality.of("1080p60");

        byte[] chunk = new byte[]{1, 2, 3};
        when(client.openSession(eq("https://twitch.tv/streamer"), eq("1080p60")))
                .thenReturn(session);
        when(session.data()).thenReturn(Flux.just(chunk));
        when(session.isAlive()).thenReturn(true);

        StepVerifier.create(provider.openStream(url, quality))
                .assertNext(conn -> {
                    assertThat(conn.isAlive()).isTrue();
                    StepVerifier.create(conn.data())
                            .expectNext(chunk)
                            .verifyComplete();

                    conn.cancel();
                    verify(session).cancel();
                })
                .verifyComplete();
    }
}
