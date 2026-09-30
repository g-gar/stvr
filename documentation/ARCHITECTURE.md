# STVR (Stream Television & Viewer Recorder)
## Comprehensive Architecture & Technical Design Specification

---

## 1. Executive Summary & Vision

**STVR** is a modern, lightweight, self-hosted streaming hub and recorder designed to eliminate the friction of terminal commands and the heavy overhead of bloated, script-saturated streaming websites. 

### Core Mission
1. **Lightweight Channel & Stream Hub:** A comfortable, distraction-free environment to organize favorite streamers and channels across multiple platforms (e.g., Twitch, YouTube, Kick, and custom URLs).
2. **Instant Live Preview & Zapping:** Watch live broadcasts in a native, high-performance HTML5 player with television-like "zapping" (previous/next channel navigation) without loading tracking scripts, ads, or bloated web players.
3. **Pluggable On-Demand Recording:** Seamlessly tap into active streams to record them to disk using customizable FFmpeg profiles (such as GPU-accelerated NVENC or zero-cost passthrough) without duplicating network downloads or interrupting the live preview.
4. **Decoupled Lifecycle:** Background recordings remain completely unaffected when users change channels or close the browser. Reconnecting to a stream being recorded taps back into the ongoing live stream instantly.
5. **Multi-User & Identity Support:** Self-hosted multi-user architecture with personal channel bookmarks, independent personal recordings, and role-based access.

---

## 2. Architectural Principles & Clean Architecture Design

To ensure industrial-grade software engineering, maintainability, and clear separation of concerns, STVR adopts **Domain-Driven Design (DDD)** and **Hexagonal / Clean Architecture** principles:

### 2.1 Separation of Concerns
- **Core First:** The system is fundamentally a live streaming hub. Features such as recording, historical media management, and Docker execution are pluggable extensions that do not pollute the core business logic.
- **Pure Domain Models:** Domain entities are pure Java POJOs/Records without any database annotations, ORM tags, or Spring Data imports.
- **Isolated Persistence:** All database-specific models (Neo4j `@Node`, `@Relationship`), repositories, and query logic are strictly contained inside dedicated `persistence` submodules. Mappers (MapStruct) bridge domain models and graph nodes.
- **External Tools as Packages:** Third-party CLI tools and system wrappers (Streamlink, yt-dlp, TikTok Live Recorder, Docker, FFmpeg) live inside a dedicated `packages/` module namespace, completely decoupled from business domain logic.
- **Hot-Pluggable Adapters as Plugins:** Connectors between third-party tool packages and Core SPI ports live inside dedicated plugin modules (`plugins/`). This ensures that tools can be added, swapped, or removed dynamically without recompiling or altering the Core.

---

## 3. Hierarchical Multi-Module Architecture

STVR is organized into a hierarchical multi-module Gradle project using the **API / Entities / Persistence / Implementation** quad-module pattern for each bounded context:

