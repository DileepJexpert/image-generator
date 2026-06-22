package com.katixo.studio.lesson;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
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
 * asserts a valid PDF comes out. Also pins the script-routing contract: Latin text stays on the
 * crisp Helvetica text path (no image XObjects), while Devanagari is rendered through the shaped
 * Java2D image path (≥1 image XObject) so Hindi worksheets are not silently dropped.
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

    /** Count image XObjects across all pages — section illustrations and shaped text-line rasters. */
    private static int imageXObjects(byte[] pdf) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            int n = 0;
            for (PDPage page : doc.getPages()) {
                PDResources res = page.getResources();
                if (res == null) {
                    continue;
                }
                for (COSName name : res.getXObjectNames()) {
                    if (res.getXObject(name) instanceof PDImageXObject) {
                        n++;
                    }
                }
            }
            return n;
        }
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
        // Exactly the two section illustrations — all the English text stayed on the text path.
        assertThat(imageXObjects(pdf)).isEqualTo(2);
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
    void englishTextOnlyUsesTextPathWithNoImages() throws Exception {
        Lesson lesson = new Lesson("Counting", "Maths", 1, "English",
                List.of(new Lesson.Section("Numbers", "We can count one, two, three.", "p")),
                List.of(new Lesson.QuizItem("How many?", List.of("Two", "Three"), "Three")));

        byte[] pdf = renderer.render(lesson, null);

        assertThat(header(pdf)).isEqualTo("%PDF-");
        assertThat(imageXObjects(pdf)).isZero();
    }

    @Test
    void hindiLessonIsRenderedViaShapedImagePath() throws Exception {
        // Devanagari literals (UTF-8 source); transliteration in trailing comments.
        String title = "जल चक्र";          // "jal chakra" (water cycle)
        String subject = "विज्ञान";   // "vigyaan" (science)
        String heading = "वाष्पीकरण"; // "vaashpeekaran" (evaporation)
        String bodyText = "सूरज पानी "
                + "को गरम करता है।"; // "...garam karta hai."
        String question = "नमस्ते दुनिया"; // "namaste duniya"
        String yes = "हाँ";          // "haan" (yes)
        String no = "नहीं";     // "nahin" (no)

        Lesson lesson = new Lesson(title, subject, 3, "Hindi",
                List.of(new Lesson.Section(heading, bodyText, "the sun over a lake")),
                List.of(new Lesson.QuizItem(question, List.of(yes, no), yes)));

        byte[] pdf = renderer.render(lesson, null);

        assertThat(header(pdf)).isEqualTo("%PDF-");
        // No section illustration supplied, so every image here is a shaped Devanagari text line.
        assertThat(imageXObjects(pdf)).isGreaterThanOrEqualTo(1);
    }

    @Test
    void rendersNonLatinPunctuationAndEmojiWithoutThrowing() throws Exception {
        Lesson lesson = new Lesson("Smart “quotes” — café 😀", "S", 2, "English",
                List.of(new Lesson.Section("He’llo",
                        "Body with an emoji 😀, a dash —, and accents café.", "p")),
                List.of());

        byte[] pdf = renderer.render(lesson, null);

        assertThat(header(pdf)).isEqualTo("%PDF-");
    }
}
