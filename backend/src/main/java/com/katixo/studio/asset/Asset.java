package com.katixo.studio.asset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Metadata for a stored media file. The bytes live behind the
 * {@link AssetStorage} abstraction; {@code filePath} is the storage key.
 * Persistence-only; never serialized to the API.
 */
@Entity
@Table(name = "assets")
public class Asset {

    @Id
    private UUID id;

    @Column(nullable = false)
    private AssetType type;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column
    private String mime;

    @Column
    private Integer width;

    @Column
    private Integer height;

    @Column(name = "source_job_id")
    private UUID sourceJobId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    protected Asset() {
        // for JPA
    }

    public Asset(UUID id, AssetType type, String filePath, String mime,
                 Integer width, Integer height, UUID sourceJobId) {
        this.id = id;
        this.type = type;
        this.filePath = filePath;
        this.mime = mime;
        this.width = width;
        this.height = height;
        this.sourceJobId = sourceJobId;
    }

    public UUID getId() {
        return id;
    }

    public AssetType getType() {
        return type;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getMime() {
        return mime;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public UUID getSourceJobId() {
        return sourceJobId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