```text
stvr/
└── java/                                 # Root Gradle project
    ├── build.gradle                      # Root Gradle configuration, plugin management & versions
    ├── settings.gradle                   # Recursive autodiscovery for submodules
    │
    ├── framework/                        # Reusable architectural primitives (CQRS, Mediator)
    │   ├── cqrs/                         # Command, CommandHandler, Query, QueryHandler abstractions
    │   └── mediator/                     # SpringMediator and Mediator abstraction
    │
    ├── core/                             # Fundamental Bounded Contexts (The Streaming Hub)
    │   ├── identity/                     # User accounts, authentication & authorization
    │   │   ├── entities/                 # Pure domain models (UserId, Role, UserPreference)
    │   │   ├── api/                      # UserService, SecurityContext, UserDto, Events
    │   │   ├── persistence/              # UserNode, ReactiveUserRepository, MapStruct mappers
    │   │   └── implementation/           # UserServiceImpl, Password hashing, JWT/Session
    │   │
    │   ├── catalog/                      # Channel & platform management
    │   │   ├── entities/                 # Pure domain models (Channel, Platform, ChannelMetadata, GenericChannelMetadata)
    │   │   ├── api/                      # Public contracts (Command/Query Handlers, DTOs, ChannelInspector Port/SPI)
    │   │   ├── persistence/              # ChannelNode, ReactiveChannelRepository, MapStruct mappers
    │   │   └── implementation/           # Command & Query Handler implementations (CQRS)
    │   │
    │   ├── presence/                     # Live presence & status monitoring (Online / Offline)
    │   │   ├── entities/                 # Pure presence status models
    │   │   ├── api/                      # PlatformStatusProvider (SPI), ChannelStatusChangedEvent
    │   │   ├── persistence/              # Historical presence records & queries
    │   │   └── implementation/           # LivePresenceHubImpl, Platform-specific status providers
    │   │
    │   └── streaming/                    # Live stream ingestion & in-memory distribution
    │       ├── entities/                 # StreamSession, StreamQuality, StreamBuffer
    │       ├── api/                      # StreamPipeline, StreamHub, StreamSink interfaces
    │       ├── persistence/              # Active session persistence (if required)
    │       └── implementation/           # ChannelStreamHubImpl, Reactive multicast engine
    │
    ├── packages/                         # Agnostic technical adapters & external CLI/tool wrappers
    │   ├── streamlink/                   # Streamlink CLI wrapper & JSON output parser
    │   ├── ytdlp/                        # yt-dlp CLI wrapper & extractor parser
    │   ├── tiktokliverecorder/           # TikTok Live Recorder CLI wrapper
    │   ├── docker/                       # Docker CLI adapter (Local socket or remote via --host)
    │   └── ffmpeg/                       # FFmpeg process orchestrator & hot-reload profiles
    │
    ├── plugins/                          # Hot-swappable integrations bridging packages/ to core SPI ports
    │   ├── streamlink/                   # Adapts packages:streamlink to ChannelInspector & PlatformStatusProvider
    │   ├── ytdlp/                        # Adapts packages:ytdlp to ChannelInspector
    │   └── tiktokliverecorder/           # Adapts packages:tiktokliverecorder to ChannelInspector
    │
    ├── features/                         # Pluggable extended functionalities
    │   ├── recorder/                     # Background & on-demand recording engine
    │   │   ├── entities/                 # Pure models: RecordingSession, RecordingProfile
    │   │   ├── api/                      # RecorderService, RecordingSessionDto
    │   │   ├── persistence/              # RecordingNode, ReactiveRecordingRepository, Mappers
    │   │   └── implementation/           # FfmpegRecorderSink (taps into core:streaming:api)
    │   │
    │   └── media/                        # Recorded video archive & Byte-Range HTTP streaming
    │       ├── entities/                 # Pure models: MediaItem, VideoMetadata
    │       ├── api/                      # MediaStorageService, VideoStreamRange
    │       ├── persistence/              # MediaFileNode, ReactiveMediaRepository, Mappers
    │       └── implementation/           # MediaStorageServiceImpl, HTTP 206 Partial Content server
    │
    ├── presentation/                     # Delivery layer (REST, SSE, Web UI, Player)
    │   └── server/                       # Executable Spring Boot application & static web player
    │
    └── testing/                          # Multi-level test suites
        ├── contracts/                    # Reusable Contract Test suites (AddChannelContractTest, etc.)
        └── integration/                  # Integration test suites
            ├── mocked/                   # Integration tests using fast in-memory mocks
            └── real/                     # Integration tests with Testcontainers & real external daemons
```

---

## 4. Distributed Multi-Node Topology & Stream Transport

STVR natively supports distributed heterogeneous hardware setups (e.g., dedicated ingestion machines capturing streams and dedicated GPU nodes for transcoding):

```mermaid
flowchart TD
    subgraph Internet
        Origin["Twitch, YouTube, Kick, etc."]
    end

    subgraph "Ingestion Worker (e.g., Machine A / AMD 580)"
        Origin -->|1x Ingestion| SL1["Streamlink (Stream 1)"]
        Origin -->|1x Ingestion| SL2["Streamlink (Stream 2)"]
        SL1 --> GW["Path-Based Media Gateway (MediaMTX / SRT)<br>Single Port: 8554"]
        SL2 --> GW
    end

    subgraph "Transcoding Worker (e.g., Machine B / NVIDIA RTX GPU)"
        GW -->|Direct LAN SRT: srt://worker-a:8554/stream1| FFMPEG["FFmpeg NVENC (HEVC 10-bit)"]
        FFMPEG --> Disk["Storage: /recordings/..."]
    end

    subgraph "STVR Server (Orchestrator & Web UI)"
        Server["Spring Boot WebFlux Backend"]
        Server -.->|Assign Stream Job| SL1
        Server -.->|Trigger Recording Job: POST /jobs| FFMPEG
        GW -.->|On-Demand Preview: srt://worker-a:8554/stream1| Server
        Server --> WebPlayer["HTML5 Browser Player (Zapping)"]
    end
```

