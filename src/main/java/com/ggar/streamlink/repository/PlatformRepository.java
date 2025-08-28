package com.ggar.streamlink.repository;

import com.ggar.streamlink.model.dto.Platform;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlatformRepository extends ReactiveNeo4jRepository<Platform, Long> {
}