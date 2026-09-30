package com.ggar.stvr.packages.streamlink.command;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Base fluent builder providing common Streamlink CLI options.
 *
 * @param <B> self type for fluent chaining
 */
@SuppressWarnings("unchecked")
public abstract class AbstractStreamlinkCommandBuilder<B extends AbstractStreamlinkCommandBuilder<B>> {

    protected String binary = "streamlink";
    protected String url;
    protected String quality;
    protected boolean json;
    protected boolean stdout;
    protected String outputPath;
    protected Integer retryStreams;
    protected Integer retryOpen;
    protected Integer streamSegmentThreads;
    protected String httpProxy;
    protected String httpsProxy;
    protected final Map<String, String> httpHeaders = new LinkedHashMap<>();
    protected final List<String> customFlags = new ArrayList<>();
    protected final Map<String, List<String>> customOptions = new LinkedHashMap<>();

    protected B self() {
        return (B) this;
    }

    public B binary(String binary) {
        this.binary = binary;
        return self();
    }

    public B url(String url) {
        this.url = url;
        return self();
    }

    public B quality(String quality) {
        this.quality = quality;
        return self();
    }

    public B json() {
        this.json = true;
        return self();
    }

    public B json(boolean json) {
        this.json = json;
        return self();
    }

    public B stdout() {
        this.stdout = true;
        return self();
    }

    public B stdout(boolean stdout) {
        this.stdout = stdout;
        return self();
    }

    public B output(String outputPath) {
        this.outputPath = outputPath;
        return self();
    }

    public B retryStreams(int retryStreams) {
        this.retryStreams = retryStreams;
        return self();
    }

    public B retryOpen(int retryOpen) {
        this.retryOpen = retryOpen;
        return self();
    }

    public B streamSegmentThreads(int threads) {
        this.streamSegmentThreads = threads;
        return self();
    }

    public B httpProxy(String proxyUrl) {
        this.httpProxy = proxyUrl;
        return self();
    }

    public B httpsProxy(String proxyUrl) {
        this.httpsProxy = proxyUrl;
        return self();
    }

    public B httpHeader(String name, String value) {
        this.httpHeaders.put(name, value);
        return self();
    }

    public B httpHeaders(Map<String, String> headers) {
        if (headers != null) {
            this.httpHeaders.putAll(headers);
        }
        return self();
    }

    public B customFlag(String flag) {
        this.customFlags.add(flag);
        return self();
    }

    public B customOption(String option, String value) {
        this.customOptions.computeIfAbsent(option, k -> new ArrayList<>()).add(value);
        return self();
    }

    public StreamlinkCommand build() {
        return StreamlinkCommand.builder()
                .binary(binary)
                .url(url)
                .quality(quality)
                .json(json)
                .stdout(stdout)
                .outputPath(outputPath)
                .retryStreams(retryStreams)
                .retryOpen(retryOpen)
                .streamSegmentThreads(streamSegmentThreads)
                .httpProxy(httpProxy)
                .httpsProxy(httpsProxy)
                .httpHeaders(httpHeaders)
                .customFlags(customFlags)
                .customOptions(customOptions)
                .build();
    }
}
