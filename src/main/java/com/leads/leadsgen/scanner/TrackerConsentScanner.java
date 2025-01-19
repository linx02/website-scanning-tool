package com.leads.leadsgen.scanner;

import com.leads.leadsgen.model.Asset;
import com.leads.leadsgen.model.ScanReport;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TrackerConsentScanner extends Scanner {

    private static final String SELENIUM_SERVER_URL = "http://selenium-standalone:4444/wd/hub";
    private static final int PAGE_LOAD_WAIT_TIME_MS = 5000;

    public TrackerConsentScanner() {
        super("TrackerConsentScanner");
    }

    /**
     * Scan for Google Analytics g/collect requests firing on page load without user interaction.
     *
     * @param asset Asset to scan
     * @return ScanReport
     */
    @Override
    public ScanReport scan(Asset asset) {
        List<String> foundRequests = null;

        BrowserMobProxy proxy = new BrowserMobProxyServer();
        proxy.setTrustAllServers(true);
        proxy.start(0);

        Proxy seleniumProxy = ClientUtil.createSeleniumProxy(proxy);

        ChromeOptions options = setupChromeOptions(seleniumProxy);

        WebDriver driver = null;

        try {
            driver = new RemoteWebDriver(getSeleniumServerUrl(), options);

            proxy.newHar("tracker-scan");

            driver.get(asset.getUrls().getFirst());

            Thread.sleep(PAGE_LOAD_WAIT_TIME_MS);

            foundRequests = extractAnalyticsRequests(proxy);

            foundRequests.forEach(request -> System.out.println("Captured Google Analytics Request: " + request));

        } catch (Exception e) {
            System.err.println("Error scanning for trackers: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
            }
            proxy.stop();
        }

        boolean hasViolations = foundRequests != null && !foundRequests.isEmpty();
        return new ScanReport("Google Analytics g/collect requests detected: " + foundRequests, asset, hasViolations);
    }

    /**
     * Setup Chrome options for Selenium WebDriver.
     *
     * @param seleniumProxy The Selenium proxy.
     * @return Configured ChromeOptions.
     */
    private ChromeOptions setupChromeOptions(Proxy seleniumProxy) {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--ignore-ssl-errors=yes");
        options.addArguments("--allow-insecure-localhost");
        options.addArguments("--disable-web-security");
        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.setCapability(CapabilityType.PROXY, seleniumProxy);
        return options;
    }

    /**
     * Get the Selenium server URL.
     *
     * @return The URL of the Selenium server.
     * @throws MalformedURLException If the URL is invalid.
     */
    private URL getSeleniumServerUrl() throws MalformedURLException {
        return new URL(SELENIUM_SERVER_URL);
    }

    /**
     * Extract Google Analytics requests from network traffic.
     *
     * @param proxy The BrowserMobProxy instance.
     * @return List of matched URLs.
     */
    private List<String> extractAnalyticsRequests(BrowserMobProxy proxy) {
        return proxy.getHar().getLog().getEntries().stream()
                .map(entry -> entry.getRequest().getUrl())
                .filter(url -> url.matches("https://[a-zA-Z0-9.-]*google-analytics\\.com/g/collect.*"))
                .collect(Collectors.toList());
    }
}