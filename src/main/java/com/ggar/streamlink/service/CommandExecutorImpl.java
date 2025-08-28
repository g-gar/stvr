package com.ggar.streamlink.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import com.ggar.streamlink.model.StreamingProcessOutput;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


@Service
public class CommandExecutorImpl implements CommandExecutor {

    @Override
    public Mono<String> execute(String... command) {
        Mono<Process> processMono = Mono.fromCallable(() ->
                new ProcessBuilder(command).start()
        ).subscribeOn(Schedulers.boundedElastic());

        return Mono.usingWhen(
                processMono,
                process -> {
                    Mono<String> output = readStream(process.getInputStream());
                    Mono<String> error = readStream(process.getErrorStream());

                    return Mono.fromFuture(process.onExit())
                            .flatMap(p -> {
                                if (p.exitValue() == 0) {
                                    return output;
                                } else {
                                    return error.flatMap(err -> Mono.error(new RuntimeException("Command execution failed for '" + String.join(" ", command) + "': " + err)));
                                }
                            });
                },
                process -> Mono.fromRunnable(process::destroy)
        );
    }

    @Override
    public Mono<StreamingProcessOutput> executeAndStream(String... command) {
        return Mono.fromCallable(() -> {
            Process process = new ProcessBuilder(command).start();

            Flux<DataBuffer> stdoutFlux = DataBufferUtils.readInputStream(
                    process::getInputStream,
                    new DefaultDataBufferFactory(),
                    8192
            );

            // Attach the process lifecycle management to the Flux
            Flux<DataBuffer> managedStdoutFlux = Flux.usingWhen(
                    Mono.just(process),
                    p -> stdoutFlux,
                    p -> Mono.fromFuture(p.onExit()).then(), // Cleanup when complete
                    (p, err) -> Mono.fromRunnable(p::destroyForcibly), // Cleanup on error
                    p -> Mono.fromRunnable(p::destroyForcibly) // Cleanup on cancel
            );

            return new StreamingProcessOutput(managedStdoutFlux, process.getErrorStream(), process);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<String> readStream(InputStream inputStream) {
        return Mono.fromCallable(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
