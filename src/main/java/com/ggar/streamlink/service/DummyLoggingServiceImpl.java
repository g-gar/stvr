package com.ggar.streamlink.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class DummyLoggingServiceImpl implements LoggingService {

    @Override
    public Mono<Void> info(String message) {
        return Mono.empty(); // Do nothing
    }

    @Override
    public Mono<Void> warn(String message) {
        return Mono.empty(); // Do nothing
    }

    @Override
    public Mono<Void> error(String message) {
        return Mono.empty(); // Do nothing
    }

    @Override
    public Mono<Void> error(String message, Throwable throwable) {
        return Mono.empty(); // Do nothing
    }

    @Override
    public Mono<Void> debug(String message) {
        return Mono.empty(); // Do nothing
    }
}