### 4.1 Key Architecture Points:
1. **Zero Server Network Tromboning:** The dedicated GPU worker pulls video bytes **directly from the ingestion worker** via high-performance SRT over the LAN (`srt://worker-a:8554/stream1`). The STVR Server orchestrates the recording via lightweight REST/control signals and never re-transmits heavy recording bytes.
2. **Single-Port Multiplexing for N Streams:** Ingestion workers run a lightweight path-based media gateway (e.g., MediaMTX, <15MB RAM). A single exposed port (`8554`) handles $N$ concurrent streams via URI paths (`/stream1`, `/stream2`), eliminating dynamic port exposure chaos.
3. **Hardware Specialization:** Ingestion machines with standard or non-NVENC hardware (e.g., AMD RX 580) focus 100% of their CPU and network on stream capture, while the dedicated NVIDIA machine (e.g., RTX 3070 Ti) concentrates exclusively on high-efficiency NVENC HEVC transcoding.
4. **Standalone Local Fallback:** On a single-machine deployment, all components bind to `localhost:8554` over loopback, retaining the exact same codebase and zero network card overhead.

---

## 5. Reactive Multicast Pipeline: The (Stream, Quality) Ingestion Pair

To maintain zero duplicate network ingestion while avoiding complex transcoding conflicts between users, active streams are managed via the tuple **`(channelId, quality)`**:

```mermaid
flowchart TD
    Origin[Origin Platform] -->|Single Ingestion Stream per Pair| Hub["ChannelStreamHub (channelId, quality)<br>Sinks.many().multicast()"]

    Hub -->|Subscriber 1| U1[User 1: Live Web Player]
    Hub -->|Subscriber 2| U2[User 2: Live Web Player]
    Hub -->|Subscriber 3| RecSink1["Recording Sink: User 1 (NVENC)"]
    Hub -->|Subscriber 4| RecSink2["Recording Sink: User 2 (Passthrough)"]

    RecSink1 --> Disk1["Storage: /media/user1/..."]
    RecSink2 --> Disk2["Storage: /media/user2/..."]
```

### 5.1 Ingestion & Multicast Rules:
1. **Shared Stream Hub:** If User 1 and User 2 watch `(Channel A, 720p)`, both subscribe to the exact same `ChannelStreamHub(Channel A, 720p)` in memory. The origin server receives only 1 connection.
2. **Quality Isolation:** If User 3 specifically requests `(Channel A, 1080p)`, a separate hub is created for `(Channel A, 1080p)`, honoring their explicit preference cleanly without cross-user interference.
3. **Independent Recording Taps:** Multiple users can record the same stream with their own desired FFmpeg profile and start/stop timestamps. Each user's recording sink writes to their own dedicated output file.

---

## 6. Persistence & User-Owned Media Model (Neo4j)

### 6.1 Graph Schema
Relationships in Neo4j remain direct and cleanly isolated per user:

```mermaid
graph TD
    User1[":User {id: 'alice'}"] -->|FAVORITED| Ch1[":Channel {name: 'Ibai'}"]
    User2[":User {id: 'bob'}"] -->|FAVORITED| Ch1

    User1 -->|OWNS| Vid1[":VideoFile {path: '/data/alice/stream_123.mkv'}"]
    User2 -->|OWNS| Vid2[":VideoFile {path: '/data/bob/stream_456.mp4'}"]
```

### 6.2 Direct Ownership & Simple Deletion Lifecycle
- **Strict 1-to-1 Ownership:** Every recording belongs strictly to the user who initiated it: `(User)-[:OWNS]->(VideoFile)`.
- **Direct Deletion:** When a user deletes one of their recorded videos:
  1. The physical file is removed from disk (`Files.deleteIfExists(path)`).
  2. The `VideoFile` node and its relationships are deleted from Neo4j.
- **Sharing as Future Scope:** Social or inter-user media sharing is deliberately separated and deferred as an optional future enhancement, avoiding premature complexity.

---

## 7. Execution Modes & Tool Integration (`packages/`)

