package com.ggar.stvr.catalog.api.inspector;

import com.ggar.stvr.catalog.entities.ChannelUrl;

import java.util.Optional;

/**
 * Factory and registry for resolving the appropriate ChannelInspector for a given stream URL.
 */
public interface ChannelInspectorFactory {

    /**
     * Returns the appropriate ChannelInspector capable of handling the specified URL.
     *
     * @param url The channel URL to inspect.
     * @return An Optional containing the matching inspector, or empty if unsupported.
     */
    Optional<ChannelInspector> getInspector(ChannelUrl url);
}
