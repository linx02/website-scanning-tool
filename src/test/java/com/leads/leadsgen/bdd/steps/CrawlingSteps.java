package com.leads.leadsgen.bdd.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leads.leadsgen.service.HttpClient;
import io.cucumber.java.en.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class CrawlingSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HttpClient httpClient;

    private MvcResult mvcResult;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Given("the domain {string} returns valid HTML")
    public void the_domain_will_return_valid_html(String domain) throws Exception {
        Mockito.when(httpClient.getHtml("https://" + domain))
                .thenReturn(Map.of("https://" + domain, "<html>test@mail.com 0763230063</html>"));

        Mockito.when(httpClient.get(contains(domain)))
                .thenReturn("User-agent: *\nDisallow:");
    }

    @When("I send a POST request to {string} with domains [{string}]")
    public void i_send_a_post_request_to_with_domains(String endpoint, String singleDomain) throws Exception {
        // Build JSON: { "domains": ["example.com"] }
        String requestBody = objectMapper.writeValueAsString(
                Map.of("domains", List.of(singleDomain))
        );

        mvcResult = mockMvc.perform(post(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andReturn();

        Thread.sleep(5000);
    }

    @Then("I should get a {int} response")
    public void i_should_get_a_response(int status) {
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(status);
    }

    @And("when I GET {string}")
    public void when_I_GET(String endpoint) throws Exception {
        mvcResult = mockMvc.perform(get(endpoint))
                .andReturn();
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(200);
    }

    @Then("the response should contain an asset for {string}")
    public void the_response_should_contain_an_asset_for(String domain) throws Exception {
        String jsonContent = mvcResult.getResponse().getContentAsString();
        System.out.println("Response from " + mvcResult.getRequest().getRequestURI() + " = " + jsonContent);

        assertThat(jsonContent).contains(domain);
        assertThat(jsonContent).contains("test@mail.com");
        assertThat(jsonContent).contains("0763230063");
    }
}