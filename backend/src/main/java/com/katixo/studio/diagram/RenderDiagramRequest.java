package com.katixo.studio.diagram;

/**
 * Params for a {@link com.katixo.studio.job.JobType#DIAGRAM} job — serialized into
 * {@code jobs.params_json} for the worker to replay. {@code source} is PlantUML text.
 */
public record RenderDiagramRequest(String source) {
}
