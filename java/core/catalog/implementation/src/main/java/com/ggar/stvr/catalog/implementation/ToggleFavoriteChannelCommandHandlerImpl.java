package com.ggar.stvr.catalog.implementation;

import com.ggar.stvr.catalog.api.exception.ChannelNotFoundException;
import com.ggar.stvr.catalog.api.ToggleFavoriteChannelCommandHandler;
import com.ggar.stvr.catalog.persistence.ChannelRepository;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Implementation of the command handler for toggling a channel's favorite status.
 * Updates favorite status via ChannelRepository or emits ChannelNotFoundException if channel is not found.
 */
@Service
public class ToggleFavoriteChannelCommandHandlerImpl implements ToggleFavoriteChannelCommandHandler {

    private final ChannelRepository channelRepository;

    public ToggleFavoriteChannelCommandHandlerImpl(ChannelRepository channelRepository) {
        this.channelRepository = Objects.requireNonNull(channelRepository, "ChannelRepository cannot be null");
    }

    @Override
    public Publisher<ToggleFavoriteResponseDto> handle(ToggleFavoriteChannelCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");
        return channelRepository.toggleFavorite(command.userId(), command.channelId())
                .map(newStatus -> new ToggleFavoriteResponseDto(command.channelId(), newStatus))
                .switchIfEmpty(Mono.error(() -> new ChannelNotFoundException(command.channelId(), command.userId())));
    }
}
