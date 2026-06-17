package com.katixo.studio.job;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Owns the job lifecycle: create, transition status, update progress, finish.
 * Every state change persists the row and broadcasts a {@link JobDto} over the
 * job's WebSocket so subscribers see live progress (CLAUDE.md section 9).
 */
@Service
public class JobService {

    private final JobRepository repository;
    private final JobProgressSocket progressSocket;

    public JobService(JobRepository repository, JobProgressSocket progressSocket) {
        this.repository = repository;
        this.progressSocket = progressSocket;
    }

    @Transactional
    public Job create(JobType type, String paramsJson) {
        Job job = new Job(UUID.randomUUID(), type, JobStatus.QUEUED, paramsJson);
        Job saved = repository.save(job);
        progressSocket.broadcast(JobDto.from(saved));
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<Job> find(UUID id) {
        return repository.findById(id);
    }

    @Transactional
    public void markRunning(UUID id) {
        Job job = require(id);
        job.setStatus(JobStatus.RUNNING);
        job.setProgress(0);
        progressSocket.broadcast(JobDto.from(repository.save(job)));
    }

    @Transactional
    public void updateProgress(UUID id, int progress) {
        Job job = require(id);
        job.setProgress(Math.max(0, Math.min(100, progress)));
        progressSocket.broadcast(JobDto.from(repository.save(job)));
    }

    @Transactional
    public void markDone(UUID id, UUID resultAssetId) {
        Job job = require(id);
        job.setStatus(JobStatus.DONE);
        job.setProgress(100);
        job.setResultAssetId(resultAssetId);
        progressSocket.broadcast(JobDto.from(repository.save(job)));
    }

    @Transactional
    public void markFailed(UUID id, String error) {
        Job job = require(id);
        job.setStatus(JobStatus.FAILED);
        job.setError(error);
        progressSocket.broadcast(JobDto.from(repository.save(job)));
    }

    private Job require(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + id));
    }
}
