package com.katixo.studio.copilot.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Tiny fluent builder for a JSON-Schema {@code object} describing a tool's
 * parameters, matching Ollama's tool spec format. Keeps each tool's
 * {@link CopilotTool#parameters()} terse and readable.
 */
public final class ToolSchema {

    private final ObjectMapper mapper;
    private final ObjectNode root;
    private final ObjectNode properties;
    private final ArrayNode required;

    private ToolSchema(ObjectMapper mapper) {
        this.mapper = mapper;
        this.root = mapper.createObjectNode();
        this.root.put("type", "object");
        this.properties = root.putObject("properties");
        this.required = root.putArray("required");
    }

    public static ToolSchema object(ObjectMapper mapper) {
        return new ToolSchema(mapper);
    }

    public ToolSchema prop(String name, String type, String description) {
        ObjectNode p = properties.putObject(name);
        p.put("type", type);
        p.put("description", description);
        return this;
    }

    /** Adds a string property constrained to an enumeration of allowed values. */
    public ToolSchema enumProp(String name, String description, String... values) {
        ObjectNode p = properties.putObject(name);
        p.put("type", "string");
        p.put("description", description);
        ArrayNode e = p.putArray("enum");
        for (String v : values) {
            e.add(v);
        }
        return this;
    }

    /** Adds an array-of-strings property. */
    public ToolSchema arrayProp(String name, String description) {
        ObjectNode p = properties.putObject(name);
        p.put("type", "array");
        p.put("description", description);
        p.putObject("items").put("type", "string");
        return this;
    }

    public ToolSchema require(String... names) {
        for (String n : names) {
            required.add(n);
        }
        return this;
    }

    public JsonNode build() {
        return root;
    }
}
