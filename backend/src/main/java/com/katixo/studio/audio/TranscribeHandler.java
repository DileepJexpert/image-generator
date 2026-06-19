package com.katixo.studio.audio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.asset.Asset;
import com.katixo.studio.asset.AssetService;
import com.katixo.studio.audio.AudioRequests.TranscribeRequest;
import com.katixo.studio.job.Job;
import com.katixo.studio.job.JobHandler;
import com.katixo.studio.job.JobService;
import com.katixo.studio.job.JobType;
import org.springframework.stereotype.Component;

/**
 * Executes {@link JobType#TRANSCRIBE}: audio asset -> faster-whisper -> the
 * transcript JSON stored as a {@code text} asset. Storing the result as an
 * asset lets transcription reuse the same job → asset → WebSocket pipeline as
 * every other result (no schema or WS-payload changes).
 */
@Component
public class TranscribeHandler implements JobHandler {

    private final WhisperClient whisperClient;
    private final AssetService assetService;
    private final JobService jobService;
    private final ObjectMapper objectMapper;

    public TranscribeHandler(WhisperClient whisperClient, AssetService assetService,
                             JobService jobService, ObjectMapper objectMapper) {
        this.whisperClient = whisperClient;
        this.assetService = assetService;
        this.jobService = jobService;
        this.objectMapper = objectMapper;
    }

    @Override
    public JobType type() {
        return JobType.TRANSCRIBE;
    }

    @Override
    public void handle(Job job) throws Exception {
        TranscribeRequest request =
                objectMapper.readValue(job.getParamsJson(), TranscribeRequest.class);
        Asset source = assetService.find(request.assetId())
                .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + request.assetId()));

        jobService.updateProgress(job.getId(), 10);
        byte[] transcriptJson =
                whisperClient.transcribe(assetService.readBytes(source), "audio.wav");
        jobService.updateProgress(job.getId(), 90);

        Asset result = assetService.saveText(transcriptJson, "application/json", job.getId());
        jobService.markDone(job.getId(), result.getId());
    }
}