### 7.1 Streamlink Runner & Inspector (`packages:streamlink` & `packages:docker`)
Streamlink execution is decoupled using the Strategy pattern and Hexagonal Ports & Adapters:
- **`StreamlinkChannelInspector` (`--json`):** Technical probe adapter implementing the core domain port `ChannelInspector` (`core:catalog:api.inspector`). Queries `streamlink --json <url>` to detect supported platforms (from `plugin`), extract channel metadata (`author`, `title`), and check live presence & available qualities without requiring platform API credentials.
- **`NativeStreamlinkRunner`:** Directly invokes the host `streamlink` binary from system `PATH`. Universal for standard installations.
- **`DockerStreamlinkRunner`:** Invokes `docker [--host <remote_host>] exec -i <container_name> streamlink ...` (targeting the `streamlink` container). Supports local Docker daemons or remote Docker endpoints via `--host` / `DOCKER_HOST`.

### 7.2 FFmpeg & Hot-Reload Profiles (`packages:ffmpeg`)
Recordings are transcoded/muxed via FFmpeg without server restarts:
- **`passthrough` (Default):** `-c copy -f matroska`. Zero CPU/GPU encoding cost.
- **`nvenc-gpu` (NVIDIA GPUs):** `scale_cuda`, `hevc_nvenc`, `main10`, `cq 31`, `p6`. Ultra-high quality hardware encoding.
- **`cpu-h264`:** `libx264 -preset veryfast -crf 23`. Universal software encoding.
- **`custom`:** Dynamic parameter string editable directly via API / Web UI.

---

## 8. External API Collectors & Event Shippers

To handle external platform polling, live chat capture, and presence monitoring without blocking or polluting the main server, STVR adopts the **Data Shipper / Collector Pattern** (analogous to *Beats / Elastic Agents*):

```mermaid
flowchart LR
    subgraph External Platforms
        API1[Twitch Helix API]
        API2[YouTube Data API]
        ChatWS[Live Chat WebSockets]
    end

    subgraph "Shipper Containers (Lightweight Collectors)"
        PCollector["Presence Shipper<br>(Monitors online/offline state)"]
        CCollector["Chat Shipper<br>(Captures live comments)"]
    end

    subgraph "STVR Server"
        IngestAPI["Internal Event Ingestion API<br>POST /api/internal/events"]
        EventHub["Reactor Event Bus / SSE"]
        GraphDB["Neo4j Graph Store"]
    end

    API1 & API2 --> PCollector
    ChatWS --> CCollector

    PCollector -->|Presence Updates| IngestAPI
    CCollector -->|Chat Messages| IngestAPI

    IngestAPI --> EventHub
    IngestAPI --> GraphDB
```

### 8.1 Advantages of the Shipper Pattern:
1. **Rate-Limit & IP Fault Isolation:** If an external platform throttles or bans an IP address due to frequent polling, only that isolated collector container is affected; the STVR server and active video streams remain 100% operational.
2. **Minimal Footprint:** Shippers are tiny asynchronous processes (Go, Python async, or Node.js, ~15-20MB RAM) that only handle text and HTTP polling.
3. **Independent Scaling:** Chat collectors can scale horizontally per active stream without requiring changes to the central Spring Boot server.

---

## 9. Automated Deployment Profiles & Orchestration

STVR supports diverse deployment scales through pre-configured **Deployment Profiles**:

```mermaid
graph TD
    Profile1["1. Standalone Profile<br>(Single Machine / Homelab PC)<br>docker-compose.yml"]
    Profile2["2. Distributed Homelab Profile<br>(Machine A: Ingestion + Machine B: GPU Transcoding)<br>docker-compose.distributed.yml"]
    Profile3["3. Kubernetes / HA Cluster Profile<br>(Full auto-healing, nodeSelector for GPU)<br>Helm Chart"]
```

### 9.1 Profiles Breakdown:
- **Profile `standalone` (Default for GitHub / Local Testing):**
  - Single `docker-compose.yml` deploying `stvr-server`, `neo4j`, and local `streamlink`.
  - All communication binds to internal Docker bridge or `localhost`.
