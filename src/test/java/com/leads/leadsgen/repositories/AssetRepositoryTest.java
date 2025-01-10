package com.leads.leadsgen.repositories;

import com.leads.leadsgen.models.Asset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AssetRepositoryTest {

    @Autowired
    private AssetRepository assetRepository;

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
