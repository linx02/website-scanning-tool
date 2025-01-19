package com.leads.leadsgen.controller;

import com.leads.leadsgen.model.EmailTemplate;
import com.leads.leadsgen.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/email")
    public ResponseEntity<Map<String, String>> sendEmails(@RequestBody Map<String, List<Map<String, String>>> body) {

        List<Map<String, String>> emails = body.get("emails");

        emailService.sendEmails(emails);

        return ResponseEntity.ok(Map.of("message", "Emails sent successfully"));
    }

}
