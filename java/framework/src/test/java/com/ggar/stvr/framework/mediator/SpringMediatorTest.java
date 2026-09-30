package com.ggar.stvr.framework.mediator;

import com.ggar.stvr.framework.cqrs.Command;
import com.ggar.stvr.framework.cqrs.CommandHandler;
import com.ggar.stvr.framework.cqrs.Query;
import com.ggar.stvr.framework.cqrs.QueryHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.context.support.GenericApplicationContext;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class SpringMediatorTest {

    private GenericApplicationContext context;
    private SpringMediator mediator;

    record SampleCommand(String value) implements Command<String> {}
    record SampleQuery(String queryParam) implements Query<Integer> {}

    static class SampleCommandHandler implements CommandHandler<SampleCommand, String> {
        @Override
        public Publisher<String> handle(SampleCommand command) {
            return Mono.just("Handled: " + command.value());
        }
    }

    static class SampleQueryHandler implements QueryHandler<SampleQuery, Integer> {
        @Override
        public Publisher<Integer> handle(SampleQuery query) {
            return Mono.just(query.queryParam().length());
        }
    }

    @BeforeEach
    void setUp() {
        context = new GenericApplicationContext();
    }

    @Test
    @DisplayName("Should successfully route command to its registered CommandHandler")
    void shouldRouteCommandToHandler() {
        context.registerBean("sampleCommandHandler", SampleCommandHandler.class, SampleCommandHandler::new);
        context.refresh();
        mediator = new SpringMediator(context);

        StepVerifier.create(mediator.send(new SampleCommand("Antigravity")))
                .expectNext("Handled: Antigravity")
                .verifyComplete();
    }

    @Test
    @DisplayName("Should successfully route query to its registered QueryHandler")
    void shouldRouteQueryToHandler() {
        context.registerBean("sampleQueryHandler", SampleQueryHandler.class, SampleQueryHandler::new);
        context.refresh();
        mediator = new SpringMediator(context);

        StepVerifier.create(mediator.send(new SampleQuery("Test")))
                .expectNext(4)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return error Mono when no CommandHandler is registered")
    void shouldReturnErrorWhenNoCommandHandlerFound() {
        context.refresh();
        mediator = new SpringMediator(context);

        StepVerifier.create(mediator.send(new SampleCommand("Unknown")))
                .expectErrorMatches(throwable -> throwable instanceof IllegalStateException
                        && throwable.getMessage().contains("No CommandHandler found"))
                .verify();
    }

    @Test
    @DisplayName("Should return error Mono when multiple CommandHandlers are registered")
    void shouldReturnErrorWhenMultipleCommandHandlersFound() {
        context.registerBean("handler1", SampleCommandHandler.class, SampleCommandHandler::new);
        context.registerBean("handler2", SampleCommandHandler.class, SampleCommandHandler::new);
        context.refresh();
        mediator = new SpringMediator(context);

        StepVerifier.create(mediator.send(new SampleCommand("Duplicate")))
                .expectErrorMatches(throwable -> throwable instanceof IllegalStateException
                        && throwable.getMessage().contains("Multiple CommandHandlers found"))
                .verify();
    }

    @Test
    @DisplayName("Should return error Mono when no QueryHandler is registered")
    void shouldReturnErrorWhenNoQueryHandlerFound() {
        context.refresh();
        mediator = new SpringMediator(context);

        StepVerifier.create(mediator.send(new SampleQuery("Unknown")))
                .expectErrorMatches(throwable -> throwable instanceof IllegalStateException
                        && throwable.getMessage().contains("No QueryHandler found"))
                .verify();
    }

    @Test
    @DisplayName("Should return error Mono when multiple QueryHandlers are registered")
    void shouldReturnErrorWhenMultipleQueryHandlersFound() {
        context.registerBean("qhandler1", SampleQueryHandler.class, SampleQueryHandler::new);
        context.registerBean("qhandler2", SampleQueryHandler.class, SampleQueryHandler::new);
        context.refresh();
        mediator = new SpringMediator(context);

        StepVerifier.create(mediator.send(new SampleQuery("Duplicate")))
                .expectErrorMatches(throwable -> throwable instanceof IllegalStateException
                        && throwable.getMessage().contains("Multiple QueryHandlers found"))
                .verify();
    }
}
