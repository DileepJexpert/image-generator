package com.katixo.studio.generation;

/** Fully-resolved params handed to {@link ComfyUiClient#generateVideo}. */
public record VideoGenerationParams(
        byte[] sourceImage,
        String prompt,
        String negativePrompt,
        int width,
        int height,
        int frames,
        int fps,
        long seed
) {
}
