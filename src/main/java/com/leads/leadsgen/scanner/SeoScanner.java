package com.leads.leadsgen.scanner;

import com.leads.leadsgen.model.Asset;
import com.leads.leadsgen.model.ScanReport;
import com.leads.leadsgen.service.CrawlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SeoScanner extends Scanner {

    @Autowired
    private CrawlService crawlService;

    public SeoScanner() {
        super("SeoScanner");
    }

    /**
     * Scan the SEO of an asset
     *
     * @param asset Asset to scan
     * @return Scan report
     */
    @Override
    public ScanReport scan(Asset asset) {

        System.out.println("Scanning SEO for: " + asset.getDomain());

        StringBuilder reportBuilder = new StringBuilder();
        Map<String, List<String>> issuesPerUrl = new HashMap<>();
        int totalChecks = 0;
        int wellOptimizedChecks = 0;

        reportBuilder.append("SEO Report for: ").append(asset.getDomain()).append("\n");
        reportBuilder.append("=============================================================\n");

        // Check for broken links
        List<String> brokenLinks = checkBrokenLinksInSitemap(asset.getUrls());
        if (!brokenLinks.isEmpty()) {
            totalChecks += brokenLinks.size();
            for (String brokenLink : brokenLinks) {
                issuesPerUrl.put(brokenLink, Collections.singletonList("Broken link in sitemap"));
            }
        }

        System.out.println("Scanning HTML content...");
        System.out.println(asset.getHtmlContents());

        if (asset.getHtmlContents() == null) {
            try {
                Asset updatedAsset = crawlService.crawl("https://" + asset.getDomain());
                asset.setHtmlContents(updatedAsset.getHtmlContents());
            } catch (Exception e) {
                System.out.println("Error crawling asset: " + asset.getDomain());
                return new ScanReport("Error crawling asset: " + asset.getDomain(), asset, true);
            }
        }

        // Scan HTML content
        for (String url : asset.getHtmlContents().keySet()) {
                String htmlContent = asset.getHtmlContents().get(url);


                if (htmlContent != null) {
                    List<String> urlIssues = new ArrayList<>();
                    int[] result = scanHtmlContent(htmlContent, urlIssues, url);
                    totalChecks += result[0];
                    wellOptimizedChecks += result[1];

                    if (!urlIssues.isEmpty()) {
                        issuesPerUrl.put(url, urlIssues);
                    }

                }
        }

        System.out.println("Scanning asset level issues...");

        // Check for missing files like robots.txt and sitemap.xml
        List<String> assetLevelIssues = new ArrayList<>();
        int[] assetChecks = checkMissingFiles(asset, assetLevelIssues);
        totalChecks += assetChecks[0];
        wellOptimizedChecks += assetChecks[1];

        if (!assetLevelIssues.isEmpty()) {
            issuesPerUrl.put(asset.getDomain(), assetLevelIssues);
        }

        System.out.println("Calculating score...");

        int totalScore = calculateScore(totalChecks, wellOptimizedChecks);

        reportBuilder.append("Total Score: ").append(totalScore).append("/100").append("\n\n");

        if (!issuesPerUrl.isEmpty()) {
            System.out.println(issuesPerUrl);
            reportBuilder.append("Issues to fix:\n");
            for (Map.Entry<String, List<String>> entry : issuesPerUrl.entrySet()) {
                reportBuilder.append("URL: ").append(entry.getKey()).append("\n");
                for (String issue : entry.getValue()) {
                    reportBuilder.append(" - ").append(issue).append("\n");
                }
            }
        } else {
            reportBuilder.append("No issues found. The SEO is perfect!\n");
        }

        boolean flagged = totalScore < 70;

        System.out.println("SEO scan complete for: " + asset.getDomain());

        return new ScanReport(reportBuilder.toString(), asset, flagged);
    }

    /**
     * Scan the HTML content of a page
     *
     * @param htmlContent HTML content to scan
     * @param issues      List to store issues
     * @param url         URL of the page
     * @return Array containing total checks and well optimized checks
     */
    private int[] scanHtmlContent(String htmlContent, List<String> issues, String url) {
        int totalChecks = 0;
        int wellOptimizedChecks = 0;

        int[] result = checkTitleTag(htmlContent, issues);
        totalChecks += result[0];
        wellOptimizedChecks += result[1];

        result = checkMetaDescription(htmlContent, issues);
        totalChecks += result[0];
        wellOptimizedChecks += result[1];

        result = checkHeadingTags(htmlContent, issues);
        totalChecks += result[0];
        wellOptimizedChecks += result[1];

        result = checkImageAltTags(htmlContent, issues);
        totalChecks += result[0];
        wellOptimizedChecks += result[1];

        result = checkSchemaMarkup(htmlContent, issues);
        totalChecks += result[0];
        wellOptimizedChecks += result[1];

        result = checkOpenGraphTags(htmlContent, issues);
        totalChecks += result[0];
        wellOptimizedChecks += result[1];

        result = checkTwitterCardTags(htmlContent, issues);
        totalChecks += result[0];
        wellOptimizedChecks += result[1];

        result = checkBrokenLinksInHtml(htmlContent, issues, url);
        totalChecks += result[0];
        wellOptimizedChecks += result[1];

        return new int[]{totalChecks, wellOptimizedChecks};
    }

    /**
     * Check for broken links in the sitemap
     *
     * @param sitemapUrls List of URLs in the sitemap
     * @return List of broken links
     */
    private List<String> checkBrokenLinksInSitemap(List<String> sitemapUrls) {
        List<String> brokenLinks = new ArrayList<>();
        for (String url : sitemapUrls) {
            if (isLinkBroken(url)) {
                brokenLinks.add(url);
            }
        }
        return brokenLinks;
    }

    /**
     * Check if a link is broken
     *
     * @param url URL to check
     * @return True if the link is broken, false otherwise
     */
    private boolean isLinkBroken(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            int responseCode = connection.getResponseCode();
            return responseCode >= 400;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Calculate the SEO score
     *
     * @param totalChecks          Total number of checks
     * @param wellOptimizedChecks  Number of well optimized checks
     * @return SEO score
     */
    private int calculateScore(int totalChecks, int wellOptimizedChecks) {
        if (totalChecks == 0) {
            return 100;
        }
        return (wellOptimizedChecks * 100) / totalChecks;
    }

    /**
     * Check for missing files like robots.txt and sitemap.xml
     *
     * @param asset  Asset to check
     * @param issues List to store issues
     * @return Array containing total checks and well optimized checks
     */
    private int[] checkMissingFiles(Asset asset, List<String> issues) {
        int totalChecks = 2; // sitemap.xml & robots.txt
        int wellOptimizedChecks = 0;

        if (asset.getUrls() != null && !asset.getUrls().isEmpty()) {
            wellOptimizedChecks++;
        } else {
            issues.add("Missing sitemap.xml");
        }

        if (asset.getUrls() != null && !asset.getUrls().isEmpty()) {
            wellOptimizedChecks++;
        } else {
            issues.add("Missing robots.txt");
        }

        return new int[]{totalChecks, wellOptimizedChecks};
    }

    /**
     * Check the title tag of a page
     *
     * @param html   HTML content of the page
     * @param issues List to store issues
     * @return Array containing total checks and well optimized checks
     */
    private int[] checkTitleTag(String html, List<String> issues) {
        int totalChecks = 1;
        int wellOptimizedChecks = 0;

        String titleRegex = "<title>(.*?)</title>";
        Matcher matcher = Pattern.compile(titleRegex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(html);

        if (matcher.find()) {
            String title = matcher.group(1).trim();
            if (title.length() >= 10 && title.length() <= 70) {
                wellOptimizedChecks++;
            } else if (title.length() < 10) {
                issues.add("Title tag too short (less than 10 characters)");
            } else if (title.length() > 70) {
                issues.add("Title tag too long (more than 70 characters)");
            }
        } else {
            issues.add("Missing title tag");
        }

        return new int[]{totalChecks, wellOptimizedChecks};
    }

    /**
     * Check the meta description of a page
     *
     * @param html   HTML content of the page
     * @param issues List to store issues
     * @return Array containing total checks and well optimized checks
     */
    private int[] checkMetaDescription(String html, List<String> issues) {
        int totalChecks = 1;
        int wellOptimizedChecks = 0;

        String metaDescriptionRegex = "<meta\\s+(?:[a-zA-Z-]+=[\"'].*?[\"']\\s+)*name=[\"']description[\"']\\s+content=[\"'](.*?)[\"'].*?>";
        Matcher matcher = Pattern.compile(metaDescriptionRegex, Pattern.CASE_INSENSITIVE).matcher(html);

        if (matcher.find()) {
            String metaDescription = matcher.group(1).trim();
            if (metaDescription.length() >= 50 && metaDescription.length() <= 160) {
                wellOptimizedChecks++;
            } else if (metaDescription.length() < 50) {
                issues.add("Meta description too short (less than 50 characters)");
            } else if (metaDescription.length() > 160) {
                issues.add("Meta description too long (more than 160 characters)");
            }
        } else {
            issues.add("Missing meta description");
        }

        return new int[]{totalChecks, wellOptimizedChecks};
    }

    /**
     * Check the heading tags of a page
     *
     * @param html   HTML content of the page
     * @param issues List to store issues
     * @return Array containing total checks and well optimized checks
     */
    private int[] checkHeadingTags(String html, List<String> issues) {
        int totalChecks = 1;
        int wellOptimizedChecks = 0;

        String h1Regex = "<h1[^>]*>(.*?)</h1>";
        Matcher matcher = Pattern.compile(h1Regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(html);

        if (matcher.find()) {
            String h1Text = matcher.group(1).trim();
            if (!h1Text.isEmpty() && h1Text.length() <= 70) {
                wellOptimizedChecks++;
            } else if (h1Text.length() > 70) {
                issues.add("H1 tag too long (more than 70 characters)");
            }
        } else {
            issues.add("Missing H1 tag");
        }

        return new int[]{totalChecks, wellOptimizedChecks};
    }

    /**
     * Check the alt tags of images in the page
     *
     * @param html   HTML content of the page
     * @param issues List to store issues
     * @return Array containing total checks and well optimized checks
     */
    private int[] checkImageAltTags(String html, List<String> issues) {
        int totalChecks = 0;
        int wellOptimizedChecks = 0;

        String imgRegex = "<img[^>]*>";
        Matcher matcher = Pattern.compile(imgRegex, Pattern.CASE_INSENSITIVE).matcher(html);

        while (matcher.find()) {
            totalChecks++;
            String imgTag = matcher.group();
            if (!imgTag.contains("alt=")) {
                issues.add("Missing alt attribute in img tag: " + imgTag);
            } else {
                String altRegex = "alt=[\"'](.*?)[\"']";
                Matcher altMatcher = Pattern.compile(altRegex, Pattern.CASE_INSENSITIVE).matcher(imgTag);
                if (altMatcher.find()) {
                    String altText = altMatcher.group(1).trim();
                    if (altText.length() <= 100) {
                        wellOptimizedChecks++;
                    } else {
                        issues.add("Alt attribute too long (more than 100 characters): " + altText);
                    }
                }
            }
        }

        return new int[]{totalChecks, wellOptimizedChecks};
    }

    /**
     * Check for schema.org markup in the page
     *
     * @param html   HTML content of the page
     * @param issues List to store issues
     * @return Array containing total checks and well optimized checks
     */
    private int[] checkSchemaMarkup(String html, List<String> issues) {
        int totalChecks = 1;
        int wellOptimizedChecks = 0;

        if (html.toLowerCase().contains("schema.org")) {
            wellOptimizedChecks++;
        } else {
            issues.add("Missing schema.org markup");
        }

        return new int[]{totalChecks, wellOptimizedChecks};
    }

    /**
     * Check for Open Graph tags in the page
     *
     * @param html   HTML content of the page
     * @param issues List to store issues
     * @return Array containing total checks and well optimized checks
     */
    private int[] checkOpenGraphTags(String html, List<String> issues) {
        int totalChecks = 1;
        int wellOptimizedChecks = 0;

        String ogTagRegex = "<meta\\s+property=[\"']og:";
        Matcher matcher = Pattern.compile(ogTagRegex, Pattern.CASE_INSENSITIVE).matcher(html);

        if (matcher.find()) {
            wellOptimizedChecks++;
        } else {
            issues.add("Missing Open Graph markup");
        }

        return new int[]{totalChecks, wellOptimizedChecks};
    }

    /**
     * Check for Twitter Card tags in the page
     *
     * @param html   HTML content of the page
     * @param issues List to store issues
     * @return Array containing total checks and well optimized checks
     */
    private int[] checkTwitterCardTags(String html, List<String> issues) {
        int totalChecks = 1;
        int wellOptimizedChecks = 0;

        String twitterCardRegex = "<meta\\s+name=[\"']twitter:";
        Matcher matcher = Pattern.compile(twitterCardRegex, Pattern.CASE_INSENSITIVE).matcher(html);

        if (matcher.find()) {
            wellOptimizedChecks++;
        } else {
            issues.add("Missing Twitter Card markup");
        }

        return new int[]{totalChecks, wellOptimizedChecks};
    }

    /**
     * Check for broken links in the HTML content
     *
     * @param html   HTML content of the page
     * @param issues List to store issues
     * @param pageUrl URL of the page
     * @return Array containing total checks and well optimized checks
     */
    private int[] checkBrokenLinksInHtml(String html, List<String> issues, String pageUrl) {
        int totalChecks = 0;
        int wellOptimizedChecks = 0;

        String linkRegex = "<a\\s+href=[\"'](http[s]?://.*?)[\"'].*?>";
        Matcher matcher = Pattern.compile(linkRegex, Pattern.CASE_INSENSITIVE).matcher(html);

        while (matcher.find()) {
            totalChecks++;
            String link = matcher.group(1);
            if (isLinkBroken(link)) {
                issues.add("Broken link: " + link);
            } else {
                wellOptimizedChecks++;
            }
        }

        return new int[]{totalChecks, wellOptimizedChecks};
    }
}