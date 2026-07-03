package com.katixo.studio.copilot.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.katixo.studio.code.CodeWorkspace;
import com.katixo.studio.code.CodeWorkspace.PatchOperation;
import com.katixo.studio.copilot.agent.CopilotTool;
import com.katixo.studio.copilot.agent.ToolResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Approval-gated multi-file patch tool. Prefer this over repeated one-file
 * writes when a coding task touches several files.
 */
@Component
public class CodeApplyPatchTool implements CopilotTool {

    private final CodeWorkspace workspace;
    private final ObjectMapper mapper;

    public CodeApplyPatchTool(CodeWorkspace workspace, ObjectMapper mapper) {
        this.workspace = workspace;
        this.mapper = mapper;
    }

    @Override
    public String name() {
        return "code_apply_patch";
    }

    @Override
    public String description() {
        return "Apply one approved multi-file code patch in the configured workspace. "
                + "Each operation either replaces a complete file with content, or performs one exact find/replace.";
    }

    @Override
    public JsonNode parameters() {
        ObjectNode root = mapper.createObjectNode();
        root.put("type", "object");
        ObjectNode props = root.putObject("properties");
        ObjectNode operations = props.putObject("operations");
        operations.put("type", "array");
        operations.put("description", "Patch operations. Maximum 20, no duplicate paths.");
        ObjectNode item = operations.putObject("items");
        item.put("type", "object");
        ObjectNode itemProps = item.putObject("properties");
        itemProps.putObject("path")
                .put("type", "string")
                .put("description", "Workspace-relative file path.");
        itemProps.putObject("content")
                .put("type", "string")
                .put("description", "Complete new file content. Mutually exclusive with find/replace.");
        itemProps.putObject("find")
                .put("type", "string")
                .put("description", "Exact existing text to replace. Must match exactly once.");
        itemProps.putObject("replace")
                .put("type", "string")
                .put("description", "Replacement text for find.");
        item.putArray("required").add("path");
        root.putArray("required").add("operations");
        return root;
    }

    @Override
    public boolean requiresApproval() {
        return true;
    }

    @Override
    public String approvalLabel(JsonNode args) {
        JsonNode ops = args.path("operations");
        int count = ops.isArray() ? ops.size() : 0;
        return "Apply code patch touching " + count + " file(s)";
    }

    @Override
    public ToolResult execute(JsonNode args) {
        try {
            JsonNode opsNode = args.path("operations");
            if (!opsNode.isArray()) {
                throw new IllegalArgumentException("operations must be an array");
            }
            List<PatchOperation> operations = new ArrayList<>();
            for (JsonNode node : opsNode) {
                operations.add(new PatchOperation(
                        Args.requireText(node, "path"),
                        optionalString(node, "content"),
                        optionalString(node, "find"),
                        optionalString(node, "replace")));
            }
            return ToolResult.text(workspace.applyPatch(operations));
        } catch (Exception e) {
            return ToolResult.text("code_apply_patch failed: " + e.getMessage());
        }
    }

    private String optionalString(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
