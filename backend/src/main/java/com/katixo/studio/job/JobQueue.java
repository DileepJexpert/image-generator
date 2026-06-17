package com.katixo.studio.job;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Redis-list-backed FIFO queue of job ids. Producers {@link #enqueue} on the
 * request thread; the single {@link JobWorker} blocks on {@link #dequeue}.
 */
@Component
public class JobQueue {

    private static final String QUEUE_KEY = "katixo:jobs:queue";

    private final StringRedisTemplate redis;

    public JobQueue(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void enqueue(UUID jobId) {
        redis.opsForList().rightPush(QUEUE_KEY, jobId.toString());
    }

    /** Blocking pop with timeout; returns null if nothing arrived in time. */
    public UUID dequeue(Duration timeout) {
        String value = redis.opsForList().leftPop(QUEUE_KEY, timeout);
        return value == null ? null : UUID.fromString(value);
    }
}
