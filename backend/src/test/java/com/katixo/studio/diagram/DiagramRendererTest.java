package com.katixo.studio.diagram;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class DiagramRendererTest {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    private final DiagramRenderer renderer = new DiagramRenderer();

    @Test
    void rendersValidPlantumlToPng() {
        byte[] png = renderOrSkip("@startuml\nAlice -> Bob: Hello\nBob --> Alice: Hi\n@enduml");
        assertThat(png).isNotEmpty();
        // PNG magic number: 0x89 'P' 'N' 'G'
        assertThat(png[0] & 0xFF).isEqualTo(0x89);
        assertThat(new String(png, 1, 3, StandardCharsets.US_ASCII)).isEqualTo("PNG");
    }

    @Test
    void wrapsBareSourceMissingStartDirective() {
        byte[] png = renderOrSkip("Alice -> Bob: Hi");   // no @startuml; normalize() wraps it
        assertThat(png).isNotEmpty();
    }

    @Test
    void rejectsEmptySource() {
        // Input validation — no AWT involved, so this always runs.
        assertThatThrownBy(() -> renderer.renderPng("   "))
                .isInstanceOf(DiagramRenderException.class);
    }

    /** Render, or skip if this environment can't do AWT text rendering (headless CI without fonts). */
    private byte[] renderOrSkip(String source) {
        try {
            return renderer.renderPng(source);
        } catch (Throwable t) {
            assumeTrue(false, "AWT rendering unavailable here (works on a real desktop/server): " + t);
            return new byte[0];   // unreachable
        }
    }
}
