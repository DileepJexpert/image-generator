package com.katixo.studio.lesson;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders a {@link Lesson} (plus optional per-section illustrations) into a printable A4 PDF
 * worksheet using PDFBox — in-process, no sidecar, no network. Text is laid out with manual
 * word-wrap and pagination; each section image is scaled to fit the content width.
 *
 * <p><b>Scripts.</b> Latin text is drawn as real, selectable PDF text with the Standard-14
 * Helvetica fonts (crisp, searchable, tiny). Any block that contains characters Helvetica's WinAnsi
 * encoding can't represent — Devanagari for Hindi lessons, other Indic/non-Latin scripts, accented
 * Latin — is instead rendered through Java2D into a transparent image and placed into the PDF.
 *
 * <p>The image path matters because PDFBox's {@code showText} performs <i>no</i> complex-script
 * shaping: naively embedding a Devanagari font would place glyphs in logical order with broken
 * matras and conjuncts. Java2D's text layout runs HarfBuzz (JDK&nbsp;9+), so a shaped raster of the
 * line is correct. The font is the bundled OFL {@code Mukta} family ({@code resources/fonts}), which
 * covers Devanagari + Latin. The full Unicode text is always preserved verbatim in the lesson JSON.
 */
@Component
public class LessonPdfRenderer {

    private static final Logger log = LoggerFactory.getLogger(LessonPdfRenderer.class);

    private static final float MARGIN = 50f;
    private static final float LEADING = 4f;          // extra gap below each text line
    private static final float BLOCK_GAP = 12f;       // gap between blocks (section/heading/image)
    private static final float IMAGE_WIDTH_FRACTION = 0.6f;
    private static final float MAX_IMAGE_HEIGHT = 240f;
    private static final int RENDER_SCALE = 4;        // px-per-point when rastering a non-Latin line

    /** Bundled OFL Devanagari+Latin font, loaded once (base size; derive per line). Null only if the
     *  resource is missing/corrupt, in which case non-Latin text degrades to ASCII rather than failing. */
    private static final Font AWT_REGULAR = loadAwtFont("/fonts/Mukta-Regular.ttf");
    private static final Font AWT_BOLD = loadAwtFont("/fonts/Mukta-Bold.ttf");

    private final PDFont body = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    private static Font loadAwtFont(String resource) {
        try (InputStream in = LessonPdfRenderer.class.getResourceAsStream(resource)) {
            if (in == null) {
                log.error("Bundled PDF font missing: {} — non-Latin lessons will fall back to ASCII", resource);
                return null;
            }
            return Font.createFont(Font.TRUETYPE_FONT, in);
        } catch (Exception e) {
            log.error("Failed to load bundled PDF font {}: {}", resource, e.toString());
            return null;
        }
    }

    private static boolean fontsReady() {
        return AWT_REGULAR != null && AWT_BOLD != null;
    }

    /**
     * @param sectionImages PNG bytes aligned to {@code lesson.sections()} (an entry may be
     *                      {@code null}, or the whole list may be {@code null}, when no illustration
     *                      is available)
     */
    public byte[] render(Lesson lesson, List<byte[]> sectionImages) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            Cursor cur = new Cursor(doc);

            cur.text(lesson.title(), true, 20f);
            String subtitle = subtitle(lesson);
            if (!subtitle.isBlank()) {
                cur.text(subtitle, false, 11f);
            }
            cur.gap(BLOCK_GAP);

            List<Lesson.Section> sections = lesson.sections() == null ? List.of() : lesson.sections();
            for (int i = 0; i < sections.size(); i++) {
                Lesson.Section s = sections.get(i);
                cur.text((i + 1) + ".  " + nullToEmpty(s.heading()), true, 14f);
                if (s.body() != null && !s.body().isBlank()) {
                    cur.text(s.body(), false, 11f);
                }
                byte[] img = (sectionImages != null && i < sectionImages.size()) ? sectionImages.get(i) : null;
                if (img != null && img.length > 0) {
                    cur.image(img, "sec" + i);
                }
                cur.gap(BLOCK_GAP);
            }

