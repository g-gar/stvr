package com.ggar.streamlink.service;

import com.ggar.streamlink.mapper.PlatformMapper;
import com.ggar.streamlink.model.dto.NewPlatformInput;
import com.ggar.streamlink.model.dto.NewStreamerInput;
import com.ggar.streamlink.model.dto.Platform;
import com.ggar.streamlink.model.dto.PlatformDTO;
import com.ggar.streamlink.model.dto.Streamer;
import com.ggar.streamlink.repository.PlatformRepository;
import com.ggar.streamlink.repository.StreamerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class Neo4jStreamerDataService implements StreamerDataService {

    private final PlatformRepository platformRepository;
    private final StreamerRepository streamerRepository;
    private final PlatformMapper platformMapper;

    @Override
    public Flux<PlatformDTO> getPlatforms() {
        return platformRepository.findAll()
            .map(platformMapper::toDto)
            .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<Streamer> getStreamersByPlatform(String platformId) {
        return streamerRepository.findByPlatform_Id(Long.parseLong(platformId))
            .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Streamer> addStreamer(NewStreamerInput input) {
        return platformRepository.findById(Long.parseLong(input.platformId()))
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Platform not found with id: " + input.platformId())))
            .flatMap(platform -> {
                Streamer newStreamer = new Streamer();
                newStreamer.setName(input.name());
                newStreamer.setPlatform(platform);
                return streamerRepository.save(newStreamer);
            }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<PlatformDTO> addPlatform(NewPlatformInput input) {
        Platform platform = new Platform();
        platform.setName(input.name());
        platform.setUrl(input.url());
        return platformRepository.save(platform)
            .map(platformMapper::toDto)
            .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Streamer> getStreamerById(String id) {
        return streamerRepository.findById(Long.parseLong(id))
            .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<PlatformDTO> getPlatformById(String id) {
        return platformRepository.findById(Long.parseLong(id))
            .map(platformMapper::toDto)
            .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Streamer> configureAutoCapture(String id, boolean enabled, String quality) {
        return streamerRepository.findById(Long.parseLong(id))
            .flatMap(streamer -> {
                streamer.setAutoCapture(enabled);
                streamer.setAutoCaptureQuality(quality);
                return streamerRepository.save(streamer);
            })
            .subscribeOn(Schedulers.boundedElastic());
    }
}
