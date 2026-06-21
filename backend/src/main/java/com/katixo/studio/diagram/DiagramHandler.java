package com.katixo.studio.diagram;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.asset.Asset;
import com.katixo.studio.asset.AssetService;
import com.katixo.studio.job.Job;
import com.katixo.studio.job.JobHandler;
import com.katixo.studio.job.JobService;
import com.katixo.studio.job.JobType;
import org.springframework.stereotype.Component;

/**
 * Executes {@link JobType#DIAGRAM} jobs: render the PlantUML source to a PNG (in-process), persist it
 * as an image {@link Asset}, then mark the job done — so the diagram appears on the canvas exactly
 * like a generated image.
 */
@Component
public class DiagramHandler implements JobHandler {

    private final DiagramRenderer renderer;
    private final AssetService assetService;
    private final JobService jobService;
    private final ObjectMapper objectMapper;

    public DiagramHandler(DiagramRenderer renderer, AssetService assetService,
                          JobService jobService, ObjectMapper objectMapper) {
        this.renderer = renderer;
        this.assetService = assetService;
        this.jobService = jobService;
        this.objectMapper = objectMapper;
    }

    @Override
    public JobType type() {
        return JobType.DIAGRAM;
    }

    @Override
    public void handle(Job job) throws Exception {
        RenderDiagramRequest request =
                objectMapper.readValue(job.getParamsJson(), RenderDiagramRequest.class);

        jobService.updateProgress(job.getId(), 20);
        byte[] png = renderer.renderPng(request.source());
        jobService.updateProgress(job.getId(), 90);

        Asset asset = assetService.saveImageResult(png, job.getId());
        jobService.markDone(job.getId(), asset.getId());
    }
}
