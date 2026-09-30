# Streamlink Reactive SDK (`packages:streamlink`)

This document details the architectural design, technical rationale, and design decisions for the `packages:streamlink` library.

---

## 1. Motivation and Objectives

Streamlink is a powerful CLI utility for extracting video streams from multiple platforms (Twitch, Kick, YouTube, TikTok, etc.). However, in modern enterprise architectures and reactive microservices built with Java:
1. **Invoking raw CLI commands (`streamlink ...`) shifts unnecessary complexity onto developers:** Each platform provides its own arguments and flags (e.g., `--twitch-disable-ads`, `--twitch-low-latency`, `--twitch-supported-codecs`, `--kick-low-latency`). Forcing developers to remember exact strings leads to runtime errors that are tedious to debug.
2. **Hardcoding execution mechanisms couples the package to the runtime environment:** Binding the library directly to the host OS `ProcessBuilder` or to `docker exec` prevents its reuse in diverse contexts (remote workers, Kubernetes jobs, serverless functions, or fast mocks in unit tests).
3. **Output parsing must be robust, validated, and typed:** The `--json` output of Streamlink must undergo rigorous validation, and any platform anomalies or errors must be mapped to a clean hierarchy of typed exceptions.

---

## 2. Architectural Decisions

### 2.1 Expressive SDK with Dynamic Platform Resolution & Fluent Builders
Rather than requiring developers to manually specify the streaming platform or type raw text flags, `packages:streamlink` automatically resolves the platform directly from the URL (via official Streamlink URL regex patterns) and provides typed fluent builders:

- **Dynamic Entry Points (Agnostic to Platform):**
  ```java
  // Direct inspection and streaming via StreamlinkClient (zero command boilerplate)
  Mono<StreamlinkInspection> inspection = client.inspect("https://twitch.tv/ibai");
  Flux<byte[]> stream = client.stream("https://twitch.tv/ibai", "best");

  // Immediate immutable command creation
  StreamlinkCommand cmd = StreamlinkCommand.of("https://twitch.tv/ibai");
  StreamlinkCommand streamCmd = StreamlinkCommand.of("https://twitch.tv/ibai", "best");

  // Platform-specific options dynamically resolved from URL
  TwitchCommandBuilder twitch = StreamlinkCommand.fromUrl("https://twitch.tv/ibai");
  StreamlinkCommand customCmd = twitch
      .disableAds()
      .lowLatency()
      .supportedCodecs("h264", "av1")
      .apiHeader("Client-ID", "...")
      .build();

  // Or with explicit class type validation:
  TwitchCommandBuilder validated = StreamlinkCommand.fromUrl("https://twitch.tv/ibai", TwitchCommandBuilder.class);
  ```
- **Benefits:**
  - Automated platform detection from URL using the Strategy Pattern (`StreamlinkPluginRegistry`).
  - IDE-assisted discoverability for platform-specific flags without hardcoding platform static methods on `StreamlinkCommand`.
  - Pre-execution validation (checking valid URLs and required arguments before spawning any process).
  - Immutability: `StreamlinkCommand` instances are fully immutable once built.

### 2.2 Decoupled Execution via Dependency Injection (DI)
The `packages:streamlink` package **does not prescribe** where or how the Streamlink binary runs. Instead, it exposes a clean execution port interface:

```java
public interface CommandExecutor {
    Mono<CommandResult> execute(List<String> command);
    StreamlinkSession openSession(List<String> command);
    default Flux<byte[]> stream(List<String> command) {
        return openSession(command).data();
    }
}
```

- **Constructor Injection in `StreamlinkClient`:**
  ```java
  StreamlinkClient client = new StreamlinkClient(commandExecutor);
  ```
- **Interchangeable Execution Strategies:**
  - **Local Host (`LocalProcessExecutor`):** Runs the process on the local operating system using `ProcessBuilder` in a reactive, non-blocking manner.
  - **Docker Containers (`DockerCommandExecutor`):** Enables running `docker exec streamlink ...` without requiring `packages:streamlink` to depend at compile-time on `packages:docker`.
  - **Mocks / Stubs:** Allows ultra-fast, environment-agnostic unit testing without requiring Python or Streamlink installed on the host machine.

### 2.3 Interactive Streaming Sessions (`StreamlinkSession`)
Streamlink emits raw binary video data via `stdout` while outputting stream status, connection diagnostics, warnings, and stream termination signals (`[stream.hls][info] Stream ended`) on `stderr`. Draining only `stdout` without reading `stderr` risks operating system pipe buffer deadlocks.

To solve this, `packages:streamlink` provides `StreamlinkSession`:
```java
public interface StreamlinkSession extends AutoCloseable {
    Flux<byte[]> data();           // Concurrent stdout video byte stream
    Flux<String> logs();           // Real-time stderr diagnostic log stream
    Mono<CommandResult> result();  // Completion signal with exit code
    boolean isAlive();             // Process status check
    void cancel();                 // On-demand termination (SIGTERM -> SIGKILL)
}
```

- **Usage in Backend / Recording Services:**
  ```java
  StreamlinkSession session = client.openSession("https://twitch.tv/streamer", "best");

  // 1. Pipe video data to storage or media pipeline
  session.data()
      .doOnNext(fileWriter::write)
      .subscribe();

  // 2. Observe stderr in real time for stream closure or reconnects
  session.logs()
      .filter(line -> line.contains("Stream ended"))
      .subscribe(line -> notifier.streamEnded());

  // 3. Stop recording on demand (button click, quota reached, timeout)
  session.cancel();
  ```

