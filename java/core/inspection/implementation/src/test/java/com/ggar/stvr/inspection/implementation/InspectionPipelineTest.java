package com.ggar.stvr.inspection.implementation;

import com.ggar.stvr.catalog.entities.ChannelUrl;
import com.ggar.stvr.catalog.entities.Platform;
import com.ggar.stvr.inspection.api.InspectionChain;
import com.ggar.stvr.inspection.api.InspectionPlugin;
import com.ggar.stvr.inspection.entities.StreamInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InspectionPipelineTest {

    @Test
    @DisplayName("Should execute plugins in order defined by natural getOrder()")
    void shouldExecuteInNaturalOrder() {
        List<String> executionLog = new ArrayList<>();

        InspectionPlugin p1 = new TestPlugin("plugin-b", 20, (info, chain) -> {
            executionLog.add("plugin-b");
            return chain.proceed(info.toBuilder().title("From B").build());
        });

        InspectionPlugin p2 = new TestPlugin("plugin-a", 10, (info, chain) -> {
            executionLog.add("plugin-a");
            return chain.proceed(info.toBuilder().title("From A").build());
        });

        InspectionPipeline pipeline = new InspectionPipeline(List.of(p1, p2), "");

        StreamInfo initial = StreamInfo.builder()
                .channelUrl(ChannelUrl.of("https://twitch.tv/ninja"))
                .platform(Platform.TWITCH)
                .build();

        StepVerifier.create(pipeline.execute(initial))
                .assertNext(info -> {
                    assertThat(executionLog).containsExactly("plugin-a", "plugin-b");
                    // plugin-b ran last, so title is From B
                    assertThat(info.getTitle()).isEqualTo("From B");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should execute plugins in order specified by configuration property")
    void shouldExecuteInConfiguredOrder() {
        List<String> executionLog = new ArrayList<>();

        InspectionPlugin p1 = new TestPlugin("streamlink", 50, (info, chain) -> {
            executionLog.add("streamlink");
            return chain.proceed(info.toBuilder().availableQualities(List.of("1080p", "720p")).live(true).build());
        });

        InspectionPlugin p2 = new TestPlugin("twitch-helix", 10, (info, chain) -> {
            executionLog.add("twitch-helix");
            return chain.proceed(info.toBuilder().title("Helix Title").category("Gaming").build());
        });

        // Config explicitly orders: streamlink, twitch-helix
        InspectionPipeline pipeline = new InspectionPipeline(List.of(p2, p1), "streamlink, twitch-helix");

        StreamInfo initial = StreamInfo.builder()
                .channelUrl(ChannelUrl.of("https://twitch.tv/ninja"))
                .platform(Platform.TWITCH)
                .build();

        StepVerifier.create(pipeline.execute(initial))
                .assertNext(info -> {
                    assertThat(executionLog).containsExactly("streamlink", "twitch-helix");
                    assertThat(info.isLive()).isTrue();
                    assertThat(info.getAvailableQualities()).containsExactly("1080p", "720p");
                    assertThat(info.getTitle()).isEqualTo("Helix Title");
                    assertThat(info.getCategory()).isEqualTo("Gaming");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should skip plugins that do not support the stream info")
    void shouldSkipUnsupportedPlugins() {
        List<String> executionLog = new ArrayList<>();

        InspectionPlugin twitchPlugin = new TestPlugin("twitch-helix", 10, (info, chain) -> {
            executionLog.add("twitch");
            return chain.proceed(info);
        }) {
            @Override
            public boolean supports(StreamInfo info) {
                return info.getPlatform() == Platform.TWITCH;
            }
        };

        InspectionPlugin kickPlugin = new TestPlugin("kick-api", 20, (info, chain) -> {
            executionLog.add("kick");
            return chain.proceed(info.toBuilder().live(true).build());
        }) {
            @Override
            public boolean supports(StreamInfo info) {
                return info.getPlatform() == Platform.KICK;
            }
        };

        InspectionPipeline pipeline = new InspectionPipeline(List.of(twitchPlugin, kickPlugin), "");

        StreamInfo kickInfo = StreamInfo.builder()
                .channelUrl(ChannelUrl.of("https://kick.com/streamer"))
                .platform(Platform.KICK)
                .build();

        StepVerifier.create(pipeline.execute(kickInfo))
                .assertNext(info -> {
                    assertThat(executionLog).containsExactly("kick");
                    assertThat(info.isLive()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should allow plugin to short-circuit the pipeline without calling proceed")
    void shouldShortCircuitPipeline() {
        List<String> executionLog = new ArrayList<>();

        InspectionPlugin detector = new TestPlugin("detector", 10, (info, chain) -> {
            executionLog.add("detector");
            // Channel is offline; short-circuit early!
            return Mono.just(info.toBuilder().live(false).build());
        });

        InspectionPlugin heavyInspector = new TestPlugin("heavy", 20, (info, chain) -> {
            executionLog.add("heavy");
            return chain.proceed(info);
        });

        InspectionPipeline pipeline = new InspectionPipeline(List.of(detector, heavyInspector), "");

        StreamInfo initial = StreamInfo.builder()
                .channelUrl(ChannelUrl.of("https://twitch.tv/offline_channel"))
                .platform(Platform.TWITCH)
                .build();

        StepVerifier.create(pipeline.execute(initial))
                .assertNext(info -> {
                    assertThat(executionLog).containsExactly("detector");
                    assertThat(info.isLive()).isFalse();
                })
                .verifyComplete();
    }

    private static class TestPlugin implements InspectionPlugin {
        private final String id;
        private final int order;
        private final java.util.function.BiFunction<StreamInfo, InspectionChain, Mono<StreamInfo>> action;

        public TestPlugin(
                String id,
                int order,
                java.util.function.BiFunction<StreamInfo, InspectionChain, Mono<StreamInfo>> action
        ) {
            this.id = id;
            this.order = order;
            this.action = action;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public int getOrder() {
            return order;
        }

        @Override
        public Mono<StreamInfo> inspect(StreamInfo info, InspectionChain chain) {
            return action.apply(info, chain);
        }
    }
}
