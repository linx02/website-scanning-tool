package com.leads.leadsgen.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SearchEngineService {

    public List<String> search(String query, int length) {
        if (length % 100 != 0 || length <= 0) {
            throw new IllegalArgumentException("Length must be a multiple of 100 and greater than 0");
        }

        // Temporary implementation to read JSON file and extract domains
        Set<String> domains = new HashSet<>();
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("data.json");
            if (inputStream == null) {
                throw new RuntimeException("data.json not found in resources");
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(inputStream);

            JsonNode organicResults = rootNode.get("organic");
            if (organicResults != null && organicResults.isArray()) {
                for (JsonNode result : organicResults) {
                    String link = result.get("link").asText();
                    String domain = extractDomain(link);
                    domains.add(domain);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read or parse JSON file: " + e.getMessage(), e);
        }

        return new ArrayList<>(domains);
    }

    private String extractDomain(String url) {
        try {
            URL parsedUrl = new URL(url);
            String host = parsedUrl.getHost();

            //if (host.startsWith("www.")) {
            //    host = host.substring(4);
            //}
            return host;
        } catch (Exception e) {
            throw new RuntimeException("Invalid URL: " + url, e);
        }
    }
}