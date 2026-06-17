package com.katixo.studio.edit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.edit.EditRequests.RemoveBgRequest;
import com.katixo.studio.edit.EditRequests.UpscaleRequest;
import com.katixo.studio.job.Job;
import com.katixo.studio.job.JobQueue;
import com.katixo.studio.job.JobService;
import com.katixo.studio.job.JobType;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Submits edit work (remove-bg, upscale) as async jobs (CLAUDE.md §9). */
@Service
public class EditService {

    private final JobService jobService;
    private final JobQueue jobQueue;
    private final ObjectMapper objectMapper;

    public EditService(JobService jobService, JobQueue jobQueue, ObjectMapper objectMapper) {
        this.jobService = jobService;
        this.jobQueue = jobQueue;
        this.objectMapper = objectMapper;
    }

    public UUID submitRemoveBg(RemoveBgRequest request) {
        return submit(JobType.REMOVE_BG, request);
    }

    public UUID submitUpscale(UpscaleRequest request) {
        if (request.scale() != 2 && request.scale() != 4) {
            throw new IllegalArgumentException("scale must be 2 or 4");
        }
        return submit(JobType.UPSCALE, request);
    }

    private UUID submit(JobType type, Object params) {
        Job job = jobService.create(type, toJson(params));
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
