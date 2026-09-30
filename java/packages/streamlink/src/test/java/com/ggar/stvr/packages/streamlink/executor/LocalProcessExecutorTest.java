package com.ggar.stvr.packages.streamlink.executor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LocalProcessExecutorTest {

    private LocalProcessExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new LocalProcessExecutor();
    }

    @Test
    void testExecuteSimpleCommand() {
        StepVerifier.create(executor.execute(List.of("echo", "streamlink-runner-test")))
                .assertNext(result -> {
                    assertThat(result.isSuccess()).isTrue();
                    assertThat(result.exitCode()).isEqualTo(0);
                    assertThat(result.stdout().trim()).isEqualTo("streamlink-runner-test");
                })
                .verifyComplete();
    }

    @Test
    void testExecuteCommandWithNonZeroExitCode() {
        // 'sh -c "exit 42"' returns exit code 42
        StepVerifier.create(executor.execute(List.of("sh", "-c", "exit 42")))
                .assertNext(result -> {
                    assertThat(result.isSuccess()).isFalse();
                    assertThat(result.exitCode()).isEqualTo(42);
                })
                .verifyComplete();
    }

    @Test
    void testStreamSimpleCommand() {
        StepVerifier.create(executor.stream(List.of("echo", "data-chunk")))
                .assertNext(bytes -> {
                    String str = new String(bytes);
                    assertThat(str).contains("data-chunk");
                })
                .verifyComplete();
    }
}
