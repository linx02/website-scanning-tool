package com.leads.leadsgen.controller;

import com.leads.leadsgen.model.Asset;
import com.leads.leadsgen.model.ScanReport;
import com.leads.leadsgen.repository.AssetRepository;
import com.leads.leadsgen.repository.ScanReportRepository;
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
