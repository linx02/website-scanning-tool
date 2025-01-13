package com.leads.leadsgen.scanners;

import com.leads.leadsgen.models.Asset;
import com.leads.leadsgen.models.ScanReport;
import io.github.bonigarcia.wdm.WebDriverManager;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class TrackerConsentScanner extends Scanner {

    public TrackerConsentScanner() {
        super("TrackerConsentScanner");
    }

    /**
     * Scan for trackers firing on page load without user interaction
     *
     * @param asset Asset to scan
     * @return ScanReport
     */
    @Override
    public ScanReport scan(Asset asset) {
        List<String> foundTrackers = null;

        List<String> trackers = Arrays.asList(
                "googletagmanager", "analytics", "facebook.net", "doubleclick",
                "taboola", "criteo", "hotjar", "clarity", "track", "pixel", "session_id", "uid"
        );

        BrowserMobProxy proxy = new BrowserMobProxyServer();
        proxy.setTrustAllServers(true);
        proxy.start(0);

        Proxy seleniumProxy = ClientUtil.createSeleniumProxy(proxy);

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.setProxy(seleniumProxy);

        // Ignore SSL errors
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--ignore-ssl-errors=yes");
        options.addArguments("--allow-insecure-localhost");
        options.addArguments("--disable-web-security");

        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");

        options.setBinary("/Users/linuselvius/.cache/selenium/chrome/mac-arm64/131.0.6778.264/Google Chrome for Testing.app/Contents/MacOS/Google Chrome for Testing");

        WebDriver driver = null;

        try {
            driver = new ChromeDriver(options);

            proxy.newHar("tracker-scan");

            driver.get(asset.getUrls().getFirst());

            // Wait for page to load
            Thread.sleep(5000);

            // Check traffic
            foundTrackers = proxy.getHar().getLog().getEntries().stream()
                    .flatMap(entry -> trackers.stream()
                            .filter(tracker -> entry.getRequest().getUrl().contains(tracker)))
                    .distinct()
                    .toList();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
            }
            proxy.stop();
        }

        return new ScanReport("Tracker strings matched on network inspection after page load without interaction: " + foundTrackers, asset, foundTrackers != null);
    }
}