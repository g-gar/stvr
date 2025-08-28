package com.ggar.streamlink.service.parser;

import com.ggar.streamlink.model.StreamOutput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class StreamDataParserImpl implements StreamDataParser {

    // Example: [download]  50.0% of 1.00GiB at 5.00MiB/s ETA 00:02:00
    private static final Pattern PROGRESS_PATTERN = Pattern.compile("\\[download\\]\\s+([\\d.]+)%\\s+of\\s+(.*?)\\s+at\\s+(.*?)\\s+ETA\\s+(.*)");
    // Example: [plugin.twitch][info] Found matching plugin twitch for URL twitch.tv/user
    private static final Pattern MESSAGE_PATTERN = Pattern.compile("\\[([\\w.]+)\\]\\[(\\w+)\\]\\s+(.*)");

    // Example: error: No plugin can handle URL: ...
    private static final Pattern SIMPLE_ERROR_PATTERN = Pattern.compile("error:\\s+(.*)");

    @Override
    public Flux<StreamOutput> parse(InputStream inputStream) {
        return Flux.fromStream(() ->
                new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines()
            )
            .subscribeOn(Schedulers.boundedElastic())
            .doOnNext(line -> log.debug("stderr: {}", line))
            .map(this::parseLine);
    }

    private StreamOutput parseLine(String line) {
        // Ignore messages that indicate a normal stream end, even if they appear on stderr.

        String lowerCaseLine = line.toLowerCase();
        if (lowerCaseLine.contains("ended") || lowerCaseLine.contains("closing")) {
            return new StreamOutput.GenericOutput(line);
        }

        Matcher progressMatcher = PROGRESS_PATTERN.matcher(line);
        if (progressMatcher.matches()) {
            double percentage = Double.parseDouble(progressMatcher.group(1));
            return new StreamOutput.ProgressOutput(
                percentage,
                progressMatcher.group(2).trim(), // size
                progressMatcher.group(3).trim(), // speed
                progressMatcher.group(4).trim(), // eta
                line
            );
        }

        Matcher messageMatcher = MESSAGE_PATTERN.matcher(line);
        if (messageMatcher.matches()) {
            String component = messageMatcher.group(1);
            String level = messageMatcher.group(2);
            String message = messageMatcher.group(3);
            return "error".equalsIgnoreCase(level)
                ? new StreamOutput.ErrorOutput(component, level, message, line)
                : new StreamOutput.PluginOutput(component, level, message, line);
        }

        Matcher simpleErrorMatcher = SIMPLE_ERROR_PATTERN.matcher(line);
        if (simpleErrorMatcher.matches()) {
            return new StreamOutput.ErrorOutput("cli", "error", simpleErrorMatcher.group(1), line);
        }

        return new StreamOutput.GenericOutput(line);
    }
}
