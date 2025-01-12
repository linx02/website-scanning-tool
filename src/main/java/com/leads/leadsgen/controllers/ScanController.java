package com.leads.leadsgen.controllers;

import com.leads.leadsgen.services.ScanService;
import com.leads.leadsgen.services.SseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ScanController {

    private final ScanService scanService;
    private final SseService sseService;

    public ScanController(ScanService scanService, SseService sseService) {
        this.scanService = scanService;
        this.sseService = sseService;
    }

    @PostMapping("/scan")
    public ResponseEntity<String> scan(@RequestBody Map<String, List<String>> body) {
        List<String> domains = body.get("domains");
        List<String> scanners = body.get("scanners");
        new Thread(() -> scanService.scanDomains(domains, scanners)).start();
        return ResponseEntity.ok("Scan initialized for domains: " + domains);
    }

    @GetMapping("/stream-scan-status")
    public SseEmitter streamStatus() {
        return sseService.registerClient();
    }
}