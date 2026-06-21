package com.katixo.studio.diagram;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.job.Job;
import com.katixo.studio.job.JobQueue;
import com.katixo.studio.job.JobService;
import com.katixo.studio.job.JobType;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Submits diagram rendering as an async job — same pattern as image generation, so the result lands
 * on the canvas through the existing job → asset → WebSocket pipeline (CLAUDE.md §4, §9). Rendering
 * itself is fast and not GPU work, but routing it through the job system keeps canvas placement
 * uniform with every other generated asset.
 */
@Service
public class DiagramService {

    private final JobService jobService;
    private final JobQueue jobQueue;
    private final ObjectMapper objectMapper;

    public DiagramService(JobService jobService, JobQueue jobQueue, ObjectMapper objectMapper) {
        this.jobService = jobService;
        this.jobQueue = jobQueue;
        this.objectMapper = objectMapper;
    }

    public UUID submitDiagramJob(RenderDiagramRequest request) {
        Job job = jobService.create(JobType.DIAGRAM, toJson(request));
        jobQueue.enqueue(job.getId());
        return job.getId();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize job params", e);
        }
    }
}
