package com.leads.leadsgen.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leads.leadsgen.repository.AssetRepository;
import com.leads.leadsgen.repository.ScanReportRepository;
import com.leads.leadsgen.scanner.SeoScanner;
import com.leads.leadsgen.scanner.TrackerConsentScanner;
import com.leads.leadsgen.service.ScanService;
import com.leads.leadsgen.service.SseService;
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

    @MockitoBean
    private SseService sseService;

    @MockitoBean
    private ScanService scanService;

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

        // Start SSE stream in a separate thread
        Thread sseClientThread = new Thread(() -> {
            try {
                MockHttpServletRequestBuilder streamRequest = get("/api/stream-scan-status")
                        .accept(MediaType.TEXT_EVENT_STREAM);

                MvcResult mvcResult = mockMvc.perform(streamRequest)
                        .andExpect(status().isOk())
                        .andReturn();

                // Wait for events
                Thread.sleep(5000);

                // Verify streamed content
                String responseContent = mvcResult.getResponse().getContentAsString();
                assertThat(responseContent).contains("domain");
                assertThat(responseContent).contains("status");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Start the SSE simulation
        sseClientThread.start();

        // Trigger scan
        mockMvc.perform(post("/api/scan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        // Wait for SSE thread to complete
        sseClientThread.join();
    }
}