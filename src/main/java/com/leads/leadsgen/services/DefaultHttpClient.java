package com.leads.leadsgen.services;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Component
public class DefaultHttpClient implements HttpClient {

    @Override
    public String get(String url) throws Exception {
        System.out.println("Fetching: " + url);
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        return response.toString();
    }

    @Override
    public Map<String, String> getHtml(String url) throws Exception {
        System.out.println("Fetching HTML content from URL: " + url);
        Map<String, String> htmlContent = new HashMap<>();
        String html = get(url);
        htmlContent.put(url, html);
        return htmlContent;
    }
}