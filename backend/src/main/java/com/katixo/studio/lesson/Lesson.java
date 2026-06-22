package com.katixo.studio.lesson;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * A structured, grade-appropriate lesson produced by the local LLM. This is the keystone artifact of
 * the lesson feature: the later milestones (illustrations, PDF worksheet, narrated video, quiz) are
 * all rendered from this object. It is persisted as a downloadable JSON {@code text} asset.
 *
 * <p>{@code imagePrompt} on each section is the wholesome, concrete illustration description the
 * image milestone will feed to ComfyUI.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Lesson(
        String title,
        String subject,
        int grade,
        String language,
        List<Section> sections,
        List<QuizItem> quiz
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Section(String heading, String body, String imagePrompt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QuizItem(String question, List<String> options, String answer) {
    }
}
