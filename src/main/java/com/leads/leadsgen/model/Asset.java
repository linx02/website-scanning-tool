package com.leads.leadsgen.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "domain", nullable = false, unique = true)
    private String domain;

    @Column(name = "urls", columnDefinition = "TEXT")
    private String urls;

    @Column(name = "emails", columnDefinition = "TEXT")
    private String emails;

    @Column(name = "phones", columnDefinition = "TEXT")
    private String phones;

    @Column(name = "scanned_by", columnDefinition = "TEXT")
    private String scannedByJson;

    @Column(name = "emailed")
    private boolean emailed;

    @Transient
    private List<Map<String, String>> scannedBy;

    @Transient
    private Map<String, String> htmlContents;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Asset() {
    }

    public Asset(
            String domain,
            List<String> urls,
            List<String> emails,
            List<String> phones,
            List<Map<String, String>> scannedBy,
            Map<String, String> htmlContents
    ) {
        this.domain = domain;
        this.urls = String.join(",", urls);
        this.emails = String.join(",", emails);
        this.phones = String.join(",", phones);
        this.scannedBy = scannedBy;
        this.htmlContents = htmlContents;
        this.emailed = false;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and setters for persisted fields

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public List<String> getUrls() {
        if (urls != null) {
            return List.of(urls.split(","));
        }
        return List.of();
    }

    public void setUrls(List<String> sitemapUrls) {
        this.urls = String.join(",", sitemapUrls);
    }

    public List<String> getEmails() {
        if (emails != null) {
            return List.of(emails.split(","));
        }
        return List.of();
    }

    public void setEmails(List<String> emails) {
        this.emails = String.join(",", emails);
    }

    public List<String> getPhones() {
        if (phones != null) {
            return List.of(phones.split(","));
        }
        return List.of();
    }

    public void setPhones(List<String> phones) {
        this.phones = String.join(",", phones);
    }

    public List<Map<String, String>> getScannedBy() {
        if (scannedBy == null && scannedByJson != null) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                scannedBy = mapper.readValue(scannedByJson, new TypeReference<List<Map<String, String>>>() {});
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                scannedBy = List.of();
            }
        }
        return scannedBy;
    }

    public void setScannedBy(List<Map<String, String>> scannedBy) {
        this.scannedBy = scannedBy;
        ObjectMapper mapper = new ObjectMapper();
        try {
            this.scannedByJson = mapper.writeValueAsString(scannedBy);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public Map<String, String> getHtmlContents() {
        return htmlContents;
    }

    public void setHtmlContents(Map<String, String> htmlContents) {
        this.htmlContents = htmlContents;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "Asset{" +
                "id=" + id +
                ", domain='" + domain + '\'' +
                ", urls=" + getUrls() +
                ", emails=" + getEmails() +
                ", phones=" + getPhones() +
                ", scannedBy=" + getScannedBy() +
                ", htmlContents=" + htmlContents +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    public boolean isEmailed() {
        return emailed;
    }

    public void setEmailed(boolean emailed) {
        this.emailed = emailed;
    }
}