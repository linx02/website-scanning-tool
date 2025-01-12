package com.leads.leadsgen.repositories;

import com.leads.leadsgen.models.Asset;
import com.leads.leadsgen.models.ScanReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ScanReportRepositoryTest {

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private ScanReportRepository scanReportRepository;

    @BeforeEach
    void setUp() {
        scanReportRepository.deleteAll();
        assetRepository.deleteAll();
    }

    @Test
    public void testFindByAssetDomain() {

        List<Map<String, String>> scannedBy = new ArrayList<>();
        scannedBy.add(new HashMap<>());
        scannedBy.getFirst().put("name", "Scanner1");
        scannedBy.getFirst().put("date", "2024-12-27");


        Asset asset = new Asset(
                "example.com",
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                null,
                null
        );

        asset.setScannedBy(scannedBy);
        assetRepository.save(asset);

        ScanReport scanReport = new ScanReport(
                "Test ScanReport",
                asset,
                true
        );


        scanReportRepository.save(scanReport);


        Optional<ScanReport> foundScanReport = scanReportRepository.findByAssetDomain("example.com");

        assertTrue(foundScanReport.isPresent());
    }

}
