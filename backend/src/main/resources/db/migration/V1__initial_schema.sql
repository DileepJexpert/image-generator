-- Katixo Studio initial schema (Milestone 1).
-- Three core tables: projects, assets, jobs. See CLAUDE.md section 8.

CREATE TABLE projects (
    id            UUID PRIMARY KEY,
    name          TEXT        NOT NULL,
    canvas_width  INTEGER     NOT NULL,
    canvas_height INTEGER     NOT NULL,
    scene_json    JSONB,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE jobs (
    id              UUID PRIMARY KEY,
    type            TEXT        NOT NULL,   -- image | image_to_video | remove_bg | upscale
    status          TEXT        NOT NULL,   -- queued | running | done | failed
    params_json     JSONB,
    progress        INTEGER     NOT NULL DEFAULT 0,
    result_asset_id UUID,
    error           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE assets (
    id            UUID PRIMARY KEY,
    type          TEXT        NOT NULL,     -- image | video
    file_path     TEXT        NOT NULL,
    mime          TEXT,
    width         INTEGER,
    height        INTEGER,
    source_job_id UUID,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_assets_source_job
        FOREIGN KEY (source_job_id) REFERENCES jobs (id) ON DELETE SET NULL
);

-- jobs.result_asset_id references assets, added after assets exists.
ALTER TABLE jobs
    ADD CONSTRAINT fk_jobs_result_asset
        FOREIGN KEY (result_asset_id) REFERENCES assets (id) ON DELETE SET NULL;

CREATE INDEX idx_assets_source_job ON assets (source_job_id);
CREATE INDEX idx_jobs_status ON jobs (status);
