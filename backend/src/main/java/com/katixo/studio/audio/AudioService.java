package com.katixo.studio.audio;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.audio.AudioRequests.GenerateSpeechRequest;
import com.katixo.studio.job.Job;
import com.katixo.studio.job.JobQueue;
import com.katixo.studio.job.JobService;
import com.katixo.studio.job.JobType;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Submits audio generation (TTS voiceover) as async jobs (CLAUDE.md §9). */
@Service
public class AudioService {

    private final JobService jobService;
    private final JobQueue jobQueue;
    private final ObjectMapper objectMapper;

    public AudioService(JobService jobService, JobQueue jobQueue, ObjectMapper objectMapper) {
        this.jobService = jobService;
        this.jobQueue = jobQueue;
        this.objectMapper = objectMapper;
    }

    public UUID submitSpeech(GenerateSpeechRequest request) {
        Job job = jobService.create(JobType.TEXT_TO_SPEECH, toJson(request));
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
