package com.ggar.stvr.streaming.implementation;

import com.ggar.stvr.streaming.entities.ActiveStreamSnapshot;
import com.ggar.stvr.streaming.entities.StreamKey;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * In-memory registry tracking all active ChannelStreamHub instances keyed by StreamKey.
 */
@Component
public class StreamHubRegistry {

    private final ConcurrentMap<StreamKey, ChannelStreamHub> hubs = new ConcurrentHashMap<>();

    public Optional<ChannelStreamHub> get(StreamKey key) {
        if (key == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(hubs.get(key));
    }

    public ChannelStreamHub computeIfAbsent(StreamKey key, Function<StreamKey, ChannelStreamHub> mappingFunction) {
        Objects.requireNonNull(key, "StreamKey cannot be null");
        Objects.requireNonNull(mappingFunction, "mappingFunction cannot be null");
        return hubs.computeIfAbsent(key, mappingFunction);
    }

    public void remove(StreamKey key) {
        if (key != null) {
            hubs.remove(key);
        }
    }

    public int activeHubCount() {
        return hubs.size();
    }

    public List<ActiveStreamSnapshot> allSnapshots() {
        return hubs.values().stream()
                .map(ChannelStreamHub::toSnapshot)
                .toList();
    }

    public void shutdownAll() {
        hubs.values().forEach(ChannelStreamHub::shutdown);
        hubs.clear();
    }
}
