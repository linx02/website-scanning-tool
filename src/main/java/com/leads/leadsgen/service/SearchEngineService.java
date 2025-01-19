package com.leads.leadsgen.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SearchEngineService {

    @Value("${apikeys.serper}")
    private String API_KEY;

    private final HttpClient httpClient;

    public SearchEngineService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Search for websites based on the given query and length.
     *
     * @param query  the search query
     * @param length the number of results to return (must be a multiple of 100)
     * @return a list of domains
     */
    public List<String> search(String query, int length) {
        if (length % 100 != 0 || length <= 0) {
            throw new IllegalArgumentException("Length must be a multiple of 100 and greater than 0");
        }

        String apiUrl = "https://google.serper.dev/search?q=" + encodeQuery(query) +
                "&gl=se&hl=sv&num=100&apiKey=" + API_KEY;

        Set<String> domains = new HashSet<>();
        try {
            String response = httpClient.get(apiUrl);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response);

            JsonNode organicResults = rootNode.get("organic");
            if (organicResults != null && organicResults.isArray()) {
                for (JsonNode result : organicResults) {
                    String link = result.get("link").asText();
                    String domain = extractDomain(link);
                    domains.add(domain);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error during search: " + e.getMessage(), e);
        }

        return new ArrayList<>(domains);
    }

    /**
     * Extracts the domain from a given URL.
     *
     * @param url the URL to extract the domain from
     * @return the domain
     */
    private String extractDomain(String url) {
        try {
            java.net.URL parsedUrl = new java.net.URL(url);
            String host = parsedUrl.getHost();
            return host;
        } catch (Exception e) {
            throw new RuntimeException("Invalid URL: " + url, e);
        }
    }

    private String encodeQuery(String query) {
        try {
            return java.net.URLEncoder.encode(query, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("Error encoding query: " + e.getMessage(), e);
        }
    }
}