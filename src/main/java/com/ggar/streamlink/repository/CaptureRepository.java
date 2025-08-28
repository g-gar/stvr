package com.ggar.streamlink.repository;

import com.ggar.streamlink.model.dto.Capture;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CaptureRepository extends ReactiveNeo4jRepository<Capture, Long> {
}
