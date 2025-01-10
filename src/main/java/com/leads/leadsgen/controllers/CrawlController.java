package com.leads.leadsgen.controllers;

import com.leads.leadsgen.models.Asset;
import com.leads.leadsgen.repositories.AssetRepository;
import com.leads.leadsgen.services.CrawlerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api")
public class CrawlController {

    private final CrawlerService crawlerService;
    private final AssetRepository assetRepository;

    // Hold status updates for streaming
    private final Map<String, String> statusMap = new ConcurrentHashMap<>();
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    // Thread pool for crawling tasks
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public CrawlController(CrawlerService crawlerService, AssetRepository assetRepository) {
        this.crawlerService = crawlerService;
        this.assetRepository = assetRepository;
    }

    @PostMapping("/crawl")
    public ResponseEntity<String> crawl(@RequestBody Map<String, List<String>> body) {
        List<String> domains = body.get("domains");

        for (String domain : domains) {
            executorService.submit(() -> crawlDomain(domain));
        }

        return ResponseEntity.ok("Crawl initialized for domains: " + domains);
    }

    @GetMapping("/stream-crawl-status")
    public SseEmitter streamStatus() {
        SseEmitter emitter = new SseEmitter();
        String emitterId = UUID.randomUUID().toString();
        emitters.put(emitterId, emitter);

        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    emitter.send(SseEmitter.event().data("keep-alive"));
                } catch (Exception e) {
                    emitters.remove(emitterId);
                    timer.cancel();
                }
            }
        }, 0, 15000); // Prevent timeout

        emitter.onCompletion(() -> emitters.remove(emitterId));
        emitter.onTimeout(() -> emitters.remove(emitterId));

        return emitter;
    }

    /**
     * The main crawl method.
     */
    private void crawlDomain(String domain) {
        try {
            System.out.println(">>> Starting crawlDomain for: " + domain);
            // Wait so client can connect to the SSE endpoint
            Thread.sleep(1000);
            updateStatus(domain, "Crawling");

            // Actual crawl
            Asset asset = crawlerService.crawl("https://" + domain);
            System.out.println(">>> CRAWLED [" + domain + "] => Emails found: " + asset.getEmails());

            // Prevent duplicates in DB
            if (assetRepository.findByDomain(domain).isPresent()) {
                System.out.println(">>> Domain already exists in DB: " + domain);
                updateStatus(domain, "Duplicate");
                return;
            }

            System.out.println(">>> Saving new asset for domain [" + domain + "]");
            assetRepository.save(asset);

            updateStatus(domain, "Completed");
        } catch (Exception e) {
            System.err.println(">>> ERROR during crawlDomain for [" + domain + "]: " + e.getMessage());
            updateStatus(domain, "Error: " + e.getMessage());
        }
    }

    /**
     * Update status for a domain, optionally include a suggested email if "Completed".
     */
    private void updateStatus(String domain, String status) {
        statusMap.put(domain, status);

        // Track dead emitters for removal AFTER the loop
        List<String> deadEmitterIds = new ArrayList<>();

        // Suggest email on complete
        String suggestedEmail = null;
        if ("Completed".equals(status)) {
            System.out.println(">>> Suggesting email for domain: " + domain);
            Optional<Asset> foundAsset = assetRepository.findByDomain(domain);
            Asset asset = foundAsset.isEmpty() ? null : foundAsset.get();

            if (asset != null) {
                System.out.println(">>> Found asset: " + asset);
                suggestedEmail = getSuggestedEmail(asset);
            } else {
                System.out.println(">>> No asset found for domain: " + domain);
            }
        }

        // Broadcast to SSE clients
        for (Map.Entry<String, SseEmitter> entry : emitters.entrySet()) {
            String emitterId = entry.getKey();
            SseEmitter emitter = entry.getValue();

            try {
                Map<String, String> payload = new HashMap<>();
                payload.put("domain", domain);
                payload.put("status", status);
                payload.put("suggestedEmail", suggestedEmail);

                emitter.send(payload);
            } catch (Exception e) {
                System.out.println(">>> Emitter [" + emitterId + "] is dead: " + e.getMessage());
                deadEmitterIds.add(emitterId);
            }
        }

        // Remove dead emitters
        for (String id : deadEmitterIds) {
            emitters.remove(id);
        }
    }

    /**
     * Suggest an email based on priority:
     * 1) info@domain
     * 2) anything@domain
     * 3) info@anything
     * 4) first email if none matched
     */
    private String getSuggestedEmail(Asset asset) {
        List<String> emails = asset.getEmails();
        if (emails.isEmpty()) {
            return null;
        }

        String domainNoWWW = asset.getDomain().replace("www.", "").toLowerCase();

        String exactInfoDomain = "info@" + domainNoWWW;

        String anyDomainEmail = null;
        String infoAnyEmail = null;
        String firstEmail = emails.getFirst(); // fallback

        for (String email : emails) {
            String lowerEmail = email.toLowerCase().trim();

            // info@domain
            if (lowerEmail.equals(exactInfoDomain)) {
                return email;
            }

            // anything@domain
            if (anyDomainEmail == null && lowerEmail.endsWith("@" + domainNoWWW)) {
                anyDomainEmail = email;
            }

            // info@anything
            if (infoAnyEmail == null && lowerEmail.startsWith("info@")) {
                infoAnyEmail = email;
            }
        }

        if (anyDomainEmail != null) {
            return anyDomainEmail;
        }
        if (infoAnyEmail != null) {
            return infoAnyEmail;
        }
        return firstEmail;
    }
}