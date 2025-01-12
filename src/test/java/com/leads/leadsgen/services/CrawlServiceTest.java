package com.leads.leadsgen.services;

import com.leads.leadsgen.models.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CrawlerServiceTest {

    private HttpClient httpClient;
    private CrawlerService crawlerService;

    @BeforeEach
    void setUp() {
        httpClient = mock(HttpClient.class);
        crawlerService = new CrawlerService(httpClient);
    }

    private void assertAsset(Asset asset, Asset expectedAsset) {
        assertEquals(expectedAsset.getDomain(), asset.getDomain(), "Domains should match");
        assertEquals(expectedAsset.getUrls(), asset.getUrls(), "Sitemap URLs should match");
        assertEquals(expectedAsset.getEmails(), asset.getEmails(), "Emails should match");
        assertEquals(expectedAsset.getPhones(), asset.getPhones(), "Phones should match");
    }

    private Asset arrangeTestCase(
            String url,
            Map<String, Boolean> exceptionMap, // URLs to throw exceptions for
            Map<String, String> responseMap,  // Mocked responses for URLs
            Asset expectedAsset               // Expected asset
    ) throws Exception {
        for (Map.Entry<String, Boolean> entry : exceptionMap.entrySet()) {
            String requestUrl = entry.getKey();
            boolean shouldThrow = entry.getValue();

            if (shouldThrow) {
                when(httpClient.get(requestUrl)).thenThrow(new Exception("Mocked exception for " + requestUrl));
                when(httpClient.getHtml(requestUrl)).thenThrow(new Exception("Mocked exception for " + requestUrl));
            } else {
                String mockedResponse = responseMap.getOrDefault(requestUrl, "");
                when(httpClient.get(requestUrl)).thenReturn(mockedResponse);
                when(httpClient.getHtml(requestUrl)).thenReturn(Map.of(requestUrl, mockedResponse));
            }
        }

        return expectedAsset;
    }

    @Test
    public void testCrawl_RobotsTxtScenarios() throws Exception {

        // Success: Unreachable robots.txt, valid sitemap
        Asset expectedAsset2 = arrangeTestCase(
                "https://example2.com",
                Map.of(
                        "https://example2.com/robots.txt", true, // Throw exception
                        "https://example2.com/sitemap.xml", false,
                        "https://example2.com/about", false,
                        "https://example2.com/contact", false
                ),
                Map.of(
                        "https://example2.com/sitemap.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><urlset xmlns=\"https://www.sitemaps.org/schemas/sitemap/0.9\">" +
                                "<url><loc>https://example2.com/about</loc></url>" +
                                "<url><loc>https://example2.com/contact</loc></url>" +
                                "</urlset>",
                        "https://example2.com/about", "<html></html>",
                        "https://example2.com/contact", "<html></html>"
                ),
                new Asset("example2.com", List.of("https://example2.com/about", "https://example2.com/contact", "https://example2.com"), List.of(), List.of(), null, null)
        );

        // Success: Reachable robots.txt but invalid syntax, valid sitemap
        Asset expectedAsset3 = arrangeTestCase(
                "https://example3.com",
                Map.of(
                        "https://example3.com/robots.txt", false,
                        "https://example3.com/sitemap.xml", false,
                        "https://example3.com/about", false,
                        "https://example3.com/contact", false
                ),
                Map.of(
                        "https://example3.com/robots.txt", "Invalid Robots Content",
                        "https://example3.com/sitemap.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><urlset xmlns=\"https://www.sitemaps.org/schemas/sitemap/0.9\">" +
                                "<url><loc>https://example3.com/about</loc></url>" +
                                "<url><loc>https://example3.com/contact</loc></url>" +
                                "</urlset>",
                        "https://example3.com/about", "<html></html>",
                        "https://example3.com/contact", "<html></html>"
                ),
                new Asset("example3.com", List.of("https://example3.com/about", "https://example3.com/contact", "https://example3.com"), List.of(), List.of(), null, null)
        );

        // TODO:
        // Success: Reachable robots.txt, Sitemap mentioned but no link, valid sitemap
        // Success: Reachable robots.txt, no sitemap mentioned, valid sitemap

        assertAsset(crawlerService.crawl("https://example2.com"), expectedAsset2);
        assertAsset(crawlerService.crawl("https://example3.com"), expectedAsset3);
    }

    @Test
    public void testCrawl_SitemapXmlScenarios() throws Exception {

        // Success: Valid robots.txt, unreachable sitemap.xml
        Asset expectedAsset2 = arrangeTestCase(
                "https://example5.com",
                Map.of(
                        "https://example5.com/robots.txt", false,
                        "https://example5.com/sitemap.xml", true,
                        "https://example5.com/about", false,
                        "https://example5.com/contact", false,
                        "https://example5.com", false
                ),
                Map.of(
                        "https://example5.com/robots.txt", "Sitemap: https://example5.com/sitemap.xml",
                        "https://example5.com/about", "<html></html>",
                        "https://example5.com/contact", "<html></html>",
                        "https://example5.com", "<html><a href='https://example5.com/about'>about</a><a href='https://example5.com/contact'>contact</a></html>"
                ),
                new Asset("example5.com", List.of("https://example5.com/about", "https://example5.com/contact", "https://example5.com"), List.of(), List.of(), null, null)
        );

        // Success: Valid robots.txt, sitemap.xml with invalid syntax
        Asset expectedAsset3 = arrangeTestCase(
                "https://example6.com",
                Map.of(
                        "https://example6.com/robots.txt", false,
                        "https://example6.com/sitemap.xml", false,
                        "https://example6.com/about", false,
                        "https://example6.com/contact", false,
                        "https://example6.com", false
                ),
                Map.of(
                        "https://example6.com/robots.txt", "Sitemap: https://example6.com/sitemap.xml",
                        "https://example6.com/sitemap.xml", "Invalid Sitemap Content",
                        "https://example6.com", "<html><a href='https://example6.com/about'>about</a><a href='https://example6.com/contact'>contact</a></html>"
                ),
                new Asset("example6.com", List.of("https://example6.com/about", "https://example6.com/contact", "https://example6.com"), List.of(), List.of(), null, null)
        );

        // TODO:
        // Success: Valid robots.txt, valid sitemap.xml but invalid & unreachable external sitemaps

        assertAsset(crawlerService.crawl("https://example5.com"), expectedAsset2);
        assertAsset(crawlerService.crawl("https://example6.com"), expectedAsset3);
    }

    @Test
    public void testCrawl_HtmlContentScenarios() throws Exception {

        // Success: Valid robots.txt, valid sitemap, but unreachable HTML content
        Asset expectedAsset2 = arrangeTestCase(
                "https://example8.com",
                Map.of(
                        "https://example8.com/robots.txt", false,
                        "https://example8.com/sitemap.xml", false,
                        "https://example8.com/about", true, // Throw exception,
                        "https://example8.com/contact", false
                ),
                Map.of(
                        "https://example8.com/robots.txt", "Sitemap: https://example8.com/sitemap.xml",
                        "https://example8.com/sitemap.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><urlset xmlns=\"https://www.sitemaps.org/schemas/sitemap/0.9\">" +
                                "<url><loc>https://example8.com/about</loc></url>" +
                                "<url><loc>https://example8.com/contact</loc></url>" +
                                "</urlset>",
                        "https://example8.com/contact", "<html><body><a href='tel:0763230063'>0763230063</a></body></html>"
                ),
                new Asset("example8.com", List.of("https://example8.com/about", "https://example8.com/contact", "https://example8.com"), List.of(), List.of("0763230063"), null, null)
        );

        // TODO:
        // Success: Valid robots.txt, valid sitemap, invalid HTML content

        assertAsset(crawlerService.crawl("https://example8.com"), expectedAsset2);
    }

}