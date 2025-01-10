package com.leads.leadsgen.services;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SearchEngineServiceTest {

    private final SearchEngineService searchEngineService = new SearchEngineService();

    @Test
    public void testSearchEngineService() {
        List<String> results = searchEngineService.search("test", 100);
        assertTrue(!results.isEmpty() && results.size() <= 100);
    }

    @Test
    public void testSearchEngineService_InvalidLimit() {
        assertThrows(IllegalArgumentException.class, () -> searchEngineService.search("test", -1));
        assertThrows(IllegalArgumentException.class, () -> searchEngineService.search("test", 0));
        assertThrows(IllegalArgumentException.class, () -> searchEngineService.search("test", 99));
    }

}
