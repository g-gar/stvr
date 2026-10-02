package com.ggar.stvr.packages.streamlink;

import com.ggar.stvr.packages.streamlink.command.StreamlinkCommand;
import com.ggar.stvr.packages.streamlink.exception.StreamlinkExecutionException;
import com.ggar.stvr.packages.streamlink.executor.CommandExecutor;
import com.ggar.stvr.packages.streamlink.executor.CommandResult;
import com.ggar.stvr.packages.streamlink.i18n.StreamlinkMessages;
import com.ggar.stvr.packages.streamlink.model.StreamlinkStreamInfo;
import com.ggar.stvr.packages.streamlink.parser.StreamlinkJsonParser;
import com.ggar.stvr.packages.streamlink.session.StreamlinkSession;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Locale;
import java.util.Objects;

/**
 * High-level reactive client for Streamlink CLI operations.
 * Decoupled from execution mechanics via {@link CommandExecutor} dependency injection.
 * Provides internationalized error resolution (i18n) and structured logging.
 */
@Slf4j
public class StreamlinkClient {

    private final CommandExecutor executor;
    private final StreamlinkJsonParser parser;

    @Getter
    private final Locale defaultLocale;

    public StreamlinkClient(CommandExecutor executor) {
        this(executor, new StreamlinkJsonParser(), Locale.ENGLISH);
    }

    public StreamlinkClient(CommandExecutor executor, Locale defaultLocale) {
        this(executor, new StreamlinkJsonParser(), defaultLocale);
    }

    public StreamlinkClient(CommandExecutor executor, StreamlinkJsonParser parser) {
        this(executor, parser, Locale.ENGLISH);
    }

    public StreamlinkClient(CommandExecutor executor, StreamlinkJsonParser parser, Locale defaultLocale) {
        this.executor = Objects.requireNonNull(executor, StreamlinkMessages.get("error.null_arg", "executor"));
        this.parser = Objects.requireNonNull(parser, StreamlinkMessages.get("error.null_arg", "parser"));
        this.defaultLocale = defaultLocale != null ? defaultLocale : Locale.ENGLISH;
    }

    /**
     * Creates a new StreamlinkClient configured with a different default locale.
     *
     * @param locale target locale for error messages
     * @return new StreamlinkClient instance sharing the same executor and parser
     */
    public StreamlinkClient withLocale(Locale locale) {
        return new StreamlinkClient(this.executor, this.parser, locale);
    }

    /**
     * Inspects a channel/stream URL by running Streamlink in JSON mode using the client's default locale.
     * The platform is automatically resolved from the URL.
     *
     * @param url target stream URL
     * @return Mono emitting the parsed StreamlinkStreamInfo
     */
    public Mono<StreamlinkStreamInfo> inspect(String url) {
        return inspect(StreamlinkCommand.of(url), this.defaultLocale);
    }

    /**
     * Inspects a channel/stream URL with a specific locale chosen for this invocation.
     * The platform is automatically resolved from the URL.
     *
     * @param url target stream URL
     * @param locale locale for translating error messages and exceptions
     * @return Mono emitting the parsed StreamlinkStreamInfo
     */
    public Mono<StreamlinkStreamInfo> inspect(String url, Locale locale) {
        return inspect(StreamlinkCommand.of(url), locale);
    }

    /**
     * Inspects a channel/stream URL by running Streamlink in JSON mode using the client's default locale.
     *
     * @param command command specification (JSON flag will be enabled automatically)
     * @return Mono emitting the parsed StreamlinkStreamInfo
     */
    public Mono<StreamlinkStreamInfo> inspect(StreamlinkCommand command) {
        return inspect(command, this.defaultLocale);
    }


    /**
     * Inspects a channel/stream URL with a specific locale chosen for this invocation.
     *
     * @param command command specification (JSON flag will be enabled automatically)
     * @param locale locale for translating error messages and exceptions
     * @return Mono emitting the parsed StreamlinkStreamInfo
     */
    public Mono<StreamlinkStreamInfo> inspect(StreamlinkCommand command, Locale locale) {
        Locale targetLocale = locale != null ? locale : this.defaultLocale;
        Objects.requireNonNull(command, StreamlinkMessages.get("error.null_arg", targetLocale, "command"));

        StreamlinkCommand jsonCommand = command.isJson() ? command : command.toBuilder().json(true).build();
        log.info("Inspecting stream URL '{}' with locale='{}'", jsonCommand.getUrl(), targetLocale);

        return executor.execute(jsonCommand.toArgs())
                .flatMap(result -> {
                    String output = result.stdout() != null && !result.stdout().isBlank() ? result.stdout() : result.stderr();

                    if (!result.isSuccess()) {
                        log.warn("Streamlink inspection command failed with exitCode={} for URL '{}'", result.exitCode(), jsonCommand.getUrl());
                        if (output != null && output.trim().startsWith("{") && output.trim().endsWith("}")) {
                            // Try parsing structured error from Streamlink JSON
                            return Mono.fromCallable(() -> parser.parse(output, jsonCommand.getUrl(), targetLocale));
                        }
                        return Mono.error(new StreamlinkExecutionException(result.exitCode(), result.stdout(), result.stderr(), targetLocale));
                    }

                    return Mono.fromCallable(() -> {
                        StreamlinkStreamInfo streamInfo = parser.parse(result.stdout(), jsonCommand.getUrl(), targetLocale);
                        log.info("Inspection resolved successfully: plugin='{}', author='{}', qualities={}",
                                streamInfo.plugin(),
                                streamInfo.metadata() != null ? streamInfo.metadata().author() : "unknown",
                                streamInfo.streams() != null ? streamInfo.streams().keySet() : "none");
                        return streamInfo;
                    });
                });
    }

