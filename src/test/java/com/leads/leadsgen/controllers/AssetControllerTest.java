package com.leads.leadsgen.controllers;

import com.leads.leadsgen.models.Asset;
import com.leads.leadsgen.repositories.AssetRepository;
import com.leads.leadsgen.repositories.ScanReportRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AssetController.class)
class AssetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssetRepository assetRepository;

    @MockitoBean
    private ScanReportRepository scanReportRepository;

    @Test
    void testGetAssets() throws Exception {
        List<Asset> mockAssets = List.of(
                new Asset("example.com", List.of("https://example.com"),
                        List.of("test@mail.com"), List.of("1234567890"), null, null)
        );
        when(assetRepository.findAll()).thenReturn(mockAssets);

        mockMvc.perform(get("/api/assets")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].domain").value("example.com"))
                .andExpect(jsonPath("$[0].urls[0]").value("https://example.com"))
                .andExpect(jsonPath("$[0].emails[0]").value("test@mail.com"))
                .andExpect(jsonPath("$[0].phones[0]").value("1234567890"));
    }
}