package com.katixo.studio.leads;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.asset.Asset;
import com.katixo.studio.asset.AssetService;
import com.katixo.studio.config.KatixoProperties;
import com.katixo.studio.copilot.ChatMessage;
import com.katixo.studio.copilot.OllamaClient;
import com.katixo.studio.job.Job;
import com.katixo.studio.job.JobHandler;
import com.katixo.studio.job.JobService;
import com.katixo.studio.job.JobType;
import com.katixo.studio.leads.LeadsDtos.Lead;
import com.katixo.studio.leads.LeadsDtos.LeadsResult;
import com.katixo.studio.leads.LeadsDtos.ScrapeLeadsRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Executes {@link JobType#LEAD_SCRAPE}: crawl the target sites, extract contact
 * leads, draft outreach copy per lead via the local Copilot LLM, and store the
 * result as a {@code text} asset (JSON) — reusing the same job → asset →
 * WebSocket pipeline as every other result.
 *
 * <p>Outreach drafting is best-effort: if the LLM sidecar is unavailable the
 * scraped leads are still returned, just without copy.
 */
@Component
public class LeadScrapeHandler implements JobHandler {

    private static final int DEFAULT_MAX_PAGES = 8;
    private static final int DEFAULT_MAX_LEADS = 25;
    // Outreach calls the LLM once per lead; cap it so a big scrape stays bounded.
    private static final int MAX_OUTREACH = 12;

    private final LeadScraper scraper;
    private final OllamaClient ollama;
    private final AssetService assetService;
    private final JobService jobService;
    private final KatixoProperties properties;
    private final ObjectMapper objectMapper;

    public LeadScrapeHandler(LeadScraper scraper, OllamaClient ollama, AssetService assetService,
                             JobService jobService, KatixoProperties properties,
                             ObjectMapper objectMapper) {
        this.scraper = scraper;
        this.ollama = ollama;
        this.assetService = assetService;
        this.jobService = jobService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public JobType type() {
        return JobType.LEAD_SCRAPE;
    }

    @Override
    public void handle(Job job) throws Exception {
        ScrapeLeadsRequest request =
                objectMapper.readValue(job.getParamsJson(), ScrapeLeadsRequest.class);
        int maxPages = clamp(request.maxPagesPerSite(), DEFAULT_MAX_PAGES, 1, 25);
        int maxLeads = clamp(request.maxLeads(), DEFAULT_MAX_LEADS, 1, 200);

        jobService.updateProgress(job.getId(), 5);
        List<Lead> scraped = scraper.scrape(request.targets(), maxPages, maxLeads);
        jobService.updateProgress(job.getId(), 55);

        List<Lead> leads = withOutreach(job, scraped, request.offering());

        LeadsResult result = new LeadsResult(
                request.offering(), request.targets().size(), leads.size(), leads);
        byte[] json = objectMapper.writeValueAsBytes(result);
        Asset asset = assetService.saveText(json, "application/json", job.getId());
        jobService.markDone(job.getId(), asset.getId());
    }

    /** Draft outreach for the first {@link #MAX_OUTREACH} leads (best-effort). */
    private List<Lead> withOutreach(Job job, List<Lead> leads, String offering) {
        String model = properties.copilotModel();
        List<Lead> out = new ArrayList<>(leads.size());
        for (int i = 0; i < leads.size(); i++) {
            Lead lead = leads.get(i);
            String copy = null;
            if (i < MAX_OUTREACH) {
                copy = draftOutreach(model, lead, offering);
                int progress = 55 + (int) ((i + 1) / (double) Math.min(leads.size(), MAX_OUTREACH) * 40);
                jobService.updateProgress(job.getId(), Math.min(progress, 95));
            }
            out.add(new Lead(lead.company(), lead.sourceUrl(), lead.email(),
                    lead.phone(), lead.socialLinks(), copy));
        }
        return out;
    }

    private String draftOutreach(String model, Lead lead, String offering) {
        String system = """
                You write concise, friendly B2B outreach emails. Output only the \
                email body: 3-4 sentences, personalized to the recipient, no \
                subject line, no placeholders like [Name], and end with a soft \
                call to action. Plain text.""";
        String user = "Recipient company: " + lead.company()
                + "\nWebsite: " + lead.sourceUrl()
                + "\nWhat I offer: " + (offering == null || offering.isBlank()
                        ? "(not specified — keep it generic but relevant)" : offering)
                + "\nWrite the outreach email now.";
        try {
            return ollama.chat(model, List.of(
                    ChatMessage.system(system),
                    new ChatMessage("user", user))).trim();
        } catch (Exception e) {
            return null; // LLM unavailable — leave outreach blank, keep the lead.
        }
    }

    private static int clamp(Integer value, int fallback, int min, int max) {
        int v = (value == null) ? fallback : value;
        return Math.max(min, Math.min(max, v));
    }
}
