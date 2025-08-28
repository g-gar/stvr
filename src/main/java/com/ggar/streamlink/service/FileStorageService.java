package com.ggar.streamlink.service;

import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Path;

/**
 * A unified service for handling file operations related to stream captures,
 * including saving, deleting, listing, and serving files.
 */
public interface FileStorageService {

    Mono<Void> saveStream(Flux<DataBuffer> stream, Path destination);

    Mono<Void> deleteFile(Path path);

    /**
     * Lists all completed capture files in the storage directory.
     * @return A Flux of Paths for each completed file.
     */
    Flux<Path> listCompletedFiles();

    /**
     * Loads a specific completed capture file as a streamable resource.
     * @param filename The name of the file to load.
     * @return A Mono emitting the resource if found, or an empty Mono otherwise.
     */
    Mono<Resource> loadFileAsResource(String filename);

    /**
     * Loads a file as a resource, but waits for the file to have content before emitting the resource.
     * This is useful for streaming files that are being written to disk.
     * @param filename The name of the file to load.
     * @return A Mono emitting the resource once it has content, or an error if it times out.
     */
    Mono<Resource> loadFileAsResourceWhenReady(String filename);
}