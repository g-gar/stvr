package com.ggar.stvr.packages.streamlink.plugins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ggar.stvr.packages.streamlink.exception.StreamlinkNoStreamsException;
import com.ggar.stvr.packages.streamlink.exception.StreamlinkParseException;
import com.ggar.stvr.packages.streamlink.exception.StreamlinkPluginException;
import com.ggar.stvr.packages.streamlink.i18n.StreamlinkMessages;
import com.ggar.stvr.packages.streamlink.model.StreamlinkStreamInfo;
import com.ggar.stvr.packages.streamlink.model.StreamlinkStreamDetails;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Base template implementation for Streamlink plugin strategies.
 */
public abstract class AbstractStreamlinkPlugin implements StreamlinkPlugin {

    protected final ObjectMapper objectMapper;

    protected AbstractStreamlinkPlugin() {
        this(new ObjectMapper());
    }

    protected AbstractStreamlinkPlugin(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper,
                StreamlinkMessages.get("error.null_arg", "objectMapper"));
    }

    @Override
    public boolean supportsPluginName(String pluginName) {
        return getName().equalsIgnoreCase(pluginName);
    }

    @Override
    public StreamlinkStreamInfo parse(JsonNode rootNode, String targetUrl, Locale locale) {
        validateStructure(rootNode, locale);

        try {
            StreamlinkStreamInfo streamInfo = objectMapper.treeToValue(rootNode, StreamlinkStreamInfo.class);
            validateStreams(streamInfo, targetUrl, locale);
            return streamInfo;
        } catch (StreamlinkNoStreamsException | StreamlinkParseException e) {
            throw e;
        } catch (JsonProcessingException e) {
            throw new StreamlinkParseException(StreamlinkMessages.get("error.parse_failed", locale, e.getMessage()), e);
        }
    }

    @Override
    public void handleError(JsonNode rootNode, String errorMsg, String targetUrl, Locale locale) {
        throw new StreamlinkPluginException(getName(), errorMsg, locale);
    }

    protected void validateStructure(JsonNode rootNode, Locale locale) {
        if (!rootNode.hasNonNull("plugin") || rootNode.get("plugin").asText().isBlank()) {
            throw new StreamlinkParseException(StreamlinkMessages.get("error.missing_plugin", locale), null);
        }
        if (!rootNode.hasNonNull("metadata")) {
            throw new StreamlinkParseException(StreamlinkMessages.get("error.missing_metadata", locale), null);
        }
    }

    protected void validateStreams(StreamlinkStreamInfo streamInfo, String targetUrl, Locale locale) {
        if (!streamInfo.hasStreams()) {
            throw new StreamlinkNoStreamsException(targetUrl != null ? targetUrl : "", StreamlinkMessages.get("error.no_qualities", locale), locale);
        }

        for (Map.Entry<String, StreamlinkStreamDetails> entry : streamInfo.streams().entrySet()) {
            StreamlinkStreamDetails details = entry.getValue();
            if (details == null || (isBlank(details.url()) && isBlank(details.master()))) {
                throw new StreamlinkParseException(
                        StreamlinkMessages.get("error.invalid_stream_url", locale, entry.getKey()),
                        null
                );
            }
        }
    }

    protected boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
