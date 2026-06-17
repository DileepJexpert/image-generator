package com.katixo.studio.job;

import java.util.UUID;

/** Standard response for generation/edit endpoints: just the async job id. */
public record JobIdResponse(UUID jobId) {
}
