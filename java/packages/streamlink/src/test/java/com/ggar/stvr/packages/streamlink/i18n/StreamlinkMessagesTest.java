package com.ggar.stvr.packages.streamlink.i18n;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class StreamlinkMessagesTest {

    @Test
    void testEnglishMessageResolution() {
        String msg = StreamlinkMessages.get("error.no_streams", Locale.ENGLISH, "https://twitch.tv/offline", "No streams found");
        assertThat(msg).isEqualTo("No playable streams found for URL 'https://twitch.tv/offline': No streams found");
    }

    @Test
    void testSpanishMessageResolution() {
        Locale spanish = Locale.forLanguageTag("es");
        String msg = StreamlinkMessages.get("error.no_streams", spanish, "https://twitch.tv/offline", "No streams found");
        assertThat(msg).isEqualTo("No se encontraron transmisiones reproducibles para la URL 'https://twitch.tv/offline': No streams found");
    }

    @Test
    void testExecutionFailedSpanish() {
        Locale spanish = Locale.forLanguageTag("es");
        String msg = StreamlinkMessages.get("error.execution_failed", spanish, 1, "command not found");
        assertThat(msg).isEqualTo("El proceso de Streamlink falló con código de salida 1: command not found");
    }

    @Test
    void testUrlBuilderMismatchBothLocales() {
        String msgEn = StreamlinkMessages.get("error.url_builder_mismatch", Locale.ENGLISH,
                "https://twitch.tv/streamer", "GenericCommandBuilder", "TwitchCommandBuilder");
        assertThat(msgEn).isEqualTo("URL 'https://twitch.tv/streamer' resolved to GenericCommandBuilder, but expected TwitchCommandBuilder");

        Locale spanish = Locale.forLanguageTag("es");
        String msgEs = StreamlinkMessages.get("error.url_builder_mismatch", spanish,
                "https://twitch.tv/streamer", "GenericCommandBuilder", "TwitchCommandBuilder");
        assertThat(msgEs).isEqualTo("La URL 'https://twitch.tv/streamer' se resolvió como GenericCommandBuilder, pero se esperaba TwitchCommandBuilder");
    }

    @Test
    void testNullArgBothLocales() {
        String msgEn = StreamlinkMessages.get("error.null_arg", Locale.ENGLISH, "command");
        assertThat(msgEn).isEqualTo("Argument 'command' must not be null");

        Locale spanish = Locale.forLanguageTag("es");
        String msgEs = StreamlinkMessages.get("error.null_arg", spanish, "command");
        assertThat(msgEs).isEqualTo("El argumento 'command' no puede ser nulo");
    }

    @Test
    void testMissingKeyReturnsKey() {
        String msg = StreamlinkMessages.get("unknown.key.code", Locale.ENGLISH);
        assertThat(msg).isEqualTo("unknown.key.code");
    }
}
