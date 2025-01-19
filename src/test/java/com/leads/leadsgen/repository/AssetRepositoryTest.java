package com.leads.leadsgen.repository;

import com.leads.leadsgen.model.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class AssetRepositoryTest {

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
    public void testFindByDomain() {

        Asset asset = new Asset(
                "example.com",
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new HashMap<>()
        );

        assetRepository.save(asset);

        Optional<Asset> foundAsset = assetRepository.findByDomain("example.com");

        assertTrue(foundAsset.isPresent());
    }
  
}
