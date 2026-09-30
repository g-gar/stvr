package com.ggar.stvr.packages.streamlink.plugins.generic;

import com.fasterxml.jackson.databind.JsonNode;
import com.ggar.stvr.packages.streamlink.command.AbstractStreamlinkCommandBuilder;
import com.ggar.stvr.packages.streamlink.exception.StreamlinkPluginException;
import com.ggar.stvr.packages.streamlink.plugins.AbstractStreamlinkPlugin;

import java.util.Locale;

/**
 * Fallback / generic strategy implementation for non-specialized Streamlink plugins.
 */
public class GenericPlugin extends AbstractStreamlinkPlugin {

    @Override
    public String getName() {
        return "generic";
    }

    @Override
    public boolean supportsUrl(String url) {
        return true;
    }

    @Override
    public boolean supportsPluginName(String pluginName) {
        return true;
    }

    @Override
    public AbstractStreamlinkCommandBuilder<?> createCommandBuilder(String url) {
        return new GenericCommandBuilder().url(url);
    }

    @Override
    public void handleError(JsonNode rootNode, String errorMsg, String targetUrl, Locale locale) {
        String plugin = rootNode != null && rootNode.hasNonNull("plugin") ? rootNode.get("plugin").asText() : "unknown";
        throw new StreamlinkPluginException(plugin, errorMsg, locale);
    }
}
