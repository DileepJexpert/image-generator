package com.katixo.studio.generation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Request body for {@code POST /api/v1/generate/image}. This is also what gets
 * serialized into {@code jobs.params_json} for the worker to replay.
 */
public record GenerateImageRequest(
        @NotBlank String prompt,
        String negativePrompt,
        @Positive @Max(2048) int width,
        @Positive @Max(2048) int height,
        String model,
        Long seed
) {
}
