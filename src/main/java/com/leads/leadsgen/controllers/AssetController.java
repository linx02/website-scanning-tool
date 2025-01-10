package com.leads.leadsgen.controllers;

import com.leads.leadsgen.models.Asset;
import com.leads.leadsgen.models.ScanReport;
import com.leads.leadsgen.repositories.AssetRepository;
import com.leads.leadsgen.repositories.ScanReportRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AssetController {

    private final AssetRepository assetRepository;
    private final ScanReportRepository scanReportRepository;

    public AssetController(AssetRepository assetRepository, ScanReportRepository scanReportRepository) {
        this.assetRepository = assetRepository;
        this.scanReportRepository = scanReportRepository;
    }

    @GetMapping("/assets")
    public ResponseEntity<List<Asset>> getAssets() {
        return ResponseEntity.ok(assetRepository.findAll());
    }

    @GetMapping("/reports")
    public ResponseEntity<List<ScanReport>> getReports() {
        return ResponseEntity.ok(scanReportRepository.findAll());
    }
}
