package com.leads.leadsgen.service;

import com.leads.leadsgen.model.Asset;
import com.leads.leadsgen.model.ScanReport;
import com.leads.leadsgen.repository.AssetRepository;
import com.leads.leadsgen.repository.ScanReportRepository;
import com.leads.leadsgen.scanner.SeoScanner;
import com.leads.leadsgen.scanner.TrackerConsentScanner;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ScanService {

    private final AssetRepository assetRepository;
    private final ScanReportRepository scanReportRepository;
    private final SeoScanner seoScanner;
    private final TrackerConsentScanner trackerConsentScanner;
    private final SseService sseService;

    public ScanService(
            AssetRepository assetRepository,
            ScanReportRepository scanReportRepository,
            SeoScanner seoScanner,
            TrackerConsentScanner trackerConsentScanner,
            SseService sseService) {
        this.assetRepository = assetRepository;
        this.scanReportRepository = scanReportRepository;
        this.seoScanner = seoScanner;
        this.trackerConsentScanner = trackerConsentScanner;
        this.sseService = sseService;
    }

    /**
     * Scan a list of domains concurrently
     *
     * @param domains   List of domains to scan
     * @param scanners  List of scanners to use
     */
    public void scanDomains(List<String> domains, List<String> scanners) {

        // TrackerConsentScanner cannot run concurrently due to resource constraints
        if (scanners.getFirst().equals("TrackerConsentScanner")) {
            domains.forEach(domain -> {
                scanDomain(domain, scanners);
            });
        } else {
            domains.forEach(domain -> {
                Thread thread = new Thread(() -> scanDomain(domain, scanners));
                thread.start();
            });
        }
    }

    /**
     * Scan a single domain
     *
     * @param domain    Domain to scan
     * @param scanners  List of scanners to use
     */
    private void scanDomain(String domain, List<String> scanners) {
        try {
            Thread.sleep(1000); // So client can connect
            sseService.broadcastStatus(domain, "Scanning", Map.of());

            Asset asset = assetRepository.findByDomain(domain)
                    .orElseThrow(() -> new IllegalArgumentException("Asset not found for domain: " + domain));

            for (String scanner : scanners) {
                ScanReport report = switch (scanner) {
                    case "SeoScanner" -> seoScanner.scan(asset);
                    case "TrackerConsentScanner" -> trackerConsentScanner.scan(asset);
                    default -> throw new IllegalArgumentException("Invalid scanner: " + scanner);
                };

                System.out.println("Scan report: " + report);

                List<Map<String, String>> scannedBy = asset.getScannedBy();

                if (scannedBy == null) {
                    scannedBy = new ArrayList<>();
                }

                scannedBy.add(Map.of("scanner", scanner, "datetime", report.getCreatedAt().toString()));
                asset.setScannedBy(scannedBy);
                assetRepository.save(asset);

                scanReportRepository.save(report);

                sseService.broadcastStatus(domain, "Scanned", Map.of("flagged", report.isFlagged() ? "yes" : "no"));
            }
        } catch (Exception e) {
            System.out.println("Error scanning domain: " + domain + " - " + e.getMessage());
            sseService.broadcastStatus(domain, "Error", Map.of("error", e.getMessage()));
        }
    }
}