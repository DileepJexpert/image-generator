package com.katixo.studio.copilot.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

/**
 * Small, forgiving readers for LLM-supplied tool arguments. Local models are
 * loose with types and omissions, so these coerce/default rather than throw —
 * except for genuinely required values, which fail clearly.
 */
final class Args {

    private Args() {
    }

    static String text(JsonNode args, String field, String fallback) {
        JsonNode n = args.path(field);
        if (n.isMissingNode() || n.isNull()) {
            return fallback;
        }
        String s = n.asText("").trim();
        return s.isEmpty() ? fallback : s;
    }

    static String requireText(JsonNode args, String field) {
        String s = text(args, field, null);
        if (s == null) {
            throw new IllegalArgumentException("Missing required argument: " + field);
        }
        return s;
    }

    static int clampedInt(JsonNode args, String field, int fallback, int min, int max) {
        int v = args.path(field).asInt(fallback);
        return Math.max(min, Math.min(max, v));
    }

    static UUID requireUuid(JsonNode args, String field) {
        String s = requireText(args, field);
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Argument '" + field + "' must be a valid id, got: " + s);
        }
    }
}
