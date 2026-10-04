package com.ggar.stvr.streaming.implementation;

import com.ggar.stvr.identity.entities.UserId;
import com.ggar.stvr.streaming.api.LiveStreamConnection;
import com.ggar.stvr.streaming.entities.ActiveStreamSnapshot;
import com.ggar.stvr.streaming.entities.StreamKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * In-memory multicast hub for a single active live stream (UC-STR-01, UC-STR-02).
 * Demuxes a single LiveStreamConnection to multiple concurrent viewers via reactive multicast sink,
 * tracking viewers and shutting down after a grace period when all viewers disconnect.
 */
public class ChannelStreamHub {

    private static final Logger log = LoggerFactory.getLogger(ChannelStreamHub.class);
    private static final Duration DEFAULT_GRACE_PERIOD = Duration.ofSeconds(5);

    private final StreamKey streamKey;
    private final LiveStreamConnection connection;
    private final Duration gracePeriod;
    private final Consumer<StreamKey> onShutdown;
    private final Instant startedAt;

    private final Sinks.Many<byte[]> sink = Sinks.many().multicast().onBackpressureBuffer(256, false);
    private final Set<UserId> activeViewers = ConcurrentHashMap.newKeySet();
    private final AtomicInteger viewerCount = new AtomicInteger(0);
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final AtomicReference<Disposable> gracePeriodDisposable = new AtomicReference<>(Disposables.disposed());
    private Disposable upstreamSubscription;

    public ChannelStreamHub(
            StreamKey streamKey,
            LiveStreamConnection connection,
            Consumer<StreamKey> onShutdown
    ) {
        this(streamKey, connection, DEFAULT_GRACE_PERIOD, onShutdown);
    }

    public ChannelStreamHub(
            StreamKey streamKey,
            LiveStreamConnection connection,
            Duration gracePeriod,
            Consumer<StreamKey> onShutdown
    ) {
        this.streamKey = Objects.requireNonNull(streamKey, "streamKey cannot be null");
        this.connection = Objects.requireNonNull(connection, "connection cannot be null");
        this.gracePeriod = gracePeriod != null ? gracePeriod : DEFAULT_GRACE_PERIOD;
        this.onShutdown = onShutdown != null ? onShutdown : k -> {};
        this.startedAt = Instant.now();

        startUpstreamConsumption();
    }

    private void startUpstreamConsumption() {
        this.upstreamSubscription = connection.data()
                .subscribe(
                        chunk -> {
                            if (!isClosed.get()) {
                                sink.tryEmitNext(chunk);
                            }
                        },
                        error -> {
                            log.error("Live stream connection error for key {}: {}", streamKey, error.getMessage());
                            sink.tryEmitError(error);
                            shutdown();
                        },
                        () -> {
                            log.info("Live stream reached normal EOF for key {}", streamKey);
                            sink.tryEmitComplete();
                            shutdown();
                        }
                );
    }

    /**
     * Attaches a user's connection to the multicast stream for live preview.
     *
     * @param userId user opening the preview
     * @return continuous Flux of video byte chunks
     */
    public Flux<byte[]> attachViewer(UserId userId) {
        Objects.requireNonNull(userId, "userId cannot be null");

        if (isClosed.get()) {
            return Flux.empty();
        }

        // Cancel pending grace period timer if a viewer joins
        cancelGracePeriod();

        if (activeViewers.add(userId)) {
            int count = viewerCount.incrementAndGet();
            log.debug("Viewer {} attached to stream {}. Total viewers: {}", userId.value(), streamKey, count);
        }

        return sink.asFlux()
                .doFinally(signalType -> detachViewer(userId));
    }

    /**
     * Detaches an active viewer from the stream hub.
     *
     * @param userId user disconnecting or closing preview
     */
    public void detachViewer(UserId userId) {
        if (userId == null) {
            return;
        }

        if (activeViewers.remove(userId)) {
            int count = viewerCount.decrementAndGet();
            log.debug("Viewer {} detached from stream {}. Remaining viewers: {}", userId.value(), streamKey, count);

            if (count <= 0) {
                scheduleGracePeriod();
            }
        }
    }

    private void cancelGracePeriod() {
        Disposable current = gracePeriodDisposable.getAndSet(Disposables.disposed());
        if (current != null && !current.isDisposed()) {
            current.dispose();
            log.debug("Grace period cancelled for stream {} as viewer attached", streamKey);
        }
    }

    private void scheduleGracePeriod() {
        log.debug("No active viewers for stream {}. Scheduling shutdown in {}s",
                streamKey, gracePeriod.toSeconds());

        Disposable timer = Mono.delay(gracePeriod, Schedulers.parallel())
                .subscribe(tick -> {
                    if (viewerCount.get() <= 0 && !isClosed.get()) {
                        log.info("Grace period expired with 0 viewers. Shutting down stream hub for {}", streamKey);
                        shutdown();
                    }
                });

        Disposable prev = gracePeriodDisposable.getAndSet(timer);
        if (prev != null && !prev.isDisposed()) {
            prev.dispose();
        }
    }

    /**
     * Closes the multicast hub immediately, terminating the underlying connection.
     */
    public void shutdown() {
        if (isClosed.compareAndSet(false, true)) {
            log.info("Shutting down ChannelStreamHub for {}", streamKey);
            cancelGracePeriod();

            if (upstreamSubscription != null && !upstreamSubscription.isDisposed()) {
                upstreamSubscription.dispose();
            }

            connection.cancel();
            sink.tryEmitComplete();
            onShutdown.accept(streamKey);
        }
    }

    public StreamKey getStreamKey() {
        return streamKey;
    }

    public int getViewerCount() {
        return Math.max(0, viewerCount.get());
    }

    public boolean isAlive() {
        return !isClosed.get() && connection.isAlive();
    }

    public ActiveStreamSnapshot toSnapshot() {
        return ActiveStreamSnapshot.builder()
                .streamKey(streamKey)
                .startedAt(startedAt)
                .connectedViewers(getViewerCount())
                .active(isAlive())
                .build();
    }
}

