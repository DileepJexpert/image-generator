package com.katixo.studio.lesson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.job.Job;
import com.katixo.studio.job.JobQueue;
import com.katixo.studio.job.JobService;
import com.katixo.studio.job.JobType;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Submits lesson generation as an async job (CLAUDE.md §4/§9): persist a queued job row, push its id
 * onto the Redis queue, and return immediately. The request thread never blocks on the model.
 */
@Service
public class LessonService {

    private final JobService jobService;
    private final JobQueue jobQueue;
    private final ObjectMapper objectMapper;

    public LessonService(JobService jobService, JobQueue jobQueue, ObjectMapper objectMapper) {
        this.jobService = jobService;
        this.jobQueue = jobQueue;
        this.objectMapper = objectMapper;
    }

    public UUID submitLessonJob(GenerateLessonRequest request) {
        Job job = jobService.create(JobType.LESSON, toJson(request));
        jobQueue.enqueue(job.getId());
        return job.getId();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize lesson job params", e);
        }
    }
}
