package com.ggar.stvr.packages.streamlink;

import com.ggar.stvr.packages.streamlink.command.StreamlinkCommand;
import com.ggar.stvr.packages.streamlink.exception.StreamlinkExecutionException;
import com.ggar.stvr.packages.streamlink.exception.StreamlinkNoStreamsException;
import com.ggar.stvr.packages.streamlink.executor.CommandExecutor;
import com.ggar.stvr.packages.streamlink.executor.CommandResult;
import com.ggar.stvr.packages.streamlink.model.StreamlinkInspection;
import com.ggar.stvr.packages.streamlink.plugins.twitch.TwitchCommandBuilder;
import com.ggar.stvr.packages.streamlink.session.StreamlinkSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StreamlinkClientTest {

    @Mock
    private CommandExecutor executor;

    private StreamlinkClient client;

    @BeforeEach
    void setUp() {
        client = new StreamlinkClient(executor);
    }

    @Test
    void testInspectSuccessfulWithAutoDetectedPlatform() {
        String json = """
                {
                  "plugin": "twitch",
                  "metadata": {
                    "id": "100",
                    "author": "testuser",
                    "category": "Gaming",
                    "title": "Playing Games"
                  },
                  "streams": {
                    "best": {
                      "type": "hls",
                      "url": "https://stream.url"
                    }
                  }
                }
                """;

        when(executor.execute(anyList()))
                .thenReturn(Mono.just(new CommandResult(0, json, "")));

        // Automatically resolves Twitch platform from URL without manually specifying it
        StepVerifier.create(client.inspect("https://twitch.tv/testuser"))
                .assertNext(inspection -> {
                    assertThat(inspection.plugin()).isEqualTo("twitch");
                    assertThat(inspection.metadata().author()).isEqualTo("testuser");
                    assertThat(inspection.streams()).containsKey("best");
                })
                .verifyComplete();

        verify(executor).execute(argThat(args ->
                args.contains("--json") && args.contains("https://twitch.tv/testuser")
        ));
    }

    @Test
    void testInspectWithCustomOptionsViaFromUrl() {
        String json = """
                {
                  "plugin": "twitch",
                  "metadata": { "id": "100", "author": "testuser", "category": "Gaming", "title": "Playing" },
                  "streams": { "best": { "type": "hls", "url": "https://stream.url" } }
                }
                """;

        when(executor.execute(anyList()))
                .thenReturn(Mono.just(new CommandResult(0, json, "")));

        StreamlinkCommand cmd = StreamlinkCommand.fromUrl("https://twitch.tv/testuser", TwitchCommandBuilder.class)
                .lowLatency()
                .build();

        StepVerifier.create(client.inspect(cmd))
                .assertNext(inspection -> assertThat(inspection.plugin()).isEqualTo("twitch"))
                .verifyComplete();

        verify(executor).execute(argThat(args ->
                args.contains("--json") && args.contains("--twitch-low-latency")
        ));
    }

    @Test
    void testInspectWhenOfflineWithExitCode1() {
        String errorJson = """
                {
                  "error": "No playable streams found on this URL: https://twitch.tv/offline"
                }
                """;

        when(executor.execute(anyList()))
                .thenReturn(Mono.just(new CommandResult(1, errorJson, "error")));

        // Calling inspect directly with the URL automatically recognizes the platform
        StepVerifier.create(client.inspect("https://twitch.tv/offline"))
                .expectError(StreamlinkNoStreamsException.class)
                .verify();
    }

    @Test
    void testInspectExecutionFailureEnglishDefault() {
        when(executor.execute(anyList()))
                .thenReturn(Mono.just(new CommandResult(127, "", "streamlink: command not found")));

        StepVerifier.create(client.inspect("https://twitch.tv/testuser"))
                .expectErrorMatches(err -> err instanceof StreamlinkExecutionException ex
                        && ex.getExitCode() == 127
                        && ex.getMessage().contains("Streamlink process failed with exit code 127"))
                .verify();
    }

    @Test
    void testInspectWithSpanishLocaleOnExecutionError() {
        when(executor.execute(anyList()))
                .thenReturn(Mono.just(new CommandResult(127, "", "streamlink: command not found")));

        StepVerifier.create(client.inspect("https://twitch.tv/testuser", Locale.forLanguageTag("es")))
                .expectErrorMatches(err -> err instanceof StreamlinkExecutionException ex
                        && ex.getExitCode() == 127
                        && ex.getMessage().contains("El proceso de Streamlink falló con código de salida 127"))
                .verify();
    }

    @Test
    void testInspectWithSpanishLocaleOnOffline() {
        String errorJson = """
                {
                  "error": "No playable streams found on this URL: https://twitch.tv/offline"
                }
                """;

        when(executor.execute(anyList()))
                .thenReturn(Mono.just(new CommandResult(1, errorJson, "error")));

        StepVerifier.create(client.inspect("https://twitch.tv/offline", Locale.forLanguageTag("es")))
                .expectErrorMatches(err -> err instanceof StreamlinkNoStreamsException ex
                        && ex.getMessage().contains("No se encontraron transmisiones reproducibles para la URL"))
                .verify();
    }

    @Test
    void testWithLocaleConfiguration() {
        Locale spanish = Locale.forLanguageTag("es");
        StreamlinkClient spanishClient = client.withLocale(spanish);

        assertThat(spanishClient.getDefaultLocale()).isEqualTo(spanish);
        assertThat(client.getDefaultLocale()).isEqualTo(Locale.ENGLISH);
    }

    @Test
    void testOpenSession() {
        StreamlinkSession session = mock(StreamlinkSession.class);
        when(session.isAlive()).thenReturn(true);
        when(executor.openSession(anyList())).thenReturn(session);

        StreamlinkSession returnedSession =
                client.openSession("https://twitch.tv/testuser", "best");

        assertThat(returnedSession).isSameAs(session);
        assertThat(returnedSession.isAlive()).isTrue();

        verify(executor).openSession(argThat(args ->
                args.contains("-O") && args.contains("best") && args.contains("https://twitch.tv/testuser")
        ));
    }

    @Test
    void testStreamDelegation() {
        byte[] chunk1 = new byte[]{1, 2, 3};
        byte[] chunk2 = new byte[]{4, 5, 6};

        StreamlinkSession session = mock(StreamlinkSession.class);
        when(session.data()).thenReturn(Flux.just(chunk1, chunk2));
        when(executor.openSession(anyList())).thenReturn(session);

        // Directly invoke stream with URL and quality
        StepVerifier.create(client.stream("https://twitch.tv/testuser", "best"))
                .expectNext(chunk1)
                .expectNext(chunk2)
                .verifyComplete();

        verify(executor).openSession(argThat(args ->
                args.contains("-O") && args.contains("best") && args.contains("https://twitch.tv/testuser")
        ));
    }

    @Test
    void testExecuteRaw() {
        CommandResult result = new CommandResult(0, "version 6.0", "");
        when(executor.execute(anyList())).thenReturn(Mono.just(result));

        StreamlinkCommand cmd = StreamlinkCommand.of("https://twitch.tv/testuser");

        StepVerifier.create(client.executeRaw(cmd))
                .expectNext(result)
                .verifyComplete();
    }
}
