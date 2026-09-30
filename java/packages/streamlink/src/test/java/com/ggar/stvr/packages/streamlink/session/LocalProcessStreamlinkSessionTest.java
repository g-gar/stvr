package com.ggar.stvr.packages.streamlink.session;

import com.ggar.stvr.packages.streamlink.executor.LocalProcessExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LocalProcessStreamlinkSessionTest {

    private LocalProcessExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new LocalProcessExecutor();
    }

    @Test
    void testSessionDataAndLogsConcurrently() {
        // Runs a command that outputs to stdout and stderr concurrently
        List<String> command = List.of("sh", "-c", "echo 'hello stdout'; echo 'warning stderr' >&2; exit 0");
        StreamlinkSession session = executor.openSession(command);

        assertThat(session).isNotNull();

        // 1. Verify stdout data
        StepVerifier.create(session.data())
                .assertNext(bytes -> {
                    String str = new String(bytes);
                    assertThat(str).contains("hello stdout");
                })
                .verifyComplete();

        // 2. Verify stderr logs
        StepVerifier.create(session.logs())
                .assertNext(line -> assertThat(line).contains("warning stderr"))
                .verifyComplete();

        // 3. Verify final process result
        StepVerifier.create(session.result())
                .assertNext(result -> {
                    assertThat(result.isSuccess()).isTrue();
                    assertThat(result.exitCode()).isEqualTo(0);
                    assertThat(result.stderr()).contains("warning stderr");
                })
                .verifyComplete();

        assertThat(session.isAlive()).isFalse();
    }

    @Test
    void testSessionCancelStopsRunningProcess() {
        // Runs a long-running process (sleep 30)
        List<String> command = List.of("sleep", "30");
        StreamlinkSession session = executor.openSession(command);

        assertThat(session.isAlive()).isTrue();

        // Cancel the session
        session.cancel();

        assertThat(session.isAlive()).isFalse();

        // Data and logs should terminate cleanly
        StepVerifier.create(session.data())
                .expectComplete()
                .verify(Duration.ofSeconds(3));

        StepVerifier.create(session.logs())
                .expectComplete()
                .verify(Duration.ofSeconds(3));
    }

    @Test
    void testDataCancellationTriggersProcessTermination() {
        // Runs sleep 30, but subscriber explicitly cancels the subscription
        List<String> command = List.of("sleep", "30");
        StreamlinkSession session = executor.openSession(command);

        assertThat(session.isAlive()).isTrue();

        // Cancelling the subscriber triggers doOnCancel and session cancellation
        StepVerifier.create(session.data())
                .thenCancel()
                .verify();

        assertThat(session.isAlive()).isFalse();
    }
}
