package com.katixo.studio.lesson;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/v1/generate/lesson}. This is also what gets serialized into
 * {@code jobs.params_json} for the worker to replay.
 *
 * @param topic    what the lesson teaches, e.g. "the water cycle"
 * @param grade    primary-school grade (1..5); drives reading level and child-safety framing
 * @param language output language; blank → English
 * @param sections how many lesson sections to produce; null → a sensible default
 */
public record GenerateLessonRequest(
        @NotBlank String topic,
        @Min(1) @Max(5) int grade,
        String language,
        @Min(1) @Max(8) Integer sections
) {

    public String languageOrDefault() {
        return (language == null || language.isBlank()) ? "English" : language.trim();
    }

    public int sectionsOrDefault() {
        return sections == null ? 4 : sections;
    }
}