- **Profile `distributed-homelab` (Multi-Machine Hardware Specialization):**
  - **Machine A (Ingestion - AMD 580):** `docker-compose.ingest.yml` (Streamlink + MediaMTX SRT on port `8554`).
  - **Machine B (Transcoding - RTX 3070 Ti):** `docker-compose.transcode.yml` (FFmpeg worker with NVIDIA container runtime).
  - **Machine C (or B):** `docker-compose.server.yml` (STVR Server + Neo4j).
  - Coordinated via environment variables:
    ```env
    STVR_INGEST_NODE_URL=srt://192.168.1.100:8554
    STVR_GPU_WORKER_URL=http://192.168.1.101:9090
    ```
- **Profile `k8s-cluster` (Enterprise / Kubernetes):**
  - Helm chart with dedicated Pod specs.
  - Transcoding pods assigned via node affinity: `nodeSelector: { "nvidia.com/gpu": "true" }`.
  - Ingestion pods running as lightweight scalable Workers.

---

## 10. Technology Stack & Key Dependencies

| Technology | Role / Purpose | Module Location |
| :--- | :--- | :--- |
| **Java 17 (OpenJDK)** | Core language runtime (LTS) | All modules |
| **Gradle 9.x (Groovy DSL)** | Multi-module build automation & dependency management | Root (`build.gradle`, `settings.gradle`) |
| **Spring Boot 3.x (WebFlux)** | Reactive, non-blocking web framework, Netty server, REST & SSE | `presentation:server`, `api`, `implementation` |
| **Project Reactor** | Reactive programming model (`Mono`, `Flux`, `Sinks.many().multicast()`) | `core:*`, `features:*`, `presentation:server` |
| **Spring Data Neo4j (SDN)** | Reactive graph object mapping (`ReactiveNeo4jRepository`) | `*:persistence` modules |
| **Neo4j Java Driver** | High-performance Bolt protocol connection to Neo4j | `*:persistence`, `presentation:server` |
| **MapStruct** | Compile-time, type-safe mapping between Domain Models and Graph Nodes | `*:persistence` modules |
| **Project Lombok** | Boilerplate reduction (getters, builders, constructors, slf4j) | Compile-only across all modules |
| **Jackson (JSR310)** | Non-blocking JSON serialization/deserialization | `api`, `presentation:server` |
| **MediaMTX / SRT Protocol** | Path-based single-port media gateway for N concurrent streams | Ingestion Workers |
| **Streamlink CLI** | Origin media stream resolution and packet extraction | `packages:streamlink`, `packages:docker` |
| **FFmpeg** | Video encoding, hardware acceleration (NVENC/CUDA) & container packaging | `packages:ffmpeg` |
| **Modern Frontend (Vanilla Web)** | Reactive HTML5 video player, Server-Sent Events listener, Dark UI | `presentation/server/src/main/resources/static` |

---

## 11. Summary of Development Workflow & Roadmap

1. **Gradle Multi-Module Foundation:** Setup `settings.gradle`, shared plugins in `build.gradle`, and module skeletons.
2. **Core Domain & Persistence:** Define pure records in `entities`, MapStruct mappers and Neo4j repositories in `persistence`.
3. **Reactive Stream Engine:** Implement `ChannelStreamHub`, multicast sinks, and Streamlink CLI adapters.
4. **Presence & Catalog:** Implement channel management and platform status providers.
5. **Pluggable Recorder:** Implement FFmpeg recording sink and dynamic tap.
6. **Web Server & UI:** Implement WebFlux controllers, SSE feeds, and the modern television-style zapping interface.

---

## 12. Architectural Decision Records (ADR) & Key Design Rationales

This section formally documents critical design decisions made during the evolution of the STVR architecture, recording the context, evaluated alternatives, and engineering rationale.

### ADR-01: Extensibility of Channel Metadata via Typed Class Inheritance (over Generic Maps or Monolithic POJOs)
- **Status:** Accepted
- **Context:** Different streaming platforms (Twitch, YouTube, Kick, etc.) provide distinct, platform-specific metadata (e.g. Twitch broadcaster type/affiliate tier, YouTube scheduled start time/privacy status, Kick chatroom ID). The system needed an extensible mechanism to represent these attributes while maintaining a clean, agnostic core.
- **Alternatives Considered:**
  1. *Flat Map/Attributes dictionary (`Map<String, Object>`)*: Requires string literal keys ("magic strings"), bypasses compile-time type checking, and forces continuous downcasting with high risk of `ClassCastException`.
  2. *Monolithic POJO with optional fields for all platforms*: Violates the Open-Closed Principle (OCP), creates unnecessary coupling, and forces core modifications whenever an external platform adds or alters attributes.
