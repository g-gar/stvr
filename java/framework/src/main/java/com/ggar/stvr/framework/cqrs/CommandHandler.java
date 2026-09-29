package com.ggar.stvr.framework.cqrs;

import org.reactivestreams.Publisher;

public interface CommandHandler<C extends Command<R>, R> {
    Publisher<R> handle(C command);
}
