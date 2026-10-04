package com.ggar.stvr.framework.cqrs;

import org.reactivestreams.Publisher;

public interface QueryHandler<Q extends Query<R>, R> {
    Publisher<R> handle(Q query);
}
