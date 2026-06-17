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
 * Executes {@link JobType#IMAGE} jobs: resolve params, drive {@link ComfyUiClient},
 * persist the produced PNG as an {@link Asset}, then mark the job done.
 */
@Component
public class ImageGenerationHandler implements JobHandler {

    private static final String DEFAULT_CHECKPOINT = "sd_xl_base_1.0.safetensors";

    private final ComfyUiClient comfyUiClient;
    private final AssetService assetService;
    private final JobService jobService;
    private final ObjectMapper objectMapper;
    private final SecureRandom random = new SecureRandom();

    public ImageGenerationHandler(ComfyUiClient comfyUiClient,
                                  AssetService assetService,
                                  JobService jobService,
                                  ObjectMapper objectMapper) {
        this.comfyUiClient = comfyUiClient;
        this.assetService = assetService;
        this.jobService = jobService;
        this.objectMapper = objectMapper;
    }

    @Override
    public JobType type() {
        return JobType.IMAGE;
    }

    @Override
    public void handle(Job job) throws Exception {
        GenerateImageRequest request = objectMapper.readValue(job.getParamsJson(), GenerateImageRequest.class);

        long seed = request.seed() != null ? request.seed() : Math.abs(random.nextLong());
        String checkpoint = (request.model() != null && !request.model().isBlank())
                ? request.model()
                : DEFAULT_CHECKPOINT;

        ImageGenerationParams params = new ImageGenerationParams(
                request.prompt(),
                request.negativePrompt(),
                request.width(),
                request.height(),
                seed,
                checkpoint
        );

        ComfyImageResult result = comfyUiClient.generateImage(
                params,
                progress -> jobService.updateProgress(job.getId(), progress));

        Asset asset = assetService.saveImage(
                result.bytes(),
                result.mime(),
                job.getId(),
                request.width(),
                request.height());

        jobService.markDone(job.getId(), asset.getId());
    }
}