    /**
     * Opens an active streaming session for the specified URL and quality using default locale.
     * The platform is automatically resolved from the URL.
     *
     * @param url target stream URL
     * @param quality stream quality (e.g. "best", "1080p60")
     * @return active StreamlinkSession providing data(), logs(), result(), and cancel()
     */
    public StreamlinkSession openSession(String url, String quality) {
        return openSession(StreamlinkCommand.of(url, quality), this.defaultLocale);
    }

    /**
     * Opens an active streaming session for the specified URL and quality with specific locale.
     * The platform is automatically resolved from the URL.
     *
     * @param url target stream URL
     * @param quality stream quality (e.g. "best", "1080p60")
     * @param locale target locale for invocation
     * @return active StreamlinkSession providing data(), logs(), result(), and cancel()
     */
    public StreamlinkSession openSession(String url, String quality, Locale locale) {
        return openSession(StreamlinkCommand.of(url, quality), locale);
    }

    /**
     * Opens an active streaming session for the specified command using default locale.
     *
     * @param command command specification
     * @return active StreamlinkSession providing data(), logs(), result(), and cancel()
     */
    public StreamlinkSession openSession(StreamlinkCommand command) {
        return openSession(command, this.defaultLocale);
    }

    /**
     * Opens an active streaming session for the specified command with specific locale.
     *
     * @param command command specification
     * @param locale target locale for invocation
     * @return active StreamlinkSession providing data(), logs(), result(), and cancel()
     */
    public StreamlinkSession openSession(StreamlinkCommand command, Locale locale) {
        Locale targetLocale = locale != null ? locale : this.defaultLocale;
        Objects.requireNonNull(command, StreamlinkMessages.get("error.null_arg", targetLocale, "command"));

        StreamlinkCommand streamCommand = command.isStdout() ? command : command.toBuilder().stdout(true).build();
        log.info("Opening StreamlinkSession for URL '{}' with quality='{}', locale='{}'",
                streamCommand.getUrl(), streamCommand.getQuality(), targetLocale);

        return executor.openSession(streamCommand.toArgs());
    }

    /**
     * Streams video bytes from the specified URL and quality to a reactive Flux using default locale.
     * The platform is automatically resolved from the URL.
     *
     * @param url target stream URL
     * @param quality stream quality (e.g. "best", "1080p60")
     * @return Flux of byte array chunks
     */
    public Flux<byte[]> stream(String url, String quality) {
        return openSession(url, quality).data();
    }

    /**
     * Streams video bytes from the specified URL and quality to a reactive Flux with specific locale for logging.
     * The platform is automatically resolved from the URL.
     *
     * @param url target stream URL
     * @param quality stream quality (e.g. "best", "1080p60")
     * @param locale target locale for invocation
     * @return Flux of byte array chunks
     */
    public Flux<byte[]> stream(String url, String quality, Locale locale) {
        return openSession(url, quality, locale).data();
    }

    /**
     * Streams video bytes from the specified command to a reactive Flux using default locale.
     *
     * @param command command specification (stdout flag will be enabled automatically)
     * @return Flux of byte array chunks
     */
    public Flux<byte[]> stream(StreamlinkCommand command) {
        return openSession(command).data();
    }

    /**
     * Streams video bytes from the specified command to a reactive Flux with specific locale for logging.
     *
     * @param command command specification (stdout flag will be enabled automatically)
     * @param locale target locale for invocation
     * @return Flux of byte array chunks
     */
    public Flux<byte[]> stream(StreamlinkCommand command, Locale locale) {
        return openSession(command, locale).data();
    }

    /**
     * Executes the command returning the raw {@link CommandResult}.
     *
     * @param command command specification
     * @return Mono emitting the CommandResult
     */
    public Mono<CommandResult> executeRaw(StreamlinkCommand command) {
        Objects.requireNonNull(command, StreamlinkMessages.get("error.null_arg", this.defaultLocale, "command"));
        log.debug("Executing raw command for URL: {}", command.getUrl());
        return executor.execute(command.toArgs());
    }
}
