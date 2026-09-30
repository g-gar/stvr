package com.ggar.stvr.catalog.implementation;

import com.ggar.stvr.catalog.api.AddChannelCommandHandler;
import com.ggar.stvr.catalog.api.exception.DuplicateChannelException;
import com.ggar.stvr.catalog.api.exception.UnsupportedPlatformException;
import com.ggar.stvr.catalog.api.inspector.ChannelInspector;
import com.ggar.stvr.catalog.api.inspector.ChannelInspectorFactory;
import com.ggar.stvr.catalog.entities.ChannelId;
import com.ggar.stvr.catalog.entities.ChannelName;
import com.ggar.stvr.catalog.entities.Platform;
import com.ggar.stvr.catalog.persistence.ChannelRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Optional;

/**
 * Reactive CQRS handler implementation for UC-CAT-01: Add Channel.
 */
@Service
public class AddChannelCommandHandlerImpl implements AddChannelCommandHandler {

    private final ChannelRepository channelRepository;
    private final ChannelInspectorFactory inspectorFactory;

    public AddChannelCommandHandlerImpl(
            ChannelRepository channelRepository,
            ChannelInspectorFactory inspectorFactory
    ) {
        this.channelRepository = Objects.requireNonNull(channelRepository, "ChannelRepository cannot be null");
        this.inspectorFactory = Objects.requireNonNull(inspectorFactory, "ChannelInspectorFactory cannot be null");
    }

    @Override
    public Mono<ChannelDto> handle(AddChannelCommand command) {
        Objects.requireNonNull(command, "AddChannelCommand cannot be null");

        // 1. Check if user already tracks this URL
        return channelRepository.existsUserChannel(command.userId(), command.url())
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        return Mono.error(new DuplicateChannelException(command.userId(), command.url()));
                    }

                    // 2. Check if channel already exists globally in the catalog
                    return channelRepository.findChannelByUrl(command.url())
                            .flatMap(existingChannel ->
                                    channelRepository.associateExistingChannel(command.userId(), existingChannel.id())
                            )
                            .switchIfEmpty(Mono.defer(() -> resolveAndCreateChannel(command)));
                });
    }

    private Mono<ChannelDto> resolveAndCreateChannel(AddChannelCommand command) {
        Optional<ChannelInspector> inspectorOpt = inspectorFactory.getInspector(command.url());

        if (inspectorOpt.isEmpty()) {
            return fallbackOrThrow(command);
        }

        ChannelInspector inspector = inspectorOpt.get();
        return inspector.inspect(command.url())
                .flatMap(inspectionResult -> {
                    ChannelName effectiveName = command.customName() != null
                            ? command.customName()
                            : inspectionResult.name();

                    ChannelDto newChannel = new ChannelDto(
                            ChannelId.random(),
                            effectiveName,
                            command.url(),
                            inspectionResult.platform(),
                            false,
                            inspectionResult.metadata()
                    );

                    return channelRepository.saveAndAssociate(command.userId(), newChannel);
                })
                .switchIfEmpty(Mono.defer(() -> fallbackOrThrow(command)))
                .onErrorResume(e -> fallbackOrThrow(command));
    }

    private Mono<ChannelDto> fallbackOrThrow(AddChannelCommand command) {
        if (command.customName() != null) {
            ChannelDto customChannel = new ChannelDto(
                    ChannelId.random(),
                    command.customName(),
                    command.url(),
                    Platform.CUSTOM,
                    false
            );
            return channelRepository.saveAndAssociate(command.userId(), customChannel);
        }
        return Mono.error(new UnsupportedPlatformException(command.url()));
    }
}
