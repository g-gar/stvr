package com.ggar.stvr.framework.plugin;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Host environment context provided to a plugin during its initialization.
 * Contains configuration properties and environment metadata.
 */
public record PluginContext(
        Map<String, String> properties,
        Map<String, Object> attributes
) {

    public PluginContext {
        properties = properties != null ? Map.copyOf(properties) : Collections.emptyMap();
        attributes = attributes != null ? Map.copyOf(attributes) : Collections.emptyMap();
    }

    public static PluginContext empty() {
        return new PluginContext(Collections.emptyMap(), Collections.emptyMap());
    }

    public static PluginContext of(Map<String, String> properties) {
        return new PluginContext(properties, Collections.emptyMap());
    }

    public Optional<String> getProperty(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        return Optional.ofNullable(properties.get(key));
    }

    public String getProperty(String key, String defaultValue) {
        return getProperty(key).orElse(defaultValue);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getAttribute(String key, Class<T> targetClass) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(targetClass, "targetClass cannot be null");
        Object val = attributes.get(key);
        if (val != null && targetClass.isInstance(val)) {
            return Optional.of((T) val);
        }
        return Optional.empty();
    }
}
