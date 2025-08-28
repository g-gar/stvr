package com.ggar.streamlink.model.dto;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.List;

@Node
public class Platform {

    @Id @GeneratedValue
    private Long id;

    private String name;
    private String url;

    @Relationship(type = "STREAMS_ON", direction = Relationship.Direction.OUTGOING)
    private List<Streamer> streamers;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public List<Streamer> getStreamers() { return streamers; }
    public void setStreamers(List<Streamer> streamers) { this.streamers = streamers; }
}