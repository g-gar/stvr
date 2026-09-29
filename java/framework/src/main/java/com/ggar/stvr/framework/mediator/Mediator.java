package com.ggar.stvr.framework.mediator;

import com.ggar.stvr.framework.cqrs.Command;
import com.ggar.stvr.framework.cqrs.Query;
import org.reactivestreams.Publisher;

public interface Mediator {
    <R> Publisher<R> send(Command<R> command);

    <R> Publisher<R> send(Query<R> query);
}
