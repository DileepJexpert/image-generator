package com.katixo.studio.copilot.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.copilot.agent.CopilotTool;
import com.katixo.studio.copilot.agent.ToolResult;
import com.katixo.studio.copilot.agent.ToolSchema;
import com.katixo.studio.leads.LeadsDtos.ScrapeLeadsRequest;
import com.katixo.studio.leads.LeadsService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Scrapes public sites for contact leads and drafts outreach. Approval-gated:
 * it reaches outside the machine, so the agent proposes it and the user confirms
 * before it runs (mirrors Cline's approval gate / OpenClaw's "gate external
 * communication" guidance).
 */
@Component
public class ScrapeLeadsTool implements CopilotTool {

    private final LeadsService leadsService;
    private final ObjectMapper mapper;

    public ScrapeLeadsTool(LeadsService leadsService, ObjectMapper mapper) {
        this.leadsService = leadsService;
        this.mapper = mapper;
    }

    @Override
    public String name() {
        return "scrape_leads";
    }

    @Override
    public String description() {
        return "Find business leads by crawling public websites and drafting "
                + "outreach for each. Reaches external sites, so it requires the "
                + "user's confirmation before running.";
    }

    @Override
    public JsonNode parameters() {
        return ToolSchema.object(mapper)
                .arrayProp("targets", "Site domains or URLs to crawl (public pages only).")
                .prop("offering", "string", "Short description of what the user offers (tailors outreach).")
                .prop("maxLeads", "integer", "Cap on total leads returned (optional).")
                .require("targets")
                .build();
    }

    @Override
    public boolean requiresApproval() {
        return true;
    }

    @Override
    public String approvalLabel(JsonNode args) {
        int count = args.path("targets").isArray() ? args.path("targets").size() : 0;
        return "Scrape " + count + " site(s) for leads and draft outreach";
    }

    @Override
    public ToolResult execute(JsonNode args) {
        List<String> targets = new ArrayList<>();
        for (JsonNode t : args.path("targets")) {
            String s = t.asText("").trim();
            if (!s.isEmpty()) {
                targets.add(s);
            }
        }
        if (targets.isEmpty()) {
            throw new IllegalArgumentException("At least one target site is required.");
        }
        String offering = Args.text(args, "offering", null);
        Integer maxLeads = args.path("maxLeads").isInt() ? args.path("maxLeads").asInt() : null;

        UUID jobId = leadsService.submitScrape(
                new ScrapeLeadsRequest(targets, offering, null, maxLeads));
        return ToolResult.job("Started lead scrape across " + targets.size() + " site(s).", jobId);
    }
}
