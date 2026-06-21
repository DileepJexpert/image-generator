package com.katixo.studio.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;

/**
 * Studio request-thread failures. Now that the doc-AI pipeline lives in the same app, its
 * {@code GlobalExceptionHandler} owns the broader cases (IllegalArgumentException, GpuBusyException,
 * BadInput, validation, and the generic fallback); this advice only adds the two studio-specific
 * checked exceptions, which are more specific so they still win.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * A sidecar call made on the request thread (e.g. the Copilot's Ollama call) failed — surface
     * 502 instead of a generic 500 so the UI can say "the model service is unreachable".
     */
    @ExceptionHandler(IOException.class)
    public ProblemDetail handleSidecarIo(IOException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    @ExceptionHandler(InterruptedException.class)
    public ProblemDetail handleInterrupted(InterruptedException ex) {
        Thread.currentThread().interrupt();
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Request interrupted before the model responded.");
    }
}
