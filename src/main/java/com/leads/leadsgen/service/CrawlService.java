package com.leads.leadsgen.service;

import com.leads.leadsgen.exception.CrawlerException;
import com.leads.leadsgen.model.Asset;
import com.leads.leadsgen.repository.AssetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CrawlService {

    private final HttpClient httpClient;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private SseService sseService;

    public CrawlService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Crawl a list of domains concurrently
     * @param domains List of domains to crawl
     */
    public void crawlDomains(List<String> domains) {
        domains.forEach(domain -> {
            Thread thread = new Thread(() -> crawlDomain(domain));
            thread.start();
        });
    }

    /**
     * Crawl a single domain
     * @param domain Domain to crawl
     */
    private void crawlDomain(String domain) {
        try {
            Thread.sleep(1000); // So SSE client can connect
            sseService.broadcastStatus(domain, "Crawling", Map.of());

            Asset asset = crawlWithTimeout("https://" + domain, 30, TimeUnit.SECONDS);

            Set<String> uniqueUrls = new LinkedHashSet<>(asset.getUrls());
            asset.setUrls(new ArrayList<>(uniqueUrls));

            if (assetRepository.findByDomain(domain).isPresent()) {
                sseService.broadcastStatus(domain, "Duplicate", Map.of());
                return;
            }

            assetRepository.save(asset);
            sseService.broadcastStatus(domain, "Completed", Map.of("suggestedEmail", getSuggestedEmail(asset)));
        } catch (CrawlerException.Timeout e) {
            Asset failedAsset = new Asset(domain, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), null, null);
            assetRepository.save(failedAsset);
            sseService.broadcastStatus(domain, "Timeout", Map.of());
        }
        catch (Exception e) {
            Asset failedAsset = new Asset(domain, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null);
            assetRepository.save(failedAsset);
            sseService.broadcastStatus(domain, "Error", Map.of("error", e.getMessage()));
        }
    }

    /**
     * Crawl a single domain and return the asset
     * @param url URL to crawl
     * @return Asset object representing the crawled domain
     * @throws CrawlerException if an error occurs during the crawl
     */
    public Asset crawl(String url) throws CrawlerException {
        List<String> urls = getUrls(url);

        if (!urls.contains(url)) {
            urls.add(url);
        }

        Map<String, String> htmlContents = new HashMap<>();

        for (String url_ : urls) {

            if (Thread.currentThread().isInterrupted()) {
                System.err.println("Thread was interrupted. Aborting crawl for URL: " + url);
                break;
            }

            try {
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

    /**
     * Crawl a single domain with a specified timeout
     * @param url URL to crawl
     * @param timeout Timeout duration
     * @param unit Timeout unit
     * @return Asset object representing the crawled domain
     * @throws CrawlerException if an error occurs during the crawl
     */
    public Asset crawlWithTimeout(String url, long timeout, TimeUnit unit) throws CrawlerException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Asset> future = executor.submit(() -> crawl(url));

            return future.get(timeout, unit);
        } catch (TimeoutException te) {
            throw new CrawlerException.Timeout("Crawl timed out for URL: " + url);
        } catch (ExecutionException | InterruptedException e) {
            throw new CrawlerException("Error during crawl for URL: " + url, e);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Get a list of URLs to crawl for a given domain
     * @param url URL to crawl
     * @return List of URLs to crawl
     */
    private List<String> getUrls(String url) {
        List<String> urls = new ArrayList<>();
        List<String> sitemapUrls = getSitemap(url);

        if (!sitemapUrls.isEmpty() && sitemapUrls.size() <= 20) {
            urls.addAll(sitemapUrls);
        }

        if (urls.isEmpty()) {
            urls.addAll(getPageLinks(url));
        }

        List<String> disallowedUrls = getDisallowedUrls(url);
        urls.removeAll(disallowedUrls);

        urls.removeIf(url_ -> !getDomainFromUrl(url_).equals(getDomainFromUrl(url)));

        return deduplicateUrls(urls);
    }

    /**
     * Remove duplicate URLs from a list
     * @param urls List of URLs
     * @return List of unique URLs
     */
    private List<String> deduplicateUrls(List<String> urls) {
        Set<String> uniqueUrls = new LinkedHashSet<>();
        for (String url : urls) {
            if (url.endsWith("/")) {
                uniqueUrls.add(url.substring(0, url.length() - 1));
            } else {
                uniqueUrls.add(url);
            }
        }
        return new ArrayList<>(uniqueUrls);
    }

    /**
     * Get sitemap URLs for a given domain
     * @param url Domain URL
     * @return List of sitemap URLs
     */
    private List<String> getSitemap(String url) {
        List<String> sitemapUrls = new ArrayList<>();
        Set<String> visitedSitemaps = new HashSet<>();
        Queue<String> sitemapQueue = new LinkedList<>();

        try {
            String robotsUrl = url + "/robots.txt";
            String robotsContent = httpClient.get(robotsUrl);

            String initialSitemapUrl = Arrays.stream(robotsContent.split("\n"))
                    .filter(line -> line.startsWith("Sitemap:"))
                    .map(line -> line.split("Sitemap:")[1].trim())
                    .findFirst()
                    .orElse(url + "/sitemap.xml");

            sitemapQueue.add(initialSitemapUrl);
        } catch (Exception e) {
            sitemapQueue.add(url + "/sitemap.xml");
        }

        while (!sitemapQueue.isEmpty()) {
            String currentSitemapUrl = sitemapQueue.poll();
            if (visitedSitemaps.contains(currentSitemapUrl)) {
                continue;
            }
            visitedSitemaps.add(currentSitemapUrl);

            try {
                String sitemapContent = httpClient.get(currentSitemapUrl);

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
                System.err.println("Error processing sitemap: " + currentSitemapUrl + ". Skipping...");
            }
        }

        return sitemapUrls;
    }

    /**
     * Get disallowed URLs from robots.txt for a given domain
     * @param url Domain URL
     * @return List of disallowed URLs
     */
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

    /**
     * Get links from html from a given URL
     * @param url URL to fetch links from
     * @return List of page links
     */
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
            System.err.println("Error fetching page links for URL: " + url);
        }
        return links;
    }

    /**
     * Get emails from HTML content
     * @param htmlContents Map of URLs to HTML content
     * @return Set of emails
     */
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

    /**
     * Get phone numbers from HTML content
     * @param htmlContents Map of URLs to HTML content
     * @return Set of phone numbers
     */
    private Set<String> getPhones(Map<String, String> htmlContents) {
        Set<String> phones = new HashSet<>();
        Pattern phonePattern = Pattern.compile(
                "(?:\\+\\d{1,3}[-.\\s]?)?" +
                        "(?:\\(\\d{1,4}\\)|\\d{1,4})?" +
                        "\\d{3,4}[-.\\s]?\\d{2,4}[-.\\s]?\\d{2,4}"
        );
        for (String html : htmlContents.values()) {
            Matcher matcher = phonePattern.matcher(html);
            while (matcher.find()) {
                phones.add(matcher.group());
            }
        }
        return phones;
    }

    /**
     * Get domain from URL
     * @param url URL to extract domain from
     * @return Domain
     */
    private String getDomainFromUrl(String url) {
        return url.replace("http://", "").replace("https://", "").split("/")[0];
    }

    /**
     * Suggest an email based on priority:
     * 1) info@domain
     * 2) anything@domain
     * 3) info@anything
     * 4) first email if none matched
     * @param asset Asset object
     * @return Suggested email
     */
    private String getSuggestedEmail(Asset asset) {
        List<String> emails = asset.getEmails();
        if (emails.isEmpty()) {
            return null;
        }

        String domainNoWWW = asset.getDomain().replace("www.", "").toLowerCase();

        String exactInfoDomain = "info@" + domainNoWWW;

        String anyDomainEmail = null;
        String infoAnyEmail = null;
        String firstEmail = emails.getFirst(); // fallback

        for (String email : emails) {
            String lowerEmail = email.toLowerCase().trim();

            // info@domain
            if (lowerEmail.equals(exactInfoDomain)) {
                return email;
            }

            // anything@domain
            if (anyDomainEmail == null && lowerEmail.endsWith("@" + domainNoWWW)) {
                anyDomainEmail = email;
            }

            // info@anything
            if (infoAnyEmail == null && lowerEmail.startsWith("info@")) {
                infoAnyEmail = email;
            }
        }

        if (anyDomainEmail != null) {
            return anyDomainEmail;
        }
        if (infoAnyEmail != null) {
            return infoAnyEmail;
        }
        return firstEmail;
    }
}