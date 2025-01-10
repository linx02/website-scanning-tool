package com.leads.leadsgen.scanners;

import com.leads.leadsgen.models.Asset;
import com.leads.leadsgen.models.ScanReport;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SeoScannerTest {

    private final SeoScanner seoScanner = new SeoScanner();

    private Asset arrangeTestCase(Map<String, String> htmlContents) {
        List<String> urls = new ArrayList<>(htmlContents.keySet());
        return new Asset(
                "example.com",
                urls,
                List.of(),
                List.of(),
                List.of(),
                htmlContents
        );
    }

    private void assertContains(ScanReport scanReport, String issue) {
        assertTrue(scanReport.getReport().contains(issue));
    }

    @Test
    public void testScan_MissingAltTags() {
        Asset asset = arrangeTestCase(Map.of(
                "https://example.com", "<img src='image.jpg' />",
                "https://example.com/about", "<img src='image.jpg' alt='Image' />"
        ));

        ScanReport scanReport = seoScanner.scan(asset);

        assertContains(scanReport, "Missing alt attribute in img tag: <img src='image.jpg' />");
    }

    @Test
    public void testScan_BrokenLinks() {

    }

    @Test
    public void testScan_HeadingScenarios() {
        // Missing H1 tag, H1 tag too long
        Asset asset = arrangeTestCase(Map.of(
                "https://example.com", "<html></html>",
                "https://example.com/about", "<h1>Heading 1</h1>",
                "https://example.com/contact", "<h1>" + "a".repeat(71) + "</h1>"
        ));

        ScanReport scanReport = seoScanner.scan(asset);

        System.out.println(scanReport.getReport());

        assertContains(scanReport, "Missing H1 tag");
        assertContains(scanReport, "H1 tag too long (more than 70 characters)");
    }

    @Test
    public void testCrawl_MetaScenarios() {
        // Title missing, too long, too short
        Asset asset = arrangeTestCase(Map.of(
                "https://example.com", "<html></html>",
                "https://example.com/about", "<title>1</title>",
                "https://example.com/contact", "<title>" + "a".repeat(71) + "</title>"
        ));

        System.out.println(asset.getHtmlContents());

        // Meta Description missing, too long, too short
        Asset asset2 = arrangeTestCase(Map.of(
                "https://example.com", "<html></html>",
                "https://example.com/about", "<meta name='description' content='1' />",
                "https://example.com/contact", "<meta name='description' content='" + "a".repeat(161) + "' />"
        ));

        ScanReport scanReport = seoScanner.scan(asset);
        ScanReport scanReport2 = seoScanner.scan(asset2);

        System.out.println(scanReport.getReport());
        System.out.println(scanReport2.getReport());

        assertContains(scanReport, "Missing title tag");
        assertContains(scanReport, "Title tag too short (less than 10 characters)");
        assertContains(scanReport, "Title tag too long (more than 70 characters)");

        assertContains(scanReport2, "Meta description too short (less than 50 characters)");
        assertContains(scanReport2, "Meta description too long (more than 160 characters)");

    }

    @Test
    public void testCrawl_MissingFiles() {
    }

}
