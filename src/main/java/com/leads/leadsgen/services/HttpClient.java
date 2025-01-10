package com.leads.leadsgen.services;

import java.util.Map;

public interface HttpClient {
    /**
     * Sends a GET request to the given URL and returns the response as a string.
     *
     * @param url the URL to send the GET request to
     * @return the response body as a string
     * @throws Exception if the request fails
     */
    String get(String url) throws Exception;

    /**
     * Sends a GET request to the given URL and returns the HTML content as a map.
     *
     * @param url the URL to fetch the HTML content from
     * @return a map containing the URL as the key and the HTML content as the value
     * @throws Exception if the request fails
     */
    Map<String, String> getHtml(String url) throws Exception;
}