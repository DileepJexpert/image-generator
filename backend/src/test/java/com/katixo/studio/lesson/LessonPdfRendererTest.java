package com.katixo.studio.lesson;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Offline test for the PDF worksheet rendering — generates a real in-memory PNG (no ComfyUI) and
 * asserts a valid PDF comes out. Also proves the sanitiser keeps PDFBox from throwing on non-Latin
 * input.
 */
class LessonPdfRendererTest {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    private final LessonPdfRenderer renderer = new LessonPdfRenderer();

    private static byte[] tinyPng() throws Exception {
        BufferedImage img = new BufferedImage(64, 48, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.CYAN);
        g.fillRect(0, 0, 64, 48);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    private static String header(byte[] pdf) {
        return new String(pdf, 0, 5, StandardCharsets.US_ASCII);
    }

    @Test
    void rendersWorksheetWithImagesAndQuiz() throws Exception {
        byte[] png = tinyPng();
        Lesson lesson = new Lesson("The Water Cycle", "Science", 3, "English",
                List.of(
                        new Lesson.Section("Evaporation", "The sun warms the water. It rises as vapour.",
                                "a smiling sun over a lake"),
                        new Lesson.Section("Rain", "Clouds get heavy. Rain falls down.", "rain clouds")),
                List.of(new Lesson.QuizItem("What warms the water?",
                        List.of("The sun", "The moon"), "The sun")));

        byte[] pdf = renderer.render(lesson, List.of(png, png));

        assertThat(pdf).isNotEmpty();
        assertThat(header(pdf)).isEqualTo("%PDF-");
    }

    @Test
    void rendersTextOnlyWhenImagesMissing() throws Exception {
        Lesson lesson = new Lesson("Counting", "Maths", 1, "English",
                List.of(new Lesson.Section("Numbers", "We can count one, two, three.", "p")),
                List.of());

        // A null entry (illustration skipped) and a null list must both be tolerated.
        assertThat(header(renderer.render(lesson, Arrays.asList((byte[]) null)))).isEqualTo("%PDF-");
        assertThat(header(renderer.render(lesson, null))).isEqualTo("%PDF-");
    }

    @Test
    void sanitisesNonLatinTextWithoutThrowing() throws Exception {
        Lesson lesson = new Lesson("Smart “quotes” — café 😀", "S", 2, "English",
                List.of(new Lesson.Section("He’llo",
                        "Body with an emoji 😀, a dash —, and accents café.", "p")),
                List.of());

        byte[] pdf = renderer.render(lesson, null);

        assertThat(header(pdf)).isEqualTo("%PDF-");
    }
}
