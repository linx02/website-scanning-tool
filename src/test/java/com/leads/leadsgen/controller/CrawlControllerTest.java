package com.leads.leadsgen.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leads.leadsgen.repository.AssetRepository;
import com.leads.leadsgen.service.CrawlService;
import com.leads.leadsgen.service.SseService;
import org.junit.jupiter.api.Test;
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

@WebMvcTest(CrawlController.class)
public class CrawlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssetRepository assetRepository;

    @MockitoBean
    private CrawlService crawlService;

    @MockitoBean
    private SseService sseService;

    @Test
    void testCrawlEndpoint() throws Exception {
        // Mock request body
        Map<String, List<String>> requestBody = Map.of(
                "domains", List.of("example.com")
        );

        // Convert the request body to JSON
        String requestJson = new ObjectMapper().writeValueAsString(requestBody);

        // Perform POST request and verify response
        mockMvc.perform(post("/api/crawl")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().string("Crawl initialized for domains: [example.com]"));

    }

    @Test
    void testStreamStatusEndpoint() throws Exception {
        // Mock request body
        Map<String, List<String>> requestBody = Map.of(
                "domains", List.of("example.com")
        );

        String requestJson = new ObjectMapper().writeValueAsString(requestBody);

        // Start SSE stream in a separate thread
        Thread sseClientThread = new Thread(() -> {
            try {
                MockHttpServletRequestBuilder streamRequest = get("/api/stream-crawl-status")
                        .accept(MediaType.TEXT_EVENT_STREAM);

                MvcResult mvcResult = mockMvc.perform(streamRequest)
                        .andExpect(status().isOk())
                        .andReturn();

                // Wait a bit for events to propagate
                Thread.sleep(5000);

                // Fetch streamed content
                String responseContent = mvcResult.getResponse().getContentAsString();

                // Assertions on the response
                assertThat(responseContent).contains("domain");
                assertThat(responseContent).contains("status");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Start the SSE client simulation
        sseClientThread.start();

        // Trigger the crawl
        mockMvc.perform(post("/api/crawl")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        // Wait for the SSE thread to complete
        sseClientThread.join();
    }
}