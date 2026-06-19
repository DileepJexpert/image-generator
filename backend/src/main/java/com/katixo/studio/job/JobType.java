package com.katixo.studio.job;

/** Kind of generation/edit work a job performs. Stored as lowercase text. */
public enum JobType {
    IMAGE("image"),
    IMAGE_TO_VIDEO("image_to_video"),
    REMOVE_BG("remove_bg"),
    UPSCALE("upscale"),
    TEXT_TO_SPEECH("text_to_speech"),
    TRANSCRIBE("transcribe"),
    LEAD_SCRAPE("lead_scrape");

    private final String value;

    JobType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static JobType fromValue(String value) {
        for (JobType t : values()) {
            if (t.value.equals(value)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown job type: " + value);
    }
}
