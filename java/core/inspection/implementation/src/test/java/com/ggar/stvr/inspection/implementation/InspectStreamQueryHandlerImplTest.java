package com.ggar.stvr.inspection.implementation;

import com.ggar.stvr.catalog.entities.ChannelUrl;
import com.ggar.stvr.catalog.entities.Platform;
import com.ggar.stvr.inspection.api.InspectStreamQueryHandler.InspectStreamQuery;
import com.ggar.stvr.inspection.api.InspectionChain;
import com.ggar.stvr.inspection.api.InspectionPlugin;
import com.ggar.stvr.inspection.entities.StreamInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InspectStreamQueryHandlerImplTest {

    @Test
    @DisplayName("Should inspect stream directly and return StreamInfo")
    void shouldInspectStreamSuccessfully() {
        ChannelUrl url = ChannelUrl.of("https://twitch.tv/ninja");

        InspectionPlugin mockPlugin = new InspectionPlugin() {
            @Override
            public String getId() {
                return "streamlink";
            }

            @Override
            public Mono<StreamInfo> inspect(StreamInfo info, InspectionChain chain) {
                return chain.proceed(info.toBuilder()
                        .live(true)
                        .title("Fortnite Tournament")
                        .category("Fortnite")
                        .availableQualities(List.of("1080p60", "720p60"))
                        .metadata(Map.of("fps", 60))
                        .build());
            }
        };

        InspectionPipeline pipeline = new InspectionPipeline(List.of(mockPlugin), "streamlink");
        InspectStreamQueryHandlerImpl handler = new InspectStreamQueryHandlerImpl(pipeline);

        InspectStreamQuery query = new InspectStreamQuery(url, Platform.TWITCH);

        StepVerifier.create(handler.handle(query))
                .assertNext((StreamInfo info) -> {
                    assertThat(info.getChannelUrl()).isEqualTo(url);
                    assertThat(info.getPlatform()).isEqualTo(Platform.TWITCH);
                    assertThat(info.isLive()).isTrue();
                    assertThat(info.getTitle()).isEqualTo("Fortnite Tournament");
                    assertThat(info.getCategory()).isEqualTo("Fortnite");
                    assertThat(info.getAvailableQualities()).containsExactly("1080p60", "720p60");
                    assertThat(info.getMetadata()).containsEntry("fps", 60);
                })
                .verifyComplete();
    }
}
