package com.leads.leadsgen.repository;

import com.leads.leadsgen.model.ScanReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScanReportRepository extends JpaRepository<ScanReport, Long> {

    @Query("SELECT sr FROM ScanReport sr WHERE sr.asset.domain = :domain")
    Optional<ScanReport> findByAssetDomain(@Param("domain") String domain);

}
