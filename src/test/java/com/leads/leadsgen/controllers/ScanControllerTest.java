package com.leads.leadsgen.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leads.leadsgen.repositories.AssetRepository;
import com.leads.leadsgen.repositories.ScanReportRepository;
import com.leads.leadsgen.scanners.SeoScanner;
import com.leads.leadsgen.scanners.TrackerConsentScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ScanController.class)
class ScanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssetRepository assetRepository;

    @MockitoBean
    private ScanReportRepository scanReportRepository;

    @MockitoBean
    private SeoScanner seoScanner;

    @MockitoBean
    private TrackerConsentScanner trackerConsentScanner;

//    @Mock
//    private ScanService scanService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testScanEndpoint() throws Exception {
        // Mock request body
        Map<String, List<String>> requestBody = Map.of(
                "domains", List.of("example.com"),
                "scanners", List.of("SeoScanner")
        );

        String requestJson = new ObjectMapper().writeValueAsString(requestBody);

        mockMvc.perform(post("/api/scan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().string("Scan initialized for domains: [example.com]"));

//        verify(scanService, times(1)).scanDomains(
//                eq(List.of("example.com")),
//                eq(List.of("SeoScanner", "TrackerConsentScanner"))
//        );
    }

    @Test
    void testStreamStatusEndpoint() throws Exception {
        Map<String, List<String>> requestBody = Map.of(
                "domains", List.of("example.com"),
                "scanners", List.of("SeoScanner")
        );

        String requestJson = new ObjectMapper().writeValueAsString(requestBody);

        // Start SSE stream in separate thread to keep it open
        MockHttpServletRequestBuilder streamRequest = get("/api/stream-scan-status")
                .accept(MediaType.TEXT_EVENT_STREAM);

        MvcResult mvcResult = mockMvc.perform(streamRequest)
                .andExpect(status().isOk())
                .andReturn();

        // Trigger scan
        mockMvc.perform(post("/api/scan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        // Wait for some time to allow the scan and event emission
        Thread.sleep(5000);

        String responseContent = mvcResult.getResponse().getContentAsString();

        assertThat(responseContent).contains("domain");
        assertThat(responseContent).contains("status");
    }
}