package com.leads.leadsgen.bdd.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leads.leadsgen.models.Asset;
import com.leads.leadsgen.repositories.AssetRepository;
import com.leads.leadsgen.services.HttpClient;
import io.cucumber.java.en.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ScanningSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AssetRepository assetRepository;

    private MvcResult mvcResult;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private HttpClient httpClient;

    @Given("an asset {string} exists in the database")
    public void an_asset_exists_in_the_database(String domain) throws Exception {
        Optional<Asset> existingAsset = assetRepository.findByDomain(domain);
        if (existingAsset.isEmpty()) {
            Asset asset = new Asset(
                    domain,
                    List.of("https://" + domain),
                    List.of(),
                    List.of(),
                    null,
                    null
            );
            assetRepository.save(asset);
        }

        when(httpClient.getHtml("https://" + domain))
                .thenReturn(Map.of("https://" + domain, "<html></html>"));
    }

    @When("I send a POST request to {string} with body:")
    public void i_send_a_post_request_to_with_body(String endpoint, String rawJson) throws Exception {
        mvcResult = mockMvc.perform(post(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rawJson))
                .andExpect(status().isOk())
                .andReturn();

        Thread.sleep(5000); // Wait for the scan to complete
    }

    @And("I send a GET request to {string} to retrieve the scan results")
    public void i_send_a_get_request_to_retrieve_the_scan_results(String endpoint) throws Exception {
        mvcResult = mockMvc.perform(get(endpoint))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Then("I should see a report for {string}")
    public void i_should_see_a_report_for(String domain) throws Exception {
        String jsonContent = mvcResult.getResponse().getContentAsString();

        assertThat(jsonContent).contains(domain);
        assertThat(jsonContent).contains("SEO Report");
    }
}