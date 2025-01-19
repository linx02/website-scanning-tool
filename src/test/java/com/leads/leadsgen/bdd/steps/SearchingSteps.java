package com.leads.leadsgen.bdd.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leads.leadsgen.service.HttpClient;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class SearchingSteps {

    String query;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HttpClient httpClient;

    private MvcResult mvcResult;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Given("I want to find websites related to {string}")
    public void i_want_to_find_websites_related_to(String searchQuery) throws Exception {
        query = searchQuery;

        String mockedResponse = """
            {
                "organic": [
                    {"link": "https://example.com"},
                    {"link": "https://example.org"},
                    {"link": "https://example.se"}
                ]
            }
        """;
        when(httpClient.get(contains("https://google.serper.dev/search")))
                .thenReturn(mockedResponse);
    }

    @When("I perform a POST request to {string} with the query {string}")
    public void i_perform_a_post_request_to_with_the_query(String url, String searchQuery) throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                Map.of("query", searchQuery, "limit", 100)
        );

        mvcResult = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andReturn();
    }

    @Then("I should receive a list of relevant websites")
    public void i_should_receive_a_list_of_relevant_websites() throws Exception {
        assertThat(mvcResult.getResponse().getContentAsString()).contains("[");
        assertThat(mvcResult.getResponse().getContentAsString()).contains("]");
        assertThat(mvcResult.getResponse().getContentAsString()).contains(",");
    }

    @Then("the results should include domains")
    public void the_results_should_include_urls() throws UnsupportedEncodingException {
        String response = mvcResult.getResponse().getContentAsString();
        assertTrue(
                response.contains(".com") ||
                        response.contains(".org") ||
                        response.contains(".net") ||
                        response.contains(".gov") ||
                        response.contains(".edu") ||
                        response.contains(".io") ||
                        response.contains(".co") ||
                        response.contains(".se")
        );
    }
}