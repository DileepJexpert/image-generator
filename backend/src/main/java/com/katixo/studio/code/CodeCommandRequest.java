package com.katixo.studio.code;

/** Parameters for an approved coding-agent command job. */
public record CodeCommandRequest(String command, String directory) {
}
