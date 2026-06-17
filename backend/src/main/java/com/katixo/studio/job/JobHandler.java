package com.katixo.studio.job;

/**
 * Strategy that executes one kind of job. Implementations live in their feature
 * package (e.g. generation), are discovered as Spring beans, and are dispatched
 * by {@link JobWorker} keyed on {@link #type()}. A handler drives its media
 * client, reports progress via {@link JobService#updateProgress}, and on success
 * calls {@link JobService#markDone}. Throwing marks the job failed.
 */
public interface JobHandler {

    JobType type();

    void handle(Job job) throws Exception;
}
