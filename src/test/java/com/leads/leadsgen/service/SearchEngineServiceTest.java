package com.leads.leadsgen.service;

import com.leads.leadsgen.service.impl.HttpClientImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SearchEngineServiceTest {

    private final HttpClient httpClient = new HttpClientImpl();

    private final SearchEngineService searchEngineService = new SearchEngineService(httpClient);

    @Test
    public void testSearchEngineService_InvalidLimit() {
        assertThrows(IllegalArgumentException.class, () -> searchEngineService.search("test", -1));
        assertThrows(IllegalArgumentException.class, () -> searchEngineService.search("test", 0));
        assertThrows(IllegalArgumentException.class, () -> searchEngineService.search("test", 99));
    }

}
