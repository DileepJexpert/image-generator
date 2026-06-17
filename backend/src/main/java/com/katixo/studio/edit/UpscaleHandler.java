package com.katixo.studio.edit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.asset.Asset;
import com.katixo.studio.asset.AssetService;
import com.katixo.studio.edit.EditRequests.UpscaleRequest;
import com.katixo.studio.job.Job;
import com.katixo.studio.job.JobHandler;
import com.katixo.studio.job.JobService;
import com.katixo.studio.job.JobType;
import com.katixo.studio.media.EsrganClient;
import org.springframework.stereotype.Component;

/** Executes {@link JobType#UPSCALE}: source asset -> Real-ESRGAN -> new PNG asset. */
@Component
public class UpscaleHandler implements JobHandler {

    private final EsrganClient esrganClient;
    private final AssetService assetService;
    private final JobService jobService;
    private final ObjectMapper objectMapper;

    public UpscaleHandler(EsrganClient esrganClient, AssetService assetService,
                          JobService jobService, ObjectMapper objectMapper) {
        this.esrganClient = esrganClient;
        this.assetService = assetService;
        this.jobService = jobService;
        this.objectMapper = objectMapper;
    }

    @Override
    public JobType type() {
        return JobType.UPSCALE;
    }

    @Override
    public void handle(Job job) throws Exception {
        UpscaleRequest request = objectMapper.readValue(job.getParamsJson(), UpscaleRequest.class);
        Asset source = assetService.find(request.assetId())
                .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + request.assetId()));

        jobService.updateProgress(job.getId(), 10);
        byte[] output = esrganClient.upscale(assetService.readBytes(source), request.scale());
        jobService.updateProgress(job.getId(), 90);

        Asset result = assetService.saveImageResult(output, job.getId());
        jobService.markDone(job.getId(), result.getId());
    }
}
