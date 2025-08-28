package com.ggar.streamlink.repository;

import com.ggar.streamlink.model.dto.Streamer;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface StreamerRepository extends ReactiveNeo4jRepository<Streamer, Long> {

    Flux<Streamer> findByPlatform_Id(Long platformId);

    @Query("MATCH (s:Streamer)-[r:STREAMS_ON]->(p:Platform) WHERE replace(p.url, '{streamer_name}', s.name) = $url RETURN s, r, p")
    Mono<Streamer> findByUrl(String url);
}
