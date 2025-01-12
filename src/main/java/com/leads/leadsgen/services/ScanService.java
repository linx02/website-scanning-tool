package com.leads.leadsgen.services;

import com.leads.leadsgen.models.Asset;
import com.leads.leadsgen.models.ScanReport;
import com.leads.leadsgen.repositories.AssetRepository;
import com.leads.leadsgen.repositories.ScanReportRepository;
import com.leads.leadsgen.scanners.SeoScanner;
import com.leads.leadsgen.scanners.TrackerConsentScanner;
import org.springframework.stereotype.Service;

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

    public void scanDomains(List<String> domains, List<String> scanners) {
        domains.forEach(domain -> scanDomain(domain, scanners));
    }

    private void scanDomain(String domain, List<String> scanners) {
        try {
            sseService.broadcastStatus(domain, "Scanning", Map.of());

            Asset asset = assetRepository.findByDomain(domain)
                    .orElseThrow(() -> new IllegalArgumentException("Asset not found for domain: " + domain));

            for (String scanner : scanners) {
                ScanReport report = switch (scanner) {
                    case "SeoScanner" -> seoScanner.scan(asset);
                    case "TrackerConsentScanner" -> trackerConsentScanner.scan(asset);
                    default -> throw new IllegalArgumentException("Invalid scanner: " + scanner);
                };

                scanReportRepository.save(report);
            }

            sseService.broadcastStatus(domain, "Completed", Map.of());
        } catch (Exception e) {
            sseService.broadcastStatus(domain, "Error", Map.of("error", e.getMessage()));
        }
    }
}