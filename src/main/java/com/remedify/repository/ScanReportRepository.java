package com.remedify.repository;

import com.remedify.model.ScanReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScanReportRepository extends JpaRepository<ScanReport, UUID> {
  Optional<ScanReport> findByScanId(UUID scanId);
}
