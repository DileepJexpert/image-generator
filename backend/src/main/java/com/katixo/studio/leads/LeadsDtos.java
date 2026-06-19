package com.katixo.studio.leads;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request/response payloads for the leads feature (DTOs only at the boundary,
 * CLAUDE.md §6). A scrape crawls the given public sites, extracts contact
 * leads, and (via the local Copilot LLM) drafts outreach copy for each.
 */
public final class LeadsDtos {

    private LeadsDtos() {
    }

    /**
     * A lead-generation request.
     *
     * @param targets          one or more site domains or URLs to crawl (public
     *                         pages only)
     * @param offering         a short description of what the user offers, used
     *                         to tailor the drafted outreach (optional)
     * @param maxPagesPerSite  crawl budget per site (homepage + likely
     *                         contact/about/team pages); defaults applied in the
     *                         service
     * @param maxLeads         overall cap on returned leads
     */
    public record ScrapeLeadsRequest(
            @NotEmpty @Size(max = 25) List<@Size(max = 300) String> targets,
            @Size(max = 500) String offering,
            Integer maxPagesPerSite,
            Integer maxLeads
    ) {
    }

    /** One discovered lead. Fields are best-effort; many sites expose only some. */
    public record Lead(
            String company,
            String sourceUrl,
            String email,
            String phone,
            List<String> socialLinks,
            String outreach
    ) {
    }

    /** The stored scrape result (persisted as a {@code text} asset, JSON body). */
    public record LeadsResult(
            String offering,
            int siteCount,
            int leadCount,
            List<Lead> leads
    ) {
    }
}
