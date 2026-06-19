package com.katixo.studio.leads;

import com.katixo.studio.job.JobIdResponse;
import com.katixo.studio.leads.LeadsDtos.ScrapeLeadsRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Leads API: kick off a scrape-and-draft job for one or more public sites.
 * Returns a {@code jobId}; the result is a {@code text} asset holding the leads
 * JSON (contacts + drafted outreach), streamed back over the job WebSocket.
 */
@RestController
@RequestMapping("/api/v1/leads")
public class LeadsController {

    private final LeadsService leadsService;

    public LeadsController(LeadsService leadsService) {
        this.leadsService = leadsService;
    }

    @PostMapping("/scrape")
    public JobIdResponse scrape(@Valid @RequestBody ScrapeLeadsRequest request) {
        return new JobIdResponse(leadsService.submitScrape(request));
    }
}
