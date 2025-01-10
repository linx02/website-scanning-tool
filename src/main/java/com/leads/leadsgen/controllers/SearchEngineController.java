package com.leads.leadsgen.controllers;

import com.leads.leadsgen.services.SearchEngineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SearchEngineController {

    private SearchEngineService searchEngineService;

    public SearchEngineController(SearchEngineService searchEngineService) {
        this.searchEngineService = searchEngineService;
    }

    @RequestMapping("/search")
    public ResponseEntity<List<String>> search(@RequestBody Map<String, ?> body) {
        String query = (String) body.get("query");
        int limit = (int) body.get("limit");
        return ResponseEntity.ok(searchEngineService.search(query, limit));
    }
}
