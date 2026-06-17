package com.katixo.studio.generation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.job.Job;
import com.katixo.studio.job.JobQueue;
import com.katixo.studio.job.JobService;
import com.katixo.studio.job.JobType;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Submits generation work as async jobs: persist a queued job row, push its id
 * onto the Redis queue, return immediately (CLAUDE.md sections 4 and 9). The
 * request thread never blocks on a model.
 */
@Service
public class GenerationService {

    private final JobService jobService;
    private final JobQueue jobQueue;
    private final ObjectMapper objectMapper;

    public GenerationService(JobService jobService, JobQueue jobQueue, ObjectMapper objectMapper) {
        this.jobService = jobService;
        this.jobQueue = jobQueue;
        this.objectMapper = objectMapper;
    }

    public UUID submitImageJob(GenerateImageRequest request) {
        String paramsJson = toJson(request);
        Job job = jobService.create(JobType.IMAGE, paramsJson);
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
