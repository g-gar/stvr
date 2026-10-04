package com.ggar.stvr.plugins.streamlink.sidecar;

import com.ggar.stvr.packages.streamlink.session.StreamlinkSession;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StreamlinkSidecarCommandExecutorTest {

    @Test
    void shouldExecuteCommandViaHttpSuccessfully() {
        String jsonResponse = """
                {
                  "exitCode": 0,
                  "stdout": "{\\"plugin\\": \\"twitch\\"}",
                  "stderr": ""
                }
                """;

        ExchangeFunction exchangeFunction = request -> {
            assertThat(request.url().getPath()).isEqualTo("/api/v1/execute");
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .body(jsonResponse)
                    .build());
        };

        WebClient webClient = WebClient.builder()
                .baseUrl("http://streamlink-sidecar:8000")
                .exchangeFunction(exchangeFunction)
                .build();

        StreamlinkSidecarCommandExecutor executor = new StreamlinkSidecarCommandExecutor(webClient, "http://streamlink-sidecar:8000");

        StepVerifier.create(executor.execute(List.of("--json", "https://twitch.tv/test")))
                .assertNext(res -> {
                    assertThat(res.isSuccess()).isTrue();
                    assertThat(res.stdout()).contains("twitch");
                })
                .verifyComplete();
    }

    @Test
    void shouldStreamDataViaHttpSuccessfully() {
        byte[] chunk1 = "video-chunk-1".getBytes(StandardCharsets.UTF_8);
        byte[] chunk2 = "video-chunk-2".getBytes(StandardCharsets.UTF_8);

        DefaultDataBufferFactory bufferFactory = new DefaultDataBufferFactory();

        ExchangeFunction exchangeFunction = request -> {
            assertThat(request.url().getPath()).isEqualTo("/api/v1/stream");
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE)
                    .body(Flux.just(bufferFactory.wrap(chunk1), bufferFactory.wrap(chunk2)))
                    .build());
        };

        WebClient webClient = WebClient.builder()
                .baseUrl("http://streamlink-sidecar:8000")
                .exchangeFunction(exchangeFunction)
                .build();

        StreamlinkSidecarCommandExecutor executor = new StreamlinkSidecarCommandExecutor(webClient, "http://streamlink-sidecar:8000");
        StreamlinkSession session = executor.openSession(List.of("https://twitch.tv/test", "best"));

        assertThat(session.isAlive()).isTrue();

        StepVerifier.create(session.data())
                .assertNext(data -> assertThat(new String(data)).isEqualTo("video-chunk-1"))
                .assertNext(data -> assertThat(new String(data)).isEqualTo("video-chunk-2"))
                .verifyComplete();

        StepVerifier.create(session.result())
                .assertNext(res -> assertThat(res.isSuccess()).isTrue())
                .verifyComplete();
    }
}
