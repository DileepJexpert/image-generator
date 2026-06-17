package com.katixo.studio.generation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/**
 * Request body for {@code POST /api/v1/generate/image-to-video}. Serialized into
 * {@code jobs.params_json} for the worker to replay.
 */
public record ImageToVideoRequest(
        @NotNull UUID sourceAssetId,
        String prompt,
        @Positive @Max(10) int durationSeconds
) {
}
