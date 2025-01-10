package com.leads.leadsgen.controllers;

import com.leads.leadsgen.models.Asset;
import com.leads.leadsgen.models.ScanReport;
import com.leads.leadsgen.repositories.AssetRepository;
import com.leads.leadsgen.repositories.ScanReportRepository;
import com.leads.leadsgen.scanners.SeoScanner;
import com.leads.leadsgen.scanners.TrackerConsentScanner;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api")
public class ScanController {

    private final SeoScanner seoScanner;
    private final TrackerConsentScanner trackerConsentScanner;

    private final AssetRepository assetRepository;
    private final ScanReportRepository scanReportRepository;

    // Hold status updates for streaming
    private final Map<String, String> statusMap = new ConcurrentHashMap<>();
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    // Thread pool for scanning tasks
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public ScanController(SeoScanner seoScanner, TrackerConsentScanner trackerConsentScanner, AssetRepository assetRepository, ScanReportRepository scanReportRepository) {
        this.seoScanner = seoScanner;
        this.trackerConsentScanner = trackerConsentScanner;
        this.assetRepository = assetRepository;
        this.scanReportRepository = scanReportRepository;
    }

    @PostMapping("/scan")
    public ResponseEntity<String> scan(@RequestBody Map<String, List<String>> body) {
        List<String> domains = body.get("domains");
        List<String> scanners = body.get("scanners");

        for (String domain : domains) {
            executorService.submit(() -> scanDomain(domain, scanners));
        }

        return ResponseEntity.ok("Scan initialized for domains: " + domains);
    }

    @GetMapping("/stream-scan-status")
    public SseEmitter streamStatus() {
        SseEmitter emitter = new SseEmitter();
        String emitterId = UUID.randomUUID().toString();
        emitters.put(emitterId, emitter);

        emitter.onCompletion(() -> emitters.remove(emitterId));
        emitter.onTimeout(() -> emitters.remove(emitterId));

        return emitter;
    }

    private void scanDomain(String domain, List<String> scanners) {
        try {
            // Wait so client can connect to the SSE endpoint
            Thread.sleep(1000);

            updateStatus(domain, "Scanning");

            Optional<Asset> asset = assetRepository.findByDomain(domain);
            if (asset.isEmpty()) {
                throw new IllegalArgumentException("Asset not found for domain: " + domain);
            }

            for (String scanner : scanners) {
                ScanReport report;
                List<Map<String, String>> scannedBy = asset.get().getScannedBy();
                if (scannedBy == null) {
                    scannedBy = new ArrayList<>();
                }
                switch (scanner) {
                    case "SeoScanner":
                        report = seoScanner.scan(asset.get());
                        scannedBy.add(Map.of("name", seoScanner.getName(), "date", java.time.LocalDate.now().toString()));
                        break;

                    case "TrackerConsentScanner":
                        // Implement TrackerConsentScanner
                        report = trackerConsentScanner.scan(asset.get());
                        scannedBy.add(Map.of("name", trackerConsentScanner.getName(), "date", java.time.LocalDate.now().toString()));
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid scanner: " + scanner);
                }

                asset.get().setScannedBy(scannedBy);
                scanReportRepository.save(report);
                assetRepository.save(asset.get());
            }

            updateStatus(domain, "Completed");
        } catch (Exception e) {
            updateStatus(domain, "Error: " + e.getMessage());
        }
    }

    private void updateStatus(String domain, String status) {
        statusMap.put(domain, status);

        // Track dead emitters for removal AFTER the loop
        List<String> deadEmitterIds = new ArrayList<>();

        boolean isFlagged = false;
        // 3) Only compute the suggestedEmail if status is "Completed"
        if ("Completed".equals(status)) {
            System.out.println(">>> Suggesting email for domain: " + domain);
            Optional<ScanReport> foundScanReport = scanReportRepository.findByAssetDomain(domain);
            ScanReport scanReport = foundScanReport.isEmpty() ? null : foundScanReport.get();

            isFlagged = scanReport != null && scanReport.isFlagged();
        }

        // 4) Broadcast to all SSE clients
        for (Map.Entry<String, SseEmitter> entry : emitters.entrySet()) {
            String emitterId = entry.getKey();
            SseEmitter emitter = entry.getValue();

            try {
                // Construct payload
                Map<String, String> payload = new HashMap<>();
                payload.put("domain", domain);
                payload.put("status", status);
                payload.put("flagged", isFlagged ? "true" : "false");

                emitter.send(payload);
            } catch (Exception e) {
                // Mark this emitter as dead
                System.out.println(">>> Emitter [" + emitterId + "] is dead: " + e.getMessage());
                deadEmitterIds.add(emitterId);
            }
        }

        // 5) Remove any dead emitters
        for (String id : deadEmitterIds) {
            emitters.remove(id);
        }
    }
}