### 2.4 Plugin-Specific Parsing via Strategy Pattern & Registry Factory
To prevent `StreamlinkJsonParser` from ballooning into a God Class full of platform-specific conditionals, the **Strategy Pattern** is applied:

```java
public interface StreamlinkPlugin {
    String getName();
    boolean supportsUrl(String url);
    boolean supportsPluginName(String pluginName);
    AbstractStreamlinkCommandBuilder<?> createCommandBuilder(String url);
    StreamlinkInspection parse(JsonNode rootNode, String targetUrl, Locale locale);
    void handleError(JsonNode rootNode, String errorMsg, String targetUrl, Locale locale);
}
```

- **`StreamlinkPluginRegistry` (Factory / Strategy Registry):**
  - Maintains available strategies (`TwitchPlugin`, `KickPlugin`, `YouTubePlugin`, `TikTokPlugin`) and a fallback strategy (`GenericPlugin`).
  - Supports dynamic registration of new strategies at runtime or via DI.
- **`StreamlinkJsonParser` (Lightweight Orchestrator):**
  - Deserializes the JSON tree via Jackson and resolves global CLI errors (`No playable streams found`, `No plugin can handle URL`).
  - Immediately delegates parsing, structural validation, and error handling to the resolved plugin strategy.
- **Encapsulation by Plugin (`plugins/<plugin_name>/`):**
  - Everything relating to a platform (command builder, parsing strategy, and typed exceptions) lives cohesively in its own subpackage.
  - Adding support for new platforms (e.g., Dailymotion, AfreecaTV) follows the **Open-Closed Principle (OCP)**: create a new subpackage under `plugins/` and register the strategy, leaving existing code untouched.

### 2.5 Robust Logging and Internationalization (i18n)
- **Logging via SLF4J:** High-level actions, process executions, latency measurements, and warning/error scenarios are recorded with structured debug and info logs.
- **Internationalization (i18n):** User-facing error messages and exceptions are internationalized using standard resource bundles (`messages_en.properties`, `messages_es.properties`). Callers can specify the desired `Locale` globally on `StreamlinkClient` or per invocation.

---

## 3. Package Structure in `java/packages/streamlink`

```text
com.ggar.stvr.packages.streamlink/
├── StreamlinkClient.java                     # Main reactive facade client
├── command/
│   ├── StreamlinkCommand.java               # Immutable command specification
│   └── AbstractStreamlinkCommandBuilder.java# Base builder for shared options (--http-header, proxy, etc.)
├── executor/
│   ├── CommandExecutor.java                 # Execution port / SPI
│   ├── CommandResult.java                   # Immutable record (exitCode, stdout, stderr)
│   └── LocalProcessExecutor.java            # Standard reactive local process runner
├── session/
│   ├── StreamlinkSession.java               # Streaming session port (data, logs, result, cancel)
│   └── LocalProcessStreamlinkSession.java   # Concurrent OS process implementation
├── i18n/
│   └── StreamlinkMessages.java              # Internationalization message resolver
├── model/
│   ├── StreamlinkInspection.java            # Top-level inspection model (--json)
│   ├── StreamlinkMetadata.java              # Channel / stream metadata
│   └── StreamlinkStreamDetails.java         # Technical details per quality/variant
├── parser/
│   └── StreamlinkJsonParser.java            # Lightweight JSON orchestrator
├── plugins/
│   ├── StreamlinkPluginParser.java          # Strategy interface for plugin parsing
│   ├── AbstractStreamlinkPluginParser.java  # Common structural validation template
│   ├── StreamlinkPluginParserRegistry.java  # Strategy factory / registry
│   ├── generic/
│   │   ├── GenericCommandBuilder.java       # Builder for generic URLs
│   │   └── GenericPluginParser.java         # Fallback parsing strategy
│   ├── twitch/
│   │   ├── TwitchCommandBuilder.java        # Typed Twitch builder
│   │   ├── TwitchPluginParser.java          # Twitch parsing strategy
│   │   └── TwitchStreamlinkException.java   # Typed Twitch exception
│   ├── kick/
│   │   ├── KickCommandBuilder.java          # Typed Kick builder
│   │   ├── KickPluginParser.java            # Kick parsing strategy
│   │   └── KickStreamlinkException.java     # Typed Kick exception
│   ├── youtube/
│   │   ├── YouTubeCommandBuilder.java       # Typed YouTube builder
│   │   ├── YouTubePluginParser.java         # YouTube parsing strategy
│   │   └── YouTubeStreamlinkException.java  # Typed YouTube exception
│   └── tiktok/
│       ├── TikTokCommandBuilder.java        # Typed TikTok builder
│       ├── TikTokPluginParser.java          # TikTok parsing strategy
│       └── TikTokStreamlinkException.java   # Typed TikTok exception
└── exception/
    ├── StreamlinkException.java             # Base unchecked exception
    ├── StreamlinkExecutionException.java    # CLI process failure (exit code != 0)
    ├── StreamlinkParseException.java        # Malformed JSON or structural error
    ├── StreamlinkNoStreamsException.java    # Offline channel or no streams available
    ├── StreamlinkPluginNotFoundException.java # Unsupported URL
    └── StreamlinkPluginException.java       # Base for plugin-specific errors
```
