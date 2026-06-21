-- Doc-AI extraction tables, merged into the Katixo monolith (formerly the standalone katixo-docai
-- service, which used ddl-auto). Column types match what Hibernate validates for these entities
-- (com.katixo.ai.persistence.ExtractionRecord / CallLog); timestamps use TIMESTAMPTZ like V1.

CREATE TABLE extraction_record (
    id                 VARCHAR(64)      PRIMARY KEY,
    doc_type           VARCHAR(16),
    model_path         VARCHAR(16),
    confidence         DOUBLE PRECISION NOT NULL,
    needs_human_review BOOLEAN          NOT NULL,
    result_json        TEXT,
    created_at         TIMESTAMPTZ
);

CREATE INDEX idx_record_review  ON extraction_record (needs_human_review);
CREATE INDEX idx_record_created ON extraction_record (created_at);

CREATE TABLE call_log (
    id                 VARCHAR(64)      PRIMARY KEY,
    record_id          TEXT,
    file_hash          VARCHAR(64),
    file_name          TEXT,
    model_path         VARCHAR(16),
    model_name         TEXT,
    prompt_version     VARCHAR(32),
    ocr_confidence     DOUBLE PRECISION NOT NULL,
    llm_latency_ms     BIGINT           NOT NULL,
    total_latency_ms   BIGINT           NOT NULL,
    ocr_text           TEXT,
    raw_model_output   TEXT,
    parsed_result_json TEXT,
    validation_outcome TEXT,
    created_at         TIMESTAMPTZ
);

CREATE INDEX idx_calllog_record ON call_log (record_id);
CREATE INDEX idx_calllog_hash   ON call_log (file_hash);
