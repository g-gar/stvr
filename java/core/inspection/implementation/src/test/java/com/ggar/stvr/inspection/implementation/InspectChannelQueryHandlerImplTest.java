package com.ggar.stvr.inspection.implementation;

import com.ggar.stvr.catalog.entities.ChannelName;
import com.ggar.stvr.catalog.entities.ChannelUrl;
import com.ggar.stvr.catalog.entities.Platform;
import com.ggar.stvr.inspection.api.InspectChannelQueryHandler.InspectChannelQuery;
import com.ggar.stvr.inspection.api.exception.UnsupportedPlatformException;
import com.ggar.stvr.inspection.api.resolver.ChannelResolution;
import com.ggar.stvr.inspection.api.resolver.ChannelResolver;
import com.ggar.stvr.inspection.api.resolver.CompositeChannelResolverFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InspectChannelQueryHandlerImplTest {

    @Test
    @DisplayName("Should inspect channel and resolve static identity using matching ChannelResolver")
    void shouldInspectChannelSuccessfully() {
        ChannelUrl url = ChannelUrl.of("https://twitch.tv/shroud");

        ChannelResolver twitchResolver = new ChannelResolver() {
            @Override
            public boolean supports(ChannelUrl u) {
                return u.asString().contains("twitch.tv");
            }

            @Override
            public Mono<ChannelResolution> resolve(ChannelUrl u) {
                return Mono.just(ChannelResolution.of(Platform.TWITCH, "shroud", ChannelName.of("shroud")));
            }
        };

        CompositeChannelResolverFactory factory = new CompositeChannelResolverFactory(List.of(twitchResolver));
        InspectChannelQueryHandlerImpl handler = new InspectChannelQueryHandlerImpl(factory);

        StepVerifier.create(handler.handle(new InspectChannelQuery(url)))
                .assertNext(res -> {
                    assertThat(res.platform()).isEqualTo(Platform.TWITCH);
                    assertThat(res.slug()).isEqualTo("shroud");
                    assertThat(res.name()).isEqualTo(ChannelName.of("shroud"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should throw UnsupportedPlatformException when no resolver supports URL")
    void shouldThrowWhenNoResolverSupportsUrl() {
        ChannelUrl url = ChannelUrl.of("https://unknown.platform.com/user");
        CompositeChannelResolverFactory factory = new CompositeChannelResolverFactory(List.of());
        InspectChannelQueryHandlerImpl handler = new InspectChannelQueryHandlerImpl(factory);

        StepVerifier.create(handler.handle(new InspectChannelQuery(url)))
                .expectErrorMatches(throwable ->
                        throwable instanceof UnsupportedPlatformException &&
                        ((UnsupportedPlatformException) throwable).getUrl().equals(url)
                )
                .verify();
    }
}
