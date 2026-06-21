package com.katixo.studio.diagram;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Renders a system-design diagram from PlantUML source to a PNG, in-process — no sidecar, no
 * network, free. PlantUML covers exactly this surface: component/deployment diagrams (microservices
 * &amp; system design), class diagrams (design patterns), and sequence diagrams (flows).
 *
 * <p>Sequence diagrams render with no external tools; class/component diagrams use Graphviz when it
 * is installed and PlantUML's bundled Smetana engine otherwise. Everything stays local and offline.
 */
@Component
public class DiagramRenderer {

    public byte[] renderPng(String source) {
        String uml = normalize(source);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            new SourceStringReader(uml).outputImage(out, new FileFormatOption(FileFormat.PNG));
        } catch (IOException e) {
            throw new DiagramRenderException("Failed to render diagram: " + e.getMessage(), e);
        }
        byte[] png = out.toByteArray();
        if (png.length == 0) {
            throw new DiagramRenderException("PlantUML produced no image; check the diagram syntax.");
        }
        return png;
    }

    /** Accept a bare diagram body (the most common LLM slip) by wrapping it in @startuml/@enduml. */
    private String normalize(String source) {
        if (source == null || source.strip().isEmpty()) {
            throw new DiagramRenderException("Diagram source is empty.");
        }
        String s = source.strip();
        return s.contains("@start") ? s : "@startuml\n" + s + "\n@enduml";
    }
}
