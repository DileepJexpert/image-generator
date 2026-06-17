package com.katixo.studio.job;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The single background worker (CLAUDE.md section 9: concurrency = 1, since one
 * 12GB GPU can't run parallel jobs). Blocks on the Redis queue, then dispatches
 * each job to the {@link JobHandler} registered for its type.
 */
@Component
public class JobWorker {

    private static final Logger log = LoggerFactory.getLogger(JobWorker.class);
    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(2);

    private final JobQueue queue;
    private final JobService jobService;
    private final Map<JobType, JobHandler> handlers = new EnumMap<>(JobType.class);

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread thread;

    public JobWorker(JobQueue queue, JobService jobService, List<JobHandler> handlerList) {
        this.queue = queue;
        this.jobService = jobService;
        for (JobHandler handler : handlerList) {
            handlers.put(handler.type(), handler);
        }
    }

    @PostConstruct
    void start() {
        running.set(true);
        thread = new Thread(this::loop, "katixo-job-worker");
        thread.setDaemon(true);
        thread.start();
        log.info("Job worker started (handlers: {})", handlers.keySet());
    }

    @PreDestroy
    void stop() {
        running.set(false);
        if (thread != null) {
            thread.interrupt();
        }
    }

    private void loop() {
        while (running.get()) {
            try {
                UUID jobId = queue.dequeue(POLL_TIMEOUT);
                if (jobId != null) {
                    process(jobId);
                }
            } catch (Exception e) {
                if (running.get()) {
                    log.error("Job worker loop error", e);
                }
            }
        }
    }

    private void process(UUID jobId) {
        Job job = jobService.find(jobId).orElse(null);
        if (job == null) {
            log.warn("Dequeued unknown job {}", jobId);
            return;
        }
        JobHandler handler = handlers.get(job.getType());
        if (handler == null) {
            jobService.markFailed(jobId, "No handler for job type " + job.getType());
            return;
        }
        log.info("Processing job {} ({})", jobId, job.getType().value());
        jobService.markRunning(jobId);
        try {
            handler.handle(job);
            log.info("Job {} completed", jobId);
        } catch (Exception e) {
            log.error("Job {} failed", jobId, e);
            jobService.markFailed(jobId, e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }
}
