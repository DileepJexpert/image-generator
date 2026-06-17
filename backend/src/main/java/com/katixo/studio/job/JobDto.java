package com.katixo.studio.job;

import java.util.UUID;

/**
 * API + WebSocket representation of a job. This is the payload pushed over
 * {@code /ws/jobs/{jobId}} and returned by {@code GET /api/v1/jobs/{jobId}}.
 */
public record JobDto(
        UUID id,
        String type,
        String status,
        int progress,
        UUID resultAssetId,
        String error
) {
    public static JobDto from(Job job) {
        return new JobDto(
                job.getId(),
                job.getType().value(),
                job.getStatus().value(),
                job.getProgress(),
                job.getResultAssetId(),
                job.getError()
        );
    }
}
