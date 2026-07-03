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
        String error,
        String logLine
) {
    public static JobDto from(Job job) {
        return new JobDto(
                job.getId(),
                job.getType().value(),
                job.getStatus().value(),
                job.getProgress(),
                job.getResultAssetId(),
                job.getError(),
                null
        );
    }

    public static JobDto log(Job job, String line) {
        return new JobDto(
                job.getId(),
                job.getType().value(),
                job.getStatus().value(),
                job.getProgress(),
                job.getResultAssetId(),
                job.getError(),
                line
        );
    }
}
