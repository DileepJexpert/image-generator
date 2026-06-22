package com.katixo.studio.lesson;

import com.katixo.studio.copilot.ChatMessage;

import java.util.List;

/**
 * Builds the chat prompt for lesson generation. The system message pins the model to child-safe,
 * grade-appropriate content and a strict JSON shape that {@link LessonParser} can read back.
 */
final class LessonPrompt {

    private LessonPrompt() {
    }

    static List<ChatMessage> build(GenerateLessonRequest req) {
        String system = """
                You are a primary-school teacher creating lessons for children in grades 1 to 5.
                Write in simple, warm, age-appropriate language for the requested grade.
                Keep EVERYTHING safe for young children: no violence, no scary, adult, political, or
                unsafe content. For each section include an "imagePrompt": a short, concrete, wholesome
                description of ONE illustration for that section, in a friendly cartoon/clip-art style a
                child would enjoy.
                Respond with ONLY a JSON object — no markdown, no code fences, no commentary — in exactly
                this shape:
                {
                  "title": "string",
                  "subject": "string",
                  "grade": <integer 1-5>,
                  "language": "string",
                  "sections": [ { "heading": "string", "body": "string", "imagePrompt": "string" } ],
                  "quiz": [ { "question": "string", "options": ["string", "string"], "answer": "string" } ]
                }
                """;
        String user = ("Create a lesson in %s for grade %d about: %s. "
                + "Produce exactly %d sections and a short quiz of 3 to 5 questions. "
                + "Each section body should be 2 to 4 short sentences a grade-%d child can read.")
                .formatted(req.languageOrDefault(), req.grade(), req.topic(),
                        req.sectionsOrDefault(), req.grade());
        return List.of(ChatMessage.system(system), new ChatMessage("user", user));
    }
}
