package com.ggar.streamlink.service;

import reactor.core.publisher.Mono;

public interface LoggingService {
    Mono<Void> info(String message);
    Mono<Void> warn(String message);
    Mono<Void> error(String message);
    Mono<Void> error(String message, Throwable throwable);
    Mono<Void> debug(String message);
}
