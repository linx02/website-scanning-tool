package com.leads.leadsgen.services;

import com.leads.leadsgen.exceptions.CrawlerException;
import com.leads.leadsgen.models.Asset;
import org.springframework.stereotype.Service;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CrawlerService {

    private final HttpClient httpClient;

    public CrawlerService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public Asset crawl(String url) throws CrawlerException {
        List<String> urls = getUrls(url);

        if (urls.isEmpty()) {
            System.out.println("No URLs found for domain: " + getDomainFromUrl(url) + ". Scanning only the provided URL.");
        }

        if (!urls.contains(url)) {
            urls.add(url);
        }

        Map<String, String> htmlContents = new HashMap<>();

        for (String url_ : urls) {
            try {
                System.out.println("Processing URL: " + url_);
                Map<String, String> html = httpClient.getHtml(url_);
                if (!html.isEmpty() && html.get(url_) != null) {
                    htmlContents.put(url_, html.get(url_));
                }
            } catch (Exception e) {
                System.err.println("Error processing URL: " + url_ + ". Skipping... " + e.getMessage());
            }
        }

        if (htmlContents.isEmpty()) {
            throw new CrawlerException("No HTML content found for domain: " + getDomainFromUrl(url));
        }

        Set<String> emails = getEmails(htmlContents);
        Set<String> phones = getPhones(htmlContents);

        return new Asset(
                getDomainFromUrl(url),
                urls,
                new ArrayList<>(emails),
                new ArrayList<>(phones),
                null,
                htmlContents
        );
    }

    private List<String> getUrls(String url) {
        List<String> urls = new ArrayList<>();
        List<String> disallowedUrls = new ArrayList<>();

        List<String> sitemapUrls = getSitemap(url);
        if (!sitemapUrls.isEmpty() && sitemapUrls.size() <= 20) {
            urls.addAll(sitemapUrls);
        }

        if (sitemapUrls.size() > 20) {
            System.out.println("Sitemap too large for domain: " + getDomainFromUrl(url));
        }

        if (urls.isEmpty()) {
            System.out.println("Running link-based crawling for domain: " + getDomainFromUrl(url));
            List<String> pageLinks = getPageLinks(url);
            urls.addAll(pageLinks);
        }

        disallowedUrls = getDisallowedUrls(url);

        urls.removeAll(disallowedUrls);

        urls.removeIf(url_ -> !getDomainFromUrl(url_).equals(getDomainFromUrl(url)));

        return urls;
    }

    private List<String> getSitemap(String url) {
        List<String> sitemapUrls = new ArrayList<>();
        Set<String> visitedSitemaps = new HashSet<>(); // To prevent cycles in recursion
        Queue<String> sitemapQueue = new LinkedList<>(); // For managing recursive sitemap processing

        try {
            // Try to fetch the sitemap URL from robots.txt
            String robotsUrl = url + "/robots.txt";
            String robotsContent = httpClient.get(robotsUrl);

            String initialSitemapUrl = Arrays.stream(robotsContent.split("\n"))
                    .filter(line -> line.startsWith("Sitemap:"))
                    .map(line -> line.split("Sitemap:")[1].trim())
                    .findFirst()
                    .orElse(url + "/sitemap.xml"); // Fallback to /sitemap.xml

            sitemapQueue.add(initialSitemapUrl);

        } catch (Exception e) {
            System.err.println("Error fetching robots.txt: " + e.getMessage() + ". Falling back to default sitemap.xml.");
            sitemapQueue.add(url + "/sitemap.xml");
        }

        // Process the sitemaps recursively using the queue
        while (!sitemapQueue.isEmpty()) {
            String currentSitemapUrl = sitemapQueue.poll();

            // Avoid re-processing the same sitemap
            if (visitedSitemaps.contains(currentSitemapUrl)) {
                continue;
            }
            visitedSitemaps.add(currentSitemapUrl);

            try {
                String sitemapContent = httpClient.get(currentSitemapUrl);

                // Parse sitemap content
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                org.w3c.dom.Document document = builder.parse(new ByteArrayInputStream(sitemapContent.getBytes()));

                org.w3c.dom.NodeList locNodes = document.getElementsByTagName("loc");
                for (int i = 0; i < locNodes.getLength(); i++) {
                    String loc = locNodes.item(i).getTextContent();

                    if (loc.endsWith(".xml")) {
                        sitemapQueue.add(loc);
                    } else {
                        sitemapUrls.add(loc);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error fetching or parsing sitemap: " + currentSitemapUrl + ". Skipping... " + e.getMessage());
            }
        }

        return sitemapUrls;
    }

    private List<String> getDisallowedUrls(String url) {
        List<String> disallowedUrls = new ArrayList<>();
        try {
            String robotsContent = httpClient.get(url + "/robots.txt");
            Arrays.stream(robotsContent.split("\n"))
                    .filter(line -> line.startsWith("Disallow:"))
                    .map(line -> line.split("Disallow:")[1].trim())
                    .forEach(disallowedUrls::add);
        } catch (Exception e) {
            System.err.println("Error fetching robots.txt: " + e.getMessage());
        }
        return disallowedUrls;
    }

    private List<String> getPageLinks(String url) {
        List<String> links = new ArrayList<>();
        try {
            Map<String, String> htmlContent = httpClient.getHtml(url);
            String html = htmlContent.getOrDefault(url, "");
            Matcher matcher = Pattern.compile("<a\\s+(?:[^>]*?\\s+)?href=([\"'])(.*?)\\1").matcher(html);

            while (matcher.find()) {
                String link = matcher.group(2);
                if (link.startsWith("/")) {
                    link = url + link;
                }
                links.add(link);
            }
        } catch (Exception e) {
            System.err.println("Error fetching page links for URL: " + url + ". " + e.getMessage());
        }
        return links;
    }

    private Set<String> getEmails(Map<String, String> htmlContents) {
        Set<String> emails = new HashSet<>();
        Pattern emailPattern = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
        for (String html : htmlContents.values()) {
                Matcher matcher = emailPattern.matcher(html);
                while (matcher.find()) {
                    emails.add(matcher.group());
            }
        }
        return emails;
    }

    private Set<String> getPhones(Map<String, String> htmlContents) {
        Set<String> phones = new HashSet<>();
        Pattern phonePattern = Pattern.compile("\\+46\\s?(?:\\(0\\))?[1-9]\\d{1,2}[-.\\s]?\\d{2,3}[-.\\s]?\\d{2,3}[-.\\s]?\\d{2,4}|0[1-9]\\d{1,2}[-.\\s]?\\d{2,3}[-.\\s]?\\d{2,3}[-.\\s]?\\d{2,4}");
        for (String html : htmlContents.values()) {
                Matcher matcher = phonePattern.matcher(html);
                while (matcher.find()) {
                    phones.add(matcher.group());
                }
            }

        return phones;
    }

    private String getDomainFromUrl(String url) {
        return url.replace("http://", "").replace("https://", "").split("/")[0];
    }
}