package com.ggar.streamlink.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;

@Slf4j
@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path captureBasePath;

    public FileStorageServiceImpl(@Value("${streamlink.capture.path:/tmp/captures}") String capturePath) {
        this.captureBasePath = Paths.get(capturePath);
        try {
            Files.createDirectories(this.captureBasePath);
            log.info("Capture storage directory initialized at: {}", this.captureBasePath);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directory: " + capturePath, e);
        }
    }

    @Override
    public Mono<Void> saveStream(Flux<DataBuffer> stream, Path destination) {
        return Mono.fromRunnable(() -> {
                try {
                    // Ensure parent directories exist
                    Files.createDirectories(destination.getParent());
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create parent directories for: " + destination, e);
                }
            })
            .subscribeOn(Schedulers.boundedElastic())
            .then(Mono.defer(() -> {
                try {
                    AsynchronousFileChannel channel = AsynchronousFileChannel.open(
                        destination,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING
                    );
                    return DataBufferUtils.write(stream, channel)
                        .doOnTerminate(() -> {
                            try {
                                channel.close();
                            } catch (IOException e) {
                                log.error("Failed to close file channel for {}", destination, e);
                            }
                        })
                        .then();
                } catch (IOException e) {
                    return Mono.error(e);
                }
            }));
    }

    @Override
    public Mono<Void> deleteFile(Path path) {
        return Mono.fromRunnable(() -> {
            try {
                Files.deleteIfExists(path);
                log.info("Deleted partial file: {}", path);
            } catch (IOException e) {
                log.error("Failed to delete file: {}", path, e);
                throw new RuntimeException("Failed to delete file: " + path, e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Flux<Path> listCompletedFiles() {
        return Flux.using(
                () -> Files.list(this.captureBasePath),
                Flux::fromStream,
                stream -> stream.close()
            )
            .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".ts"))
            .subscribeOn(Schedulers.boundedElastic())
            .doOnError(e -> log.error("Failed to list files in {}", captureBasePath, e));
    }

    @Override
    public Mono<Resource> loadFileAsResource(String filename) {
        return Mono.fromCallable(() -> {
                Path filePath = this.captureBasePath.resolve(filename).normalize();

                // Security check to prevent directory traversal attacks
                if (!filePath.startsWith(this.captureBasePath)) {
                    throw new MalformedURLException("Cannot access file outside of capture directory: " + filename);
                }

                Resource resource = new UrlResource(filePath.toUri());
                if (resource.exists() && resource.isReadable()) {
                    return resource;
                } else {
                    // Return null to signal an empty Mono, which will result in a 404 in the controller
                    return null;
                }
            })
            .flatMap(Mono::justOrEmpty) // Handles the null case gracefully
            .subscribeOn(Schedulers.boundedElastic())
            .doOnError(MalformedURLException.class, e -> log.warn("Blocked potential directory traversal attempt: {}", e.getMessage()))
            .doOnError(e -> !(e instanceof MalformedURLException), e -> log.error("Error reading file: {}", filename, e));
    }

    @Override
    public Mono<Resource> loadFileAsResourceWhenReady(String filename) {
        return loadFileAsResource(filename)
            .filter(resource -> {
                try {
                    return resource.contentLength() > 0;
                } catch (IOException e) {
                    log.error("Could not get content length for resource: {}", filename, e);
                    return false;
                }
            })
            .switchIfEmpty(Mono.error(new IOException("Resource is empty")))
            .retryWhen(Retry.backoff(20, Duration.ofMillis(250)).maxBackoff(Duration.ofSeconds(1))
                .doBeforeRetry(retrySignal -> log.info("File {} is not ready yet, retrying... ({})", filename, retrySignal.totalRetries() + 1)))
            .timeout(Duration.ofSeconds(10))
            .doOnError(throwable -> log.error("Failed to load resource {} after multiple retries.", filename, throwable));
    }
}
