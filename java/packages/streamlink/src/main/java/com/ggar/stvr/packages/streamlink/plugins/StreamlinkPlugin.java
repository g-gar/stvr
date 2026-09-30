package com.ggar.stvr.packages.streamlink.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import com.ggar.stvr.packages.streamlink.command.AbstractStreamlinkCommandBuilder;
import com.ggar.stvr.packages.streamlink.model.StreamlinkInspection;

import java.util.Locale;

/**
 * Strategy interface encapsulating all platform-specific logic for a Streamlink plugin:
 * URL matching, command builder creation, JSON output parsing, and error mapping.
 */
public interface StreamlinkPlugin {

    /**
     * Canonical Streamlink plugin identifier (e.g. "twitch", "kick", "youtube", "tiktok", "generic").
     */
    String getName();

    /**
     * Determines whether this plugin supports the given stream URL.
     * Each plugin defines its own supported URL patterns.
     *
     * @param url target stream URL
     * @return true if this plugin supports the URL
     */
    boolean supportsUrl(String url);

    /**
     * Determines whether this plugin matches the plugin name reported by Streamlink JSON output.
     *
     * @param pluginName plugin name from JSON
     * @return true if this plugin matches
     */
    boolean supportsPluginName(String pluginName);

    /**
     * Factory method creating a platform-specific command builder initialized with the given URL.
     *
     * @param url target stream URL
     * @return platform-specific command builder
     */
    AbstractStreamlinkCommandBuilder<?> createCommandBuilder(String url);

    /**
     * Parses and validates JSON inspection output for this plugin with the specified locale.
     *
     * @param rootNode Jackson root node representing Streamlink output
     * @param targetUrl original stream URL
     * @param locale user-selected locale for error formatting
     * @return validated StreamlinkInspection model
     */
    StreamlinkInspection parse(JsonNode rootNode, String targetUrl, Locale locale);

    /**
     * Handles and maps a plugin-specific error to a typed exception with the specified locale.
     *
     * @param rootNode Jackson root node representing error output
     * @param errorMsg error message from the "error" field
     * @param targetUrl original stream URL
     * @param locale user-selected locale for error formatting
     */
    void handleError(JsonNode rootNode, String errorMsg, String targetUrl, Locale locale);
}
