package com.ggar.streamlink.model.dto;

import com.fasterxml.jackson.annotation.JsonBackReference;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.Instant;

@Node
public class Capture {

    @Id @GeneratedValue
    private Long id;

    private String filename;
    private Instant startTime;

    @JsonBackReference
    @Relationship(type = "CAPTURE_OF", direction = Relationship.Direction.OUTGOING)
    private Streamer streamer;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
    public Streamer getStreamer() { return streamer; }
    public void setStreamer(Streamer streamer) { this.streamer = streamer; }
}
