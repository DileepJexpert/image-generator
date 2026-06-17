package com.katixo.studio.generation;

import java.util.UUID;

/** Standard response for generation/edit endpoints: just the async job id. */
public record JobIdResponse(UUID jobId) {
}
