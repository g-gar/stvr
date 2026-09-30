package com.ggar.stvr.test.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * In-memory test event bus that captures emitted domain events for test assertions.
 */
public class CapturingEventBus {

    private final List<Object> events = new CopyOnWriteArrayList<>();

    public void publish(Object event) {
        if (event != null) {
            events.add(event);
        }
    }

    public List<Object> getEvents() {
        return Collections.unmodifiableList(events);
    }

    public <T> List<T> getEventsOfType(Class<T> type) {
        return events.stream()
                .filter(type::isInstance)
                .map(type::cast)
                .collect(Collectors.toList());
    }

    public boolean hasEventOfType(Class<?> type) {
        return events.stream().anyMatch(type::isInstance);
    }

    public void clear() {
        events.clear();
    }
}
