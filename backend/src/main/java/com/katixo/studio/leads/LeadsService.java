package com.katixo.studio.leads;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.job.Job;
import com.katixo.studio.job.JobQueue;
import com.katixo.studio.job.JobService;
import com.katixo.studio.job.JobType;
import com.katixo.studio.leads.LeadsDtos.ScrapeLeadsRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Submits lead-generation (scrape + outreach) as async jobs (CLAUDE.md §9). */
@Service
public class LeadsService {

    private final JobService jobService;
    private final JobQueue jobQueue;
    private final ObjectMapper objectMapper;

    public LeadsService(JobService jobService, JobQueue jobQueue, ObjectMapper objectMapper) {
        this.jobService = jobService;
        this.jobQueue = jobQueue;
        this.objectMapper = objectMapper;
    }

    public UUID submitScrape(ScrapeLeadsRequest request) {
        Job job = jobService.create(JobType.LEAD_SCRAPE, toJson(request));
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
