package com.katixo.studio.lesson;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders a {@link Lesson} (plus optional per-section illustrations) into a printable A4 PDF
 * worksheet using PDFBox — in-process, no sidecar, no network. Text is laid out with manual
 * word-wrap and pagination; each section image is scaled to fit the content width.
 *
 * <p>Fonts are the Standard-14 (Helvetica) set, which only encode Latin (WinAnsi) text, so all
 * content is sanitised to a safe ASCII range before drawing (otherwise PDFBox throws on smart quotes,
 * emoji, accented or non-Latin characters). Non-Latin scripts — e.g. Devanagari for Hindi lessons —
 * would need an embedded TrueType font; that is a planned follow-up. The full Unicode text is always
 * preserved in the lesson JSON regardless of what the PDF can render.
 */
@Component
public class LessonPdfRenderer {

    private static final float MARGIN = 50f;
    private static final float LEADING = 4f;          // extra gap below each text line
    private static final float BLOCK_GAP = 12f;       // gap between blocks (section/heading/image)
    private static final float IMAGE_WIDTH_FRACTION = 0.6f;
    private static final float MAX_IMAGE_HEIGHT = 240f;

    private final PDFont body = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    /**
     * @param sectionImages PNG bytes aligned to {@code lesson.sections()} (an entry may be
     *                      {@code null}, or the whole list may be {@code null}, when no illustration
     *                      is available)
     */
    public byte[] render(Lesson lesson, List<byte[]> sectionImages) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            Cursor cur = new Cursor(doc);

            cur.text(safe(lesson.title()), bold, 20f);
            String subtitle = subtitle(lesson);
            if (!subtitle.isBlank()) {
                cur.text(subtitle, body, 11f);
            }
            cur.gap(BLOCK_GAP);

            List<Lesson.Section> sections = lesson.sections() == null ? List.of() : lesson.sections();
            for (int i = 0; i < sections.size(); i++) {
                Lesson.Section s = sections.get(i);
                cur.text(safe((i + 1) + ".  " + nullToEmpty(s.heading())), bold, 14f);
                if (s.body() != null && !s.body().isBlank()) {
                    cur.text(safe(s.body()), body, 11f);
                }
                byte[] img = (sectionImages != null && i < sectionImages.size()) ? sectionImages.get(i) : null;
                if (img != null && img.length > 0) {
                    cur.image(img, "sec" + i);
                }
                cur.gap(BLOCK_GAP);
            }

            if (lesson.quiz() != null && !lesson.quiz().isEmpty()) {
                cur.gap(BLOCK_GAP);
                cur.text("Quiz", bold, 16f);
                int q = 1;
                for (Lesson.QuizItem item : lesson.quiz()) {
                    cur.text(safe(q + ")  " + nullToEmpty(item.question())), bold, 12f);
                    if (item.options() != null) {
                        char opt = 'a';
                        for (String o : item.options()) {
                            cur.text(safe("    " + (opt++) + ". " + nullToEmpty(o)), body, 11f);
                        }
                    }
                    if (item.answer() != null && !item.answer().isBlank()) {
                        cur.text(safe("    Answer: " + item.answer()), body, 11f);
                    }
                    cur.gap(6f);
                    q++;
                }
            }

            cur.close();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private String subtitle(Lesson lesson) {
        List<String> parts = new ArrayList<>();
        if (lesson.subject() != null && !lesson.subject().isBlank()) {
            parts.add(safe(lesson.subject()));
        }
        if (lesson.grade() > 0) {
            parts.add("Grade " + lesson.grade());
        }
        if (lesson.language() != null && !lesson.language().isBlank()) {
            parts.add(safe(lesson.language()));
        }
        return String.join("  -  ", parts);
    }

    private List<String> wrap(String text, PDFont font, float size, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return lines;
        }
        for (String paragraph : text.split("\n")) {
            String[] words = paragraph.trim().split("\\s+");
            StringBuilder line = new StringBuilder();
            for (String word : words) {
                if (word.isEmpty()) {
                    continue;
                }
                String candidate = line.length() == 0 ? word : line + " " + word;
                if (line.length() > 0 && width(font, size, candidate) > maxWidth) {
                    lines.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(candidate);
                }
            }
            if (line.length() > 0) {
                lines.add(line.toString());
            }
        }
        return lines;
    }

    private float width(PDFont font, float size, String s) throws IOException {
        return font.getStringWidth(s) / 1000f * size;
    }

    /** Replace characters the Standard-14 fonts can't encode so PDFBox never throws while drawing. */
    static String safe(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '‘', '’', 'ʼ' -> b.append('\'');
                case '“', '”' -> b.append('"');
                case '–', '—' -> b.append('-');
                case '…' -> b.append("...");
                case '\n', '\r', '\t' -> b.append(' ');
                default -> b.append(c >= 32 && c < 127 ? c : ' ');
            }
        }
        return b.toString();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /**
     * A write cursor over a growing PDF: tracks the current page + baseline, wraps text, draws scaled
     * images, and starts a new page whenever the next block would cross the bottom margin.
     */
    private final class Cursor {

        private final PDDocument doc;
        private final float contentWidth;
        private PDPageContentStream cs;
        private float y;

        Cursor(PDDocument doc) throws IOException {
            this.doc = doc;
            this.contentWidth = PDRectangle.A4.getWidth() - 2 * MARGIN;
            newPage();
        }

        private void newPage() throws IOException {
            if (cs != null) {
                cs.close();
            }
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            cs = new PDPageContentStream(doc, page);
            y = PDRectangle.A4.getHeight() - MARGIN;
        }

        void gap(float h) {
            y -= h;
        }

        void text(String text, PDFont font, float size) throws IOException {
            for (String line : wrap(text, font, size, contentWidth)) {
                float lineHeight = size + LEADING;
                if (y - lineHeight < MARGIN) {
                    newPage();
                }
                cs.beginText();
                cs.setFont(font, size);
                cs.newLineAtOffset(MARGIN, y - size);
                cs.showText(line);
                cs.endText();
                y -= lineHeight;
            }
        }

        void image(byte[] bytes, String name) throws IOException {
            PDImageXObject image = PDImageXObject.createFromByteArray(doc, bytes, name);
            float w = contentWidth * IMAGE_WIDTH_FRACTION;
            float scale = w / image.getWidth();
            float h = image.getHeight() * scale;
            if (h > MAX_IMAGE_HEIGHT) {
                scale = MAX_IMAGE_HEIGHT / image.getHeight();
                h = MAX_IMAGE_HEIGHT;
                w = image.getWidth() * scale;
            }
            y -= 4f;
            if (y - h < MARGIN) {
                newPage();
            }
            cs.drawImage(image, MARGIN, y - h, w, h);
            y -= (h + 4f);
        }

        void close() throws IOException {
            if (cs != null) {
                cs.close();
                cs = null;
            }
        }
    }
}