- **Decision:** Use an abstract domain base class `ChannelMetadata` with strongly-typed common fields (`isLive`, `title`, `category`, `tags`, `availableQualities`) in `core:catalog:entities`, accompanied by a standard fallback implementation (`GenericChannelMetadata`). Platform-specific modules extend this base class (e.g. `TwitchChannelMetadata extends ChannelMetadata`), preserving 100% type safety and IDE autocomplete without leaking external dependencies or proprietary fields into the core domain.

### ADR-02: Spring Bean Autodiscovery for Platform Providers (Elimination of Composite Factories)
- **Status:** Accepted
- **Context:** Platform metadata providers need to be registered and resolved dynamically at runtime based on platform type or channel URL.
- **Alternatives Considered:**
  1. *Custom Composite Factories (`CompositePlatformMetadataProviderFactory`)*: Introduces an additional wrapper layer and boilerplate registry code.
  2. *Spring Autowired Collections*: Spring natively discovers all `@Component` beans implementing a common interface (`PlatformMetadataProvider<T>`) and injects them as `List<PlatformMetadataProvider<?>>` via constructor injection.
- **Decision:** Rely directly on Spring's native bean autodiscovery for `PlatformMetadataProvider<T>`. Consuming services inject the provider list directly into their constructor, removing redundant wrapper factories and minimizing accidental architectural complexity. Pure unit tests achieve the exact same behavior by supplying `List.of(mockProvider)`.

### ADR-03: Decoupled Channel Inspection and Pluggable Inspector Factory (Ports & Adapters / DI Strategy)
- **Status:** Accepted
- **Context:** When a channel is registered via `AddChannelCommandHandler` (UC-CAT-01), the system must validate the URL, identify the streaming platform, extract streamer slug and display name, and detect the initial live status and available stream qualities without requiring upfront user credentials.
  In initial drafts, `core:catalog:implementation` directly invoked Streamlink CLI commands (`streamlink --json <url>`), creating a direct compile-time and runtime dependency on `packages:streamlink`.
  This violated core architectural principles:
  1. *Violation of Dependency Inversion Principle (DIP):* High-level business logic in `core:catalog` depended on low-level infrastructure details (a third-party Python CLI wrapper).
  2. *Single Tool Monopoly:* Streaming platforms often offer multiple resolution paths: official REST APIs (Twitch Helix, YouTube Data API v3), alternative extractors (`yt-dlp`), direct HLS/DASH manifest probes, or internal RTMP/SRT ingest endpoints. Tying the core to Streamlink prevented coexistence or dynamic selection among providers.
  3. *Testability Bottlenecks:* Contract and unit tests would require external binaries, Docker daemons, or heavy process mocking, slowing down test execution and introducing brittleness.

- **Alternatives Considered:**
  1. *Direct Core Dependency on Streamlink (`core:catalog -> packages:streamlink`)*: Direct coupling, makes Streamlink mandatory across the entire application lifecycle, prevents alternative inspectors, and violates hexagonal architecture.
  2. *Hardcoded `switch(platform)` / Conditional Branches in Core*: Violates the Open-Closed Principle (OCP); adding a new platform or provider forces modifications inside core use case handlers.
  3. *Static Factory / ServiceLocator*: Uses global static state, impeding test isolation and concurrent test execution.
  4. *Hexagonal Port (`ChannelInspector`) + Factory/Composite Pattern with Spring Constructor DI*: The Core defines a clean domain port and relies on dependency injection. Concrete adapters implement the port in external packages, and a composite factory orchestrates them based on URL compatibility and priority.

