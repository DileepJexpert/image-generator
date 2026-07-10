package com.katixo.studio.code;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.job.Job;
import com.katixo.studio.job.JobQueue;
import com.katixo.studio.job.JobService;
import com.katixo.studio.job.JobType;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Submits approved code/test commands as async jobs with live WebSocket logs. */
@Service
public class CodeCommandService {

    private final JobService jobService;
    private final JobQueue jobQueue;
    private final ObjectMapper objectMapper;
    private final WorkspaceCheckStatus checkStatus;

    public CodeCommandService(JobService jobService, JobQueue jobQueue, ObjectMapper objectMapper,
                              WorkspaceCheckStatus checkStatus) {
        this.jobService = jobService;
        this.jobQueue = jobQueue;
        this.objectMapper = objectMapper;
        this.checkStatus = checkStatus;
    }

    public UUID submit(CodeCommandRequest request) {
        Job job = jobService.create(JobType.CODE_COMMAND, toJson(request));
        checkStatus.recordCheckStarted(request.command());
        jobQueue.enqueue(job.getId());
        return job.getId();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize command job params", e);
        }
    }
}
