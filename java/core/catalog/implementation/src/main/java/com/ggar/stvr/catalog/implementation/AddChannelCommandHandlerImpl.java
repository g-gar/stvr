package com.ggar.stvr.catalog.implementation;

import com.ggar.stvr.catalog.api.AddChannelCommandHandler;
import com.ggar.stvr.catalog.api.exception.DuplicateChannelException;
import com.ggar.stvr.catalog.entities.ChannelId;
import com.ggar.stvr.catalog.persistence.ChannelRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Reactive CQRS handler implementation for UC-CAT-01: Add Channel.
 * Purely atomic catalog persistence and association.
 */
@Service
public class AddChannelCommandHandlerImpl implements AddChannelCommandHandler {

    private final ChannelRepository channelRepository;

    public AddChannelCommandHandlerImpl(ChannelRepository channelRepository) {
        this.channelRepository = Objects.requireNonNull(channelRepository, "ChannelRepository cannot be null");
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
                            .flatMap(existing -> channelRepository.associateExistingChannel(command.userId(), existing.id()))
                            .switchIfEmpty(Mono.defer(() -> {
                                ChannelDto newChannel = new ChannelDto(
                                        ChannelId.random(),
                                        command.name(),
                                        command.url(),
                                        command.platform(),
                                        false
                                );
                                return channelRepository.saveAndAssociate(command.userId(), newChannel);
                            }));
                });
    }
}
