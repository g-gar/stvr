package com.ggar.stvr.catalog.entities;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

/**
 * Pure domain model representing a streaming channel in the catalog.
 * Completely decoupled from database annotations (Neo4j, JPA) and external frameworks.
 */
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class Channel {

    @EqualsAndHashCode.Include
    private final ChannelId id;
    private final ChannelUrl url;
    private final Platform platform;
    private final String slug;
    private final ChannelName name;

    public Channel(
            ChannelId id,
            ChannelUrl url,
            Platform platform,
            String slug,
            ChannelName name
    ) {
        this.id = Objects.requireNonNull(id, "ChannelId cannot be null");
        this.url = Objects.requireNonNull(url, "ChannelUrl cannot be null");
        this.platform = Objects.requireNonNull(platform, "Platform cannot be null");
        this.slug = slug != null ? slug : "";
        this.name = Objects.requireNonNull(name, "ChannelName cannot be null");
    }
}
