package com.katixo.studio.leads;

import com.katixo.studio.leads.LeadsDtos.Lead;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Crawls public pages of a target site and extracts company-level contact leads
 * (emails, phones, social links). A good-citizen crawler: it identifies itself,
 * honors {@code robots.txt} (wildcard group), rate-limits, caps pages, and only
 * follows likely contact/about/team links on the same host.
 *
 * <p>This is deliberately conservative — it surfaces contact details a business
 * already publishes for inbound enquiries, for the single local operator to do
 * their own outreach. It is not a bulk harvester.
 */
@Component
public class LeadScraper {

    private static final String USER_AGENT =
            "KatixoLeadsBot/1.0 (+local single-user; honors robots.txt)";
    private static final int TIMEOUT_MS = 10_000;
    private static final long POLITE_DELAY_MS = 600L;
    private static final int MAX_BODY_BYTES = 2 * 1024 * 1024;

    private static final Pattern EMAIL = Pattern.compile(
            "[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}");
    private static final Set<String> SOCIAL_HOSTS = Set.of(
            "linkedin.com", "twitter.com", "x.com", "facebook.com",
            "instagram.com", "youtube.com");
    private static final Set<String> JUNK_EMAIL_HOSTS = Set.of(
            "example.com", "example.org", "sentry.io", "wixpress.com",
            "domain.com", "email.com", "yourdomain.com");
    private static final List<String> LEAD_PATH_HINTS = List.of(
            "contact", "about", "team", "company", "support", "impressum", "kontakt");

    /** Crawl several targets, accumulating leads up to {@code maxLeads}. */
    public List<Lead> scrape(List<String> targets, int maxPagesPerSite, int maxLeads) {
        List<Lead> all = new ArrayList<>();
        for (String target : targets) {
            if (all.size() >= maxLeads) {
                break;
            }
            for (Lead lead : scrapeSite(target, maxPagesPerSite)) {
                all.add(lead);
                if (all.size() >= maxLeads) {
                    break;
                }
            }
        }
        return all;
    }

    /** Crawl one site and return its company-level leads (no outreach yet). */
    public List<Lead> scrapeSite(String target, int maxPages) {
        URI base = normalize(target);
        if (base == null) {
            return List.of();
        }
        Robots robots = Robots.fetch(base);

        Set<String> visited = new LinkedHashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(base.toString());

        String company = base.getHost();
        String sourceUrl = base.toString();
        Set<String> emails = new LinkedHashSet<>();
        Set<String> phones = new LinkedHashSet<>();
        Set<String> socials = new LinkedHashSet<>();

        int pages = 0;
        while (!queue.isEmpty() && pages < maxPages) {
            String url = queue.poll();
            if (!visited.add(url) || !robots.allows(pathOf(url))) {
                continue;
            }
            Document doc = fetch(url);
            if (doc == null) {
                continue;
            }
            pages++;
            if (pages == 1) {
                company = siteName(doc, base.getHost());
                sourceUrl = url;
            }
            extractEmails(doc, emails);
            extractPhones(doc, phones);
            extractSocials(doc, socials);
            enqueueLeadPages(doc, base, visited, queue, pages, maxPages);
            polite();
        }

        List<String> social = new ArrayList<>(socials);
        String phone = phones.isEmpty() ? null : phones.iterator().next();
        if (emails.isEmpty()) {
            return List.of(new Lead(company, sourceUrl, null, phone, social, null));
        }
        List<Lead> leads = new ArrayList<>();
        for (String email : emails) {
            leads.add(new Lead(company, sourceUrl, email, phone, social, null));
        }
        return leads;
    }

    private void enqueueLeadPages(Document doc, URI base, Set<String> visited,
                                  Deque<String> queue, int pages, int maxPages) {
        for (Element a : doc.select("a[href]")) {
            String abs = stripFragment(a.absUrl("href"));
            if (abs.isBlank() || visited.contains(abs)) {
                continue;
            }
            URI u = safeUri(abs);
            if (u == null || u.getHost() == null || !sameHost(u.getHost(), base.getHost())) {
                continue;
            }
            String path = (u.getPath() == null ? "" : u.getPath()).toLowerCase(Locale.ROOT);
            boolean looksLikeLeadPage = LEAD_PATH_HINTS.stream().anyMatch(path::contains);
            if (looksLikeLeadPage && queue.size() + pages < maxPages) {
                queue.add(abs);
            }
        }
    }

