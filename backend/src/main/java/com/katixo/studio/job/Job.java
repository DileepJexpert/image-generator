package com.katixo.studio.job;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * A unit of generation/edit work. Persistence-only; never serialized to the API.
 * See CLAUDE.md section 8 for the schema and section 9 for the lifecycle.
 */
@Entity
@Table(name = "jobs")
public class Job {

    @Id
    private UUID id;

    @Column(nullable = false)
    private JobType type;

    @Column(nullable = false)
    private JobStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "params_json")
    private String paramsJson;

    @Column(nullable = false)
    private int progress;

    @Column(name = "result_asset_id")
    private UUID resultAssetId;

    @Column
    private String error;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    protected Job() {
        // for JPA
    }

    public Job(UUID id, JobType type, JobStatus status, String paramsJson) {
        this.id = id;
        this.type = type;
        this.status = status;
        this.paramsJson = paramsJson;
        this.progress = 0;
    }

    public UUID getId() {
        return id;
    }

    public JobType getType() {
        return type;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public String getParamsJson() {
        return paramsJson;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public UUID getResultAssetId() {
        return resultAssetId;
    }

    public void setResultAssetId(UUID resultAssetId) {
        this.resultAssetId = resultAssetId;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
