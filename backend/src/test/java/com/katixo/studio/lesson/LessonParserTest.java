package com.katixo.studio.lesson;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Offline unit test for the defensive JSON extraction + lesson validation — no live model needed.
 */
class LessonParserTest {

    private final LessonParser parser = new LessonParser(new ObjectMapper());

    private static final String CLEAN = """
            {"title":"The Water Cycle","subject":"Science","grade":3,"language":"English",
            "sections":[{"heading":"Evaporation","body":"The sun warms the water. It rises as vapour.",
            "imagePrompt":"a smiling sun warming a blue lake, cartoon clip-art"}],
            "quiz":[{"question":"What warms the water?","options":["The sun","The moon"],"answer":"The sun"}]}
            """;

    @Test
    void parsesCleanJson() {
        Lesson lesson = parser.parse(CLEAN);
        assertThat(lesson.title()).isEqualTo("The Water Cycle");
        assertThat(lesson.grade()).isEqualTo(3);
        assertThat(lesson.sections()).hasSize(1);
        assertThat(lesson.sections().get(0).imagePrompt()).contains("sun");
        assertThat(lesson.quiz()).hasSize(1);
        assertThat(lesson.quiz().get(0).answer()).isEqualTo("The sun");
    }

    @Test
    void stripsCodeFencesAndPreamble() {
        String fenced = "Sure! Here is your lesson:\n```json\n" + CLEAN + "\n```\nHope it helps!";
        Lesson lesson = parser.parse(fenced);
        assertThat(lesson.title()).isEqualTo("The Water Cycle");
        assertThat(lesson.sections()).hasSize(1);
    }

    @Test
    void rejectsOutputWithoutJson() {
        assertThatThrownBy(() -> parser.parse("I'm sorry, I can't do that."))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsLessonMissingSections() {
        assertThatThrownBy(() -> parser.parse("{\"title\":\"X\",\"sections\":[]}"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
