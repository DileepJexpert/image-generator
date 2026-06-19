package com.katixo.studio.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;

/**
 * Maps invalid-argument failures (e.g. bad upscale scale) to 400 instead of the
 * default 500. Bean-validation failures are already handled by Spring as 400.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * A sidecar call made on the request thread (e.g. the Copilot's Ollama call)
     * failed — surface 502 instead of a generic 500 so the UI can say "the model
     * service is unreachable".
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
