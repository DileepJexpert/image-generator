package com.katixo.studio.audio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.asset.Asset;
import com.katixo.studio.asset.AssetService;
import com.katixo.studio.audio.AudioRequests.GenerateSpeechRequest;
import com.katixo.studio.job.Job;
import com.katixo.studio.job.JobHandler;
import com.katixo.studio.job.JobService;
import com.katixo.studio.job.JobType;
import org.springframework.stereotype.Component;

/** Executes {@link JobType#TEXT_TO_SPEECH}: text -> Piper TTS -> WAV audio asset. */
@Component
public class SpeechGenerationHandler implements JobHandler {

    private final TtsClient ttsClient;
    private final AssetService assetService;
    private final JobService jobService;
    private final ObjectMapper objectMapper;

    public SpeechGenerationHandler(TtsClient ttsClient, AssetService assetService,
                                   JobService jobService, ObjectMapper objectMapper) {
        this.ttsClient = ttsClient;
        this.assetService = assetService;
        this.jobService = jobService;
        this.objectMapper = objectMapper;
    }

    @Override
    public JobType type() {
        return JobType.TEXT_TO_SPEECH;
    }

    @Override
    public void handle(Job job) throws Exception {
        GenerateSpeechRequest request =
                objectMapper.readValue(job.getParamsJson(), GenerateSpeechRequest.class);

        jobService.updateProgress(job.getId(), 10);
        byte[] wav = ttsClient.synthesize(request.text(), request.voice());
        jobService.updateProgress(job.getId(), 90);

        Asset result = assetService.saveAudio(wav, "audio/wav", job.getId());
        jobService.markDone(job.getId(), result.getId());
    }
}