            if (lesson.quiz() != null && !lesson.quiz().isEmpty()) {
                cur.gap(BLOCK_GAP);
                cur.text("Quiz", true, 16f);
                int q = 1;
                for (Lesson.QuizItem item : lesson.quiz()) {
                    cur.text(q + ")  " + nullToEmpty(item.question()), true, 12f);
                    if (item.options() != null) {
                        char opt = 'a';
                        for (String o : item.options()) {
                            cur.text("    " + (opt++) + ". " + nullToEmpty(o), false, 11f);
                        }
                    }
                    if (item.answer() != null && !item.answer().isBlank()) {
                        cur.text("    Answer: " + item.answer(), false, 11f);
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
            parts.add(lesson.subject());
        }
        if (lesson.grade() > 0) {
            parts.add("Grade " + lesson.grade());
        }
        if (lesson.language() != null && !lesson.language().isBlank()) {
            parts.add(lesson.language());
        }
        return String.join("  -  ", parts);
    }

    // ---- routing ---------------------------------------------------------------------------------

    /**
     * Normalise typographic punctuation to ASCII equivalents (smart quotes, dashes, ellipsis, NBSP,
     * stray tabs/CRs). Everything else — including non-Latin scripts — is preserved; routing decides
     * how it gets drawn.
     */
    static String normalize(String s) {
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
                case ' ', '\r', '\t' -> b.append(' ');
                default -> b.append(c);
            }
        }
        return b.toString();
    }

    /** True when {@code normalized} has a character Helvetica can't draw, so the shaped-image path is
     *  needed. Always false when the bundled fonts are unavailable (we degrade to ASCII instead). */
    private static boolean needsImagePath(String normalized) {
        if (!fontsReady()) {
            return false;
        }
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (c != '\n' && (c < 32 || c > 126)) {
                return true;
            }
        }
        return false;
    }

    /** Last-resort fallback (bundled fonts failed to load): drop anything Helvetica can't encode. */
    private static String asciiOnly(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            b.append(c == '\n' ? '\n' : (c >= 32 && c < 127 ? c : ' '));
        }
        return b.toString();
    }

    // ---- word wrap -------------------------------------------------------------------------------

    private List<String> wrap(String text, PDFont font, float size, float maxWidth) throws IOException {
        return wrap(text, candidate -> font.getStringWidth(candidate) / 1000f * size, maxWidth);
    }

    private List<String> wrapAwt(String text, Font font, float maxWidth) throws IOException {
        FontRenderContext frc = new FontRenderContext(null, true, true);
        return wrap(text, candidate -> (float) font.getStringBounds(candidate, frc).getWidth(), maxWidth);
    }

    private List<String> wrap(String text, Measurer measure, float maxWidth) throws IOException {
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
                if (line.length() > 0 && measure.width(candidate) > maxWidth) {
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

    /** Width of a candidate string in PDF points; may consult font metrics that throw. */
    @FunctionalInterface
    private interface Measurer {
        float width(String candidate) throws IOException;
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
        private int imgSeq;

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

        /** Draw a block of text at the given weight/size, routing non-Latin content to the image path. */
        void text(String raw, boolean boldStyle, float size) throws IOException {
            String n = normalize(raw);
            if (needsImagePath(n)) {
                Font f = (boldStyle ? AWT_BOLD : AWT_REGULAR).deriveFont(size);
                for (String line : wrapAwt(n, f, contentWidth)) {
                    imageLine(line, f, size);
                }
            } else {
                PDFont f = boldStyle ? bold : body;
                String t = fontsReady() ? n : asciiOnly(n);
                for (String line : wrap(t, f, size, contentWidth)) {
                    float lineHeight = size + LEADING;
                    if (y - lineHeight < MARGIN) {
                        newPage();
                    }
                    cs.beginText();
                    cs.setFont(f, size);
                    cs.newLineAtOffset(MARGIN, y - size);
                    cs.showText(line);
                    cs.endText();
                    y -= lineHeight;
                }
            }
        }

        /** Render one wrapped non-Latin line via Java2D (HarfBuzz shaping) and place it as an image. */
        private void imageLine(String line, Font font, float size) throws IOException {
            if (line.isBlank()) {
                y -= size + LEADING;
                return;
            }
            FontRenderContext frc = new FontRenderContext(null, true, true);
            LineMetrics lm = font.getLineMetrics(line, frc);
            float ascent = lm.getAscent();
            float descent = lm.getDescent();
            float wPt = (float) Math.ceil(font.getStringBounds(line, frc).getWidth()) + 2f;
            float hPt = (float) Math.ceil(ascent + descent) + 2f;

            int pxW = Math.max(1, (int) Math.ceil(wPt * RENDER_SCALE));
            int pxH = Math.max(1, (int) Math.ceil(hPt * RENDER_SCALE));
            BufferedImage img = new BufferedImage(pxW, pxH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.scale(RENDER_SCALE, RENDER_SCALE);
            g.setColor(Color.BLACK);
            g.setFont(font);
            g.drawString(line, 1f, ascent + 1f);
            g.dispose();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", bos);

            if (y - hPt < MARGIN) {
                newPage();
            }
            PDImageXObject xo = PDImageXObject.createFromByteArray(doc, bos.toByteArray(), "txt" + (imgSeq++));
            cs.drawImage(xo, MARGIN, y - hPt, wPt, hPt);
            y -= hPt + LEADING;
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
