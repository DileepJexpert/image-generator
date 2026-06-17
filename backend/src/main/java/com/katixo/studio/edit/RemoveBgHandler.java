package com.katixo.studio.edit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.asset.Asset;
import com.katixo.studio.asset.AssetService;
import com.katixo.studio.edit.EditRequests.RemoveBgRequest;
import com.katixo.studio.job.Job;
import com.katixo.studio.job.JobHandler;
import com.katixo.studio.job.JobService;
import com.katixo.studio.job.JobType;
import com.katixo.studio.media.RembgClient;
import org.springframework.stereotype.Component;

/** Executes {@link JobType#REMOVE_BG}: source asset -> rembg -> new PNG asset. */
@Component
public class RemoveBgHandler implements JobHandler {

    private final RembgClient rembgClient;
    private final AssetService assetService;
    private final JobService jobService;
    private final ObjectMapper objectMapper;

    public RemoveBgHandler(RembgClient rembgClient, AssetService assetService,
                           JobService jobService, ObjectMapper objectMapper) {
        this.rembgClient = rembgClient;
        this.assetService = assetService;
        this.jobService = jobService;
        this.objectMapper = objectMapper;
    }

    @Override
    public JobType type() {
        return JobType.REMOVE_BG;
    }

    @Override
    public void handle(Job job) throws Exception {
        RemoveBgRequest request = objectMapper.readValue(job.getParamsJson(), RemoveBgRequest.class);
        Asset source = assetService.find(request.assetId())
                .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + request.assetId()));

        jobService.updateProgress(job.getId(), 10);
        byte[] output = rembgClient.removeBackground(assetService.readBytes(source));
        jobService.updateProgress(job.getId(), 90);

        Asset result = assetService.saveImageResult(output, job.getId());
        jobService.markDone(job.getId(), result.getId());
    }
}
