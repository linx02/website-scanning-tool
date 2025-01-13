package com.leads.leadsgen.controller;

import com.leads.leadsgen.service.CrawlService;
import com.leads.leadsgen.service.SseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CrawlController {

    private final CrawlService crawlService;
    private final SseService sseService;

    public CrawlController(CrawlService crawlService, SseService sseService) {
        this.crawlService = crawlService;
        this.sseService = sseService;
    }

    @PostMapping("/crawl")
    public ResponseEntity<String> crawl(@RequestBody Map<String, List<String>> body) {
        List<String> domains = body.get("domains");
        new Thread(() -> crawlService.crawlDomains(domains)).start();
        return ResponseEntity.ok("Crawl initialized for domains: " + domains);
    }

    @GetMapping("/stream-crawl-status")
    public SseEmitter streamStatus() {
        return sseService.registerClient();
    }
}