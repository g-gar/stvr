package com.ggar.stvr.catalog.implementation;

import com.ggar.stvr.catalog.api.RemoveChannelCommandHandler;
import com.ggar.stvr.catalog.api.exception.ChannelNotFoundException;
import com.ggar.stvr.catalog.persistence.ChannelRepository;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Implementation of the command handler for removing a channel from a user's catalog.
 * Invokes ChannelRepository to delete the user-channel relationship or emits ChannelNotFoundException.
 */
@Service
public class RemoveChannelCommandHandlerImpl implements RemoveChannelCommandHandler {

    private final ChannelRepository channelRepository;

    public RemoveChannelCommandHandlerImpl(ChannelRepository channelRepository) {
        this.channelRepository = Objects.requireNonNull(channelRepository, "ChannelRepository cannot be null");
    }

    @Override
    public Publisher<Void> handle(RemoveChannelCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");
        return channelRepository.deleteUserChannel(command.userId(), command.channelId())
                .switchIfEmpty(Mono.error(() -> new ChannelNotFoundException(command.channelId(), command.userId())))
                .then();
    }
}
