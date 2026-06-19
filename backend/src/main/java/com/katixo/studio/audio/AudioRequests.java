package com.katixo.studio.audio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request payloads for the audio feature. */
public final class AudioRequests {

    private AudioRequests() {
    }

    /**
     * Text-to-speech (voiceover) generation. {@code voice} is optional — the
     * sidecar falls back to its default voice when blank.
     */
    public record GenerateSpeechRequest(
            @NotBlank @Size(max = 5000) String text,
            String voice
    ) {
    }
}
