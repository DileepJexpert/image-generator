package com.katixo.studio.generation;

/** Fully-resolved params handed to {@link ComfyUiClient} (seed + checkpoint filled in). */
public record ImageGenerationParams(
        String prompt,
        String negativePrompt,
        int width,
        int height,
        long seed,
        String checkpoint
) {
}
