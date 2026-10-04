package com.ggar.stvr.plugins.streamlink;

import com.ggar.stvr.packages.streamlink.session.StreamlinkSession;
import com.ggar.stvr.streaming.api.LiveStreamConnection;
import reactor.core.publisher.Flux;

import java.util.Objects;

/**
 * Adapter wrapping a StreamlinkSession into the STVR LiveStreamConnection interface.
 */
public class StreamlinkLiveStreamConnection implements LiveStreamConnection {

    private final StreamlinkSession session;

    public StreamlinkLiveStreamConnection(StreamlinkSession session) {
        this.session = Objects.requireNonNull(session, "StreamlinkSession cannot be null");
    }

    @Override
    public Flux<byte[]> data() {
        return session.data();
    }

    @Override
    public boolean isAlive() {
        return session.isAlive();
    }

    @Override
    public void cancel() {
        session.cancel();
    }

    public StreamlinkSession getSession() {
        return session;
    }
}
