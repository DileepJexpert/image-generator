package com.katixo.studio.copilot.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Collects every {@link CopilotTool} bean into the agent's fixed toolset and
 * renders the Ollama {@code tools} spec array. Keeping the registry small and
 * well-described matters: tool-selection accuracy on local models drops as the
 * list grows (see {@code docs/agentic-copilot-research.md}).
 */
@Component
public class ToolRegistry {

    private final Map<String, CopilotTool> tools = new LinkedHashMap<>();
    private final ObjectMapper mapper;

    public ToolRegistry(List<CopilotTool> beans, ObjectMapper mapper) {
        this.mapper = mapper;
        for (CopilotTool tool : beans) {
            tools.put(tool.name(), tool);
        }
    }

    public Optional<CopilotTool> find(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    /** Builds the Ollama {@code tools} array (OpenAI-style function specs). */
    public ArrayNode specs() {
        ArrayNode array = mapper.createArrayNode();
        for (CopilotTool tool : tools.values()) {
            ObjectNode entry = array.addObject();
            entry.put("type", "function");
            ObjectNode fn = entry.putObject("function");
            fn.put("name", tool.name());
            fn.put("description", tool.description());
            fn.set("parameters", tool.parameters());
        }
        return array;
    }
}
