package com.ggar.streamlink.model.dto;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.List;

@Node
public class Streamer {

    @Id @GeneratedValue
    private Long id;

    private String name;

    @Relationship(type = "STREAMS_ON", direction = Relationship.Direction.OUTGOING)
    private Platform platform;

    @JsonManagedReference
    @Relationship(type = "HAS_CAPTURE", direction = Relationship.Direction.OUTGOING)
    private List<Capture> captures;

    private boolean autoCapture = false;

    private String autoCaptureQuality;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUrl() {
        if (this.platform != null && this.platform.getUrl() != null) {
            return this.platform.getUrl().replace("{streamer_name}", this.name);
        }
        return null;
    }
    public Platform getPlatform() { return platform; }
    public void setPlatform(Platform platform) { this.platform = platform; }
    public List<Capture> getCaptures() { return captures; }
    public void setCaptures(List<Capture> captures) { this.captures = captures; }
    public boolean isAutoCapture() { return autoCapture; }
    public void setAutoCapture(boolean autoCapture) { this.autoCapture = autoCapture; }
    public String getAutoCaptureQuality() { return autoCaptureQuality; }
    public void setAutoCaptureQuality(String autoCaptureQuality) { this.autoCaptureQuality = autoCaptureQuality; }
}