- **Decision:** Adopt Hexagonal Architecture (Ports and Adapters) with dynamic Dependency Injection:
  1. **Domain Outbound Port (`core:catalog:api.inspector`):**
     - `ChannelInspector`: Contract defining `boolean supports(ChannelUrl url)` and `Mono<ChannelInspectionResult> inspect(ChannelUrl url)`.
     - `ChannelInspectionResult`: Pure domain record encapsulating platform, slug, name, live status, category, tags, and available stream qualities.
     - `ChannelInspectorFactory`: Port interface exposing `Optional<ChannelInspector> getInspector(ChannelUrl url)`.
     - `CompositeChannelInspectorFactory`: Standard composite implementation that evaluates registered inspectors in priority order.
  2. **Infrastructure Adapters (`plugins:streamlink`, `plugins:ytdlp`, `plugins:tiktokliverecorder`):**
     - Adapters live inside dedicated plugin modules under `plugins/` (e.g. `StreamlinkChannelInspector` in `plugins:streamlink`), bridging agnostic tool wrappers (`packages/`) to the domain port `ChannelInspector`.
     - `core:catalog:implementation` has **zero build dependencies** on external tools or plugin modules.
  3. **Dependency Injection & Spring Autodiscovery:**
     - *Spring Runtime:* Spring automatically discovers all beans implementing `ChannelInspector` (`@Component`) and injects them as `List<ChannelInspector>` into `CompositeChannelInspectorFactory`. Bean order (`@Order`) controls precedence (e.g. Native Platform APIs > CLI Extractors like Streamlink/yt-dlp > Generic Probes).
     - *Pure TDD / Unit & Contract Testing:* Tests inject a fast in-memory `MockChannelInspector` directly into `CompositeChannelInspectorFactory.of(mockInspector)`, allowing comprehensive contract suites (e.g. `AddChannelContractTest`) to execute 10 scenarios in milliseconds without Docker or network dependencies.
  4. **Multi-Step Resolution Pipeline in `AddChannelCommandHandlerImpl`:**
     - Step 1: User-level duplicate check (`existsUserChannel`) -> throws `DuplicateChannelException` if already tracked.
     - Step 2: Global catalog check (`findChannelByUrl`) -> if channel already exists in system, reuses the existing `Channel` entity and establishes user association `(:User)-[:TRACKS]->(:Channel)`, avoiding redundant inspection and duplicate polling.
     - Step 3: Dynamic inspector resolution via `ChannelInspectorFactory.getInspector(url)`.
     - Step 4: Reactive metadata extraction (`inspector.inspect(url)`). User-provided `customName` takes precedence over scraped name if present.
     - Step 5: Fallback handling: If no inspector supports the URL or inspection fails:
       - If user provided `customName`: falls back to `Platform.CUSTOM`.
       - If no `customName`: throws `UnsupportedPlatformException`.

```mermaid
flowchart TD
    subgraph "Core: Catalog Bounded Context"
        subgraph "core:catalog:api.inspector"
            Port["<<interface>>\nChannelInspector\n+supports(url): boolean\n+inspect(url): Mono<ChannelInspectionResult>"]
            Factory["<<interface>>\nChannelInspectorFactory\n+getInspector(url): Optional<ChannelInspector>"]
            Composite["CompositeChannelInspectorFactory\n(implements ChannelInspectorFactory)"]
        end

        subgraph "core:catalog:implementation"
            Handler["AddChannelCommandHandlerImpl\n(injects ChannelInspectorFactory & ChannelRepository)"]
        end
    end

    subgraph "Plugins (Hot-Swappable Adapters)"
        SL["StreamlinkChannelInspector\n(plugins:streamlink)"]
        YT["YtDlpChannelInspector\n(plugins:ytdlp)"]
        TT["TikTokChannelInspector\n(plugins:tiktokliverecorder)"]
    end

    subgraph "Packages (Agnostic Tool Wrappers)"
        SL_CLI["Streamlink CLI Wrapper\n(packages:streamlink)"]
        YT_CLI["yt-dlp CLI Wrapper\n(packages:ytdlp)"]
        TT_CLI["TikTok Live Recorder Wrapper\n(packages:tiktokliverecorder)"]
    end

    subgraph "Testing Layer"
        Mock["MockChannelInspector\n(testing:integration:mocked)"]
    end

    Handler --> Factory
    Factory <|.. Composite
    Composite --> Port
    SL -. implements .-> Port
    YT -. implements .-> Port
    TT -. implements .-> Port
    Mock -. implements .-> Port

    SL --> SL_CLI
    YT --> YT_CLI
    TT --> TT_CLI
```

- **Consequences:**
  - *Positive:* Total decoupling of Core from third-party tools; adding or replacing extraction mechanisms (Streamlink, `ytdlp`, `tiktokliverecorder`) requires zero changes and zero recompilations of `core:catalog`; 100% deterministic, high-speed contract tests.
  - *Trade-off:* Requires an additional port and factory abstraction, but this cost is negligible compared to the architectural flexibility gained.

