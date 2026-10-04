package com.ggar.stvr.packages.streamlink.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ggar.stvr.packages.streamlink.exception.StreamlinkException;
import com.ggar.stvr.packages.streamlink.exception.StreamlinkNoStreamsException;
import com.ggar.stvr.packages.streamlink.exception.StreamlinkParseException;
import com.ggar.stvr.packages.streamlink.exception.StreamlinkPluginNotFoundException;
import com.ggar.stvr.packages.streamlink.i18n.StreamlinkMessages;
import com.ggar.stvr.packages.streamlink.model.StreamlinkStreamInfo;
import com.ggar.stvr.packages.streamlink.plugins.StreamlinkPlugin;
import com.ggar.stvr.packages.streamlink.plugins.StreamlinkPluginRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;
import java.util.Objects;

/**
 * High-level orchestrator for Streamlink JSON output parsing.
 * Delegates platform-specific parsing, validation, and error mapping
 * to pluggable {@link StreamlinkPlugin} strategies.
 */
@Slf4j
public class StreamlinkJsonParser {

    private final ObjectMapper objectMapper;
    private final StreamlinkPluginRegistry registry;

    public StreamlinkJsonParser() {
        this(new ObjectMapper(), StreamlinkPluginRegistry.defaultRegistry());
    }

    public StreamlinkJsonParser(ObjectMapper objectMapper) {
        this(objectMapper, StreamlinkPluginRegistry.defaultRegistry());
    }

    public StreamlinkJsonParser(ObjectMapper objectMapper, StreamlinkPluginRegistry registry) {
        this.objectMapper = Objects.requireNonNull(objectMapper, StreamlinkMessages.get("error.null_arg", "objectMapper"));
        this.registry = Objects.requireNonNull(registry, StreamlinkMessages.get("error.null_arg", "registry"));
    }

    /**
     * Parses and validates the JSON output produced by {@code streamlink --json <url>} using default English locale.
     *
     * @param rawJson JSON string from streamlink standard output
     * @param targetUrl original URL passed to Streamlink
     * @return validated StreamlinkStreamInfo
     */
    public StreamlinkStreamInfo parse(String rawJson, String targetUrl) {
        return parse(rawJson, targetUrl, Locale.ENGLISH);
    }

    /**
     * Parses and validates the JSON output with a specific target Locale for error message localization.
     *
     * @param rawJson JSON string from streamlink standard output
     * @param targetUrl original URL passed to Streamlink
     * @param locale target locale for localized exceptions
     * @return validated StreamlinkStreamInfo
     */
    public StreamlinkStreamInfo parse(String rawJson, String targetUrl, Locale locale) {
        Locale targetLocale = locale != null ? locale : Locale.ENGLISH;

        if (rawJson == null || rawJson.isBlank()) {
            log.warn("Attempted to parse empty or null Streamlink JSON output for URL: {}", targetUrl);
            throw new StreamlinkParseException(StreamlinkMessages.get("error.empty_output", targetLocale), null);
        }

        try {
            JsonNode rootNode = objectMapper.readTree(rawJson);
            String pluginName = extractPluginName(rootNode);

            StreamlinkPlugin strategy = registry.find(pluginName, targetUrl);
            log.debug("Resolved plugin strategy '{}' for plugin='{}', url='{}'", strategy.getName(), pluginName, targetUrl);

            // Handle errors reported in JSON
            if (rootNode.hasNonNull("error")) {
                String errorMsg = rootNode.get("error").asText();
                log.warn("Streamlink output contains error for URL '{}': {}", targetUrl, errorMsg);

                if (errorMsg.contains("No playable streams found") || errorMsg.contains("No streams found")) {
                    throw new StreamlinkNoStreamsException(targetUrl != null ? targetUrl : "", strategy.getName(), errorMsg, targetLocale);
                }
                if (errorMsg.contains("No plugin can handle URL")) {
                    throw new StreamlinkPluginNotFoundException(targetUrl != null ? targetUrl : "", errorMsg, targetLocale);
                }

                // Delegate platform-specific error handling to the matched strategy
                strategy.handleError(rootNode, errorMsg, targetUrl, targetLocale);
            }

            // Delegate structure validation and parsing to the strategy
            StreamlinkStreamInfo streamInfo = strategy.parse(rootNode, targetUrl, targetLocale);
            log.debug("Successfully parsed inspection for plugin='{}', id='{}', stream qualities={}",
                    streamInfo.plugin(),
                    streamInfo.metadata() != null ? streamInfo.metadata().id() : "unknown",
                    streamInfo.streams() != null ? streamInfo.streams().keySet() : "none");

            return streamInfo;
        } catch (StreamlinkException e) {
            throw e;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse raw Streamlink JSON for URL '{}': {}", targetUrl, e.getMessage());
            throw new StreamlinkParseException(StreamlinkMessages.get("error.parse_failed", targetLocale, e.getMessage()), e);
        } catch (Exception e) {
            log.error("Unexpected error parsing Streamlink output for URL '{}': {}", targetUrl, e.getMessage(), e);
            throw new StreamlinkParseException(StreamlinkMessages.get("error.parse_failed", targetLocale, e.getMessage()), e);
        }
    }

    private String extractPluginName(JsonNode rootNode) {
        if (rootNode != null && rootNode.hasNonNull("plugin") && !rootNode.get("plugin").asText().isBlank()) {
            return rootNode.get("plugin").asText().trim().toLowerCase();
        }
        return null;
    }
}
