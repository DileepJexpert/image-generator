package com.katixo.studio.lesson;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * Turns the local LLM's raw text response into a validated {@link Lesson}. Small local models
 * sometimes wrap their JSON in ```code fences``` or add a sentence of preamble/epilogue, so we
 * defensively slice out the outermost JSON object before parsing. Kept separate from the handler so
 * it is unit-testable without a live model.
 */
@Component
public class LessonParser {

    private final ObjectMapper mapper;

    public LessonParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * @throws IllegalArgumentException if the output contains no JSON object, the JSON is malformed,
     *                                  or the lesson is structurally empty (no title / no sections)
     */
    public Lesson parse(String rawModelOutput) {
        String json = extractJsonObject(rawModelOutput);
        Lesson lesson;
        try {
            lesson = mapper.readValue(json, Lesson.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Model did not return valid lesson JSON", e);
        }
        if (lesson.title() == null || lesson.title().isBlank()
                || lesson.sections() == null || lesson.sections().isEmpty()) {
            throw new IllegalArgumentException("Lesson JSON is missing a title or sections");
        }
        return lesson;
    }

    /** Slice from the first '{' to the last '}', dropping any fences/preamble around it. */
    private String extractJsonObject(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Model returned an empty response");
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("No JSON object found in model output");
        }
        return raw.substring(start, end + 1);
    }
}
