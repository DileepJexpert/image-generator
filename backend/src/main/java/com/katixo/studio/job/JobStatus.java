package com.katixo.studio.job;

/** Lifecycle state of a job. Stored as lowercase text. */
public enum JobStatus {
    QUEUED("queued"),
    RUNNING("running"),
    DONE("done"),
    FAILED("failed");

    private final String value;

    JobStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static JobStatus fromValue(String value) {
        for (JobStatus s : values()) {
            if (s.value.equals(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown job status: " + value);
    }
}
