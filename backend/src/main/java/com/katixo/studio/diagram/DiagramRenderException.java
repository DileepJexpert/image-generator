package com.katixo.studio.diagram;

/** Thrown when a diagram cannot be rendered (empty/invalid source, or a renderer failure). */
public class DiagramRenderException extends RuntimeException {

    public DiagramRenderException(String message) {
        super(message);
    }

    public DiagramRenderException(String message, Throwable cause) {
        super(message, cause);
    }
}