    private void extractEmails(Document doc, Set<String> out) {
        for (Element a : doc.select("a[href^=mailto:]")) {
            addEmail(a.attr("href").substring("mailto:".length()).split("\\?")[0], out);
        }
        Matcher m = EMAIL.matcher(doc.text());
        while (m.find()) {
            addEmail(m.group(), out);
        }
    }

    private void addEmail(String raw, Set<String> out) {
        String email = raw.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(email).matches()) {
            return;
        }
        String host = email.substring(email.indexOf('@') + 1);
        if (JUNK_EMAIL_HOSTS.contains(host)) {
            return;
        }
        out.add(email);
    }

    private void extractPhones(Document doc, Set<String> out) {
        for (Element a : doc.select("a[href^=tel:]")) {
            String phone = a.attr("href").substring("tel:".length()).trim();
            if (!phone.isBlank()) {
                out.add(phone);
            }
        }
    }

    private void extractSocials(Document doc, Set<String> out) {
        for (Element a : doc.select("a[href]")) {
            URI u = safeUri(a.absUrl("href"));
            if (u == null || u.getHost() == null) {
                continue;
            }
            String host = u.getHost().toLowerCase(Locale.ROOT).replaceFirst("^www\\.", "");
            for (String social : SOCIAL_HOSTS) {
                if (host.equals(social) || host.endsWith("." + social)) {
                    out.add(stripFragment(u.toString()));
                    break;
                }
            }
        }
    }

    private String siteName(Document doc, String fallback) {
        String og = doc.select("meta[property=og:site_name]").attr("content").trim();
        if (!og.isBlank()) {
            return og;
        }
        String title = doc.title().trim();
        if (!title.isBlank()) {
            return title.split("[|\\-–—:]")[0].trim();
        }
        return fallback;
    }

    private Document fetch(String url) {
        try {
            return Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .maxBodySize(MAX_BODY_BYTES)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .get();
        } catch (Exception e) {
            return null;
        }
    }

    private void polite() {
        try {
            Thread.sleep(POLITE_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static URI normalize(String target) {
        String t = target == null ? "" : target.trim();
        if (t.isEmpty()) {
            return null;
        }
        if (!t.matches("(?i)^https?://.*")) {
            t = "https://" + t;
        }
        URI u = safeUri(t);
        return (u != null && u.getHost() != null) ? u : null;
    }

    private static URI safeUri(String s) {
        try {
            return URI.create(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static String pathOf(String url) {
        URI u = safeUri(url);
        String p = (u == null ? null : u.getPath());
        return (p == null || p.isEmpty()) ? "/" : p;
    }

    private static String stripFragment(String url) {
        int h = url.indexOf('#');
        return h >= 0 ? url.substring(0, h) : url;
    }

    private static boolean sameHost(String a, String b) {
        return a.replaceFirst("^www\\.", "").equalsIgnoreCase(b.replaceFirst("^www\\.", ""));
    }

    /**
     * Minimal {@code robots.txt} support: honors {@code Disallow} rules in the
     * wildcard ({@code User-agent: *}) group. Conservative by design — on any
     * fetch/parse error it allows, but it never ignores an explicit wildcard
     * disallow.
     */
    private static final class Robots {
        private final List<String> disallow;

        private Robots(List<String> disallow) {
            this.disallow = disallow;
        }

        static Robots fetch(URI base) {
            try {
                String robotsUrl = base.getScheme() + "://" + base.getAuthority() + "/robots.txt";
                String body = Jsoup.connect(robotsUrl)
                        .userAgent(USER_AGENT)
                        .timeout(TIMEOUT_MS)
                        .ignoreContentType(true)
                        .ignoreHttpErrors(true)
                        .execute()
                        .body();
                return parse(body);
            } catch (Exception e) {
                return new Robots(List.of());
            }
        }

        static Robots parse(String text) {
            List<String> rules = new ArrayList<>();
            boolean applies = false;
            for (String raw : text.split("\\R")) {
                String line = raw;
                int hash = line.indexOf('#');
                if (hash >= 0) {
                    line = line.substring(0, hash);
                }
                line = line.trim();
                int colon = line.indexOf(':');
                if (colon < 0) {
                    continue;
                }
                String key = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
                String value = line.substring(colon + 1).trim();
                if (key.equals("user-agent")) {
                    applies = value.equals("*");
                } else if (key.equals("disallow") && applies && !value.isEmpty()) {
                    rules.add(value);
                }
            }
            return new Robots(rules);
        }

        boolean allows(String path) {
            for (String rule : disallow) {
                if (path.startsWith(rule)) {
                    return false;
                }
            }
            return true;
        }
    }
}
