package com.leads.leadsgen.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class ScanReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(name = "report", nullable = false, columnDefinition = "TEXT")
    private String report;

    @ManyToOne
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(name = "flagged", nullable = false)
    private boolean flagged;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected ScanReport() {
    }

    public ScanReport(
            String report,
            Asset asset,
            boolean flagged
    ) {
        this.report = report;
        this.asset = asset;
        this.flagged = flagged;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReport() {
        return report;
    }

    public void setReport(String report) {
        this.report = report;
    }

    public Asset getAsset() {
        return asset;
    }

    public void setAsset(Asset asset) {
        this.asset = asset;
    }

    public boolean isFlagged() {
        return flagged;
    }

    public void setFlagged(boolean flagged) {
        this.flagged = flagged;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "ScanReport{" +
                "id=" + id +
                ", report='" + report + '\'' +
                ", asset=" + (asset != null ? asset.getId() : null) +
                ", flagged=" + flagged +
                ", createdAt=" + createdAt +
                '}';
    }
}