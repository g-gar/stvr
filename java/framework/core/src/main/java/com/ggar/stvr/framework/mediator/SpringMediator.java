package com.ggar.stvr.framework.mediator;

import com.ggar.stvr.framework.cqrs.Command;
import com.ggar.stvr.framework.cqrs.CommandHandler;
import com.ggar.stvr.framework.cqrs.Query;
import com.ggar.stvr.framework.cqrs.QueryHandler;
import org.reactivestreams.Publisher;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import reactor.core.publisher.Mono;

import java.util.Arrays;

public class SpringMediator implements Mediator {

    private final ApplicationContext applicationContext;

    public SpringMediator(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> Publisher<R> send(Command<R> command) {
        String[] beanNames = Arrays.stream(applicationContext.getBeanNamesForType(CommandHandler.class))
                .filter(name -> {
                    Class<?> type = applicationContext.getType(name);
                    if (type == null) return false;
                    ResolvableType handlerType = ResolvableType.forClass(type).as(CommandHandler.class);
                    return handlerType.getGeneric(0).isAssignableFrom(command.getClass());
                })
                .toArray(String[]::new);

        if (beanNames.length == 0) {
            return Mono.error(new IllegalStateException(
                    "No CommandHandler found for command: " + command.getClass().getName()));
        }
        if (beanNames.length > 1) {
            return Mono.error(new IllegalStateException("Multiple CommandHandlers found for command: "
                    + command.getClass().getName()));
        }

        CommandHandler<Command<R>, R> handler =
                (CommandHandler<Command<R>, R>) applicationContext.getBean(beanNames[0]);
        return handler.handle(command);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> Publisher<R> send(Query<R> query) {
        String[] beanNames = Arrays.stream(applicationContext.getBeanNamesForType(QueryHandler.class))
                .filter(name -> {
                    Class<?> type = applicationContext.getType(name);
                    if (type == null) return false;
                    ResolvableType handlerType = ResolvableType.forClass(type).as(QueryHandler.class);
                    return handlerType.getGeneric(0).isAssignableFrom(query.getClass());
                })
                .toArray(String[]::new);

        if (beanNames.length == 0) {
            return Mono.error(new IllegalStateException(
                    "No QueryHandler found for query: " + query.getClass().getName()));
        }
        if (beanNames.length > 1) {
            return Mono.error(new IllegalStateException("Multiple QueryHandlers found for query: "
                    + query.getClass().getName()));
        }

        QueryHandler<Query<R>, R> handler = (QueryHandler<Query<R>, R>) applicationContext.getBean(beanNames[0]);
        return handler.handle(query);
    }
}
