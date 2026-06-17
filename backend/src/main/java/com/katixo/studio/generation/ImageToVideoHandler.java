package com.katixo.studio.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.asset.Asset;
import com.katixo.studio.asset.AssetService;
import com.katixo.studio.job.Job;
import com.katixo.studio.job.JobHandler;
import com.katixo.studio.job.JobService;
import com.katixo.studio.job.JobType;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Executes {@link JobType#IMAGE_TO_VIDEO}: take a source image asset, drive the
 * ComfyUI LTX image-to-video workflow, persist the clip, mark the job done.
 */
@Component
public class ImageToVideoHandler implements JobHandler {

    private static final int FPS = 24;
    private static final int MAX_DIMENSION = 768;

    private final ComfyUiClient comfyUiClient;
    private final AssetService assetService;
    private final JobService jobService;
    private final ObjectMapper objectMapper;
    private final SecureRandom random = new SecureRandom();

    public ImageToVideoHandler(ComfyUiClient comfyUiClient, AssetService assetService,
                               JobService jobService, ObjectMapper objectMapper) {
        this.comfyUiClient = comfyUiClient;
        this.assetService = assetService;
        this.jobService = jobService;
        this.objectMapper = objectMapper;
    }

    @Override
    public JobType type() {
        return JobType.IMAGE_TO_VIDEO;
    }

    @Override
    public void handle(Job job) throws Exception {
        ImageToVideoRequest request =
                objectMapper.readValue(job.getParamsJson(), ImageToVideoRequest.class);
        Asset source = assetService.find(request.sourceAssetId())
                .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + request.sourceAssetId()));

        int width = videoDimension(source.getWidth(), 768);
        int height = videoDimension(source.getHeight(), 512);
        // LTX expects frame counts of the form 8n+1.
        int frames = Math.max(9, roundTo8nPlus1(request.durationSeconds() * FPS));
        long seed = Math.abs(random.nextLong());

        VideoGenerationParams params = new VideoGenerationParams(
                assetService.readBytes(source),
                request.prompt(),
                "",
                width,
                height,
                frames,
                FPS,
                seed);

        ComfyVideoResult result = comfyUiClient.generateVideo(
                params,
                progress -> jobService.updateProgress(job.getId(), progress));

        Asset asset = assetService.saveVideo(result.bytes(), result.mime(), job.getId(), width, height);
        jobService.markDone(job.getId(), asset.getId());
    }

    /** Round a source dimension to a multiple of 32, capped, with a sensible default. */
    private int videoDimension(Integer source, int fallback) {
        int value = (source == null || source <= 0) ? fallback : source;
        value = Math.min(value, MAX_DIMENSION);
        int rounded = Math.round(value / 32f) * 32;
        return Math.max(32, rounded);
    }

    private int roundTo8nPlus1(int frames) {
        int n = Math.round((frames - 1) / 8f);
        return n * 8 + 1;
    }
}
