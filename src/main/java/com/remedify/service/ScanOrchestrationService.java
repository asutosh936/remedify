package com.remedify.service;

import com.remedify.exception.ScanException;
import com.remedify.model.RepositoryScan;
import com.remedify.model.ScanStage;
import com.remedify.repository.RepositoryScanRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
public class ScanOrchestrationService {

  private final RepositoryScanRepository repositoryScanRepository;
  private final RepositoryCloneService cloneService;
  private final VulnerabilityDetectionService detectionService;
  private final AIRecommendationService aiService;
  private final BuildValidationService buildService;
  private final ReportGenerationService reportService;

  public ScanOrchestrationService(
      RepositoryScanRepository repositoryScanRepository,
      RepositoryCloneService cloneService,
      VulnerabilityDetectionService detectionService,
      AIRecommendationService aiService,
      BuildValidationService buildService,
      ReportGenerationService reportService) {
    this.repositoryScanRepository = repositoryScanRepository;
    this.cloneService = cloneService;
    this.detectionService = detectionService;
    this.aiService = aiService;
    this.buildService = buildService;
    this.reportService = reportService;
  }

  @Async
  @Transactional
  public void orchestrateScan(UUID scanId) {
    try {
      RepositoryScan scan = repositoryScanRepository.findById(scanId)
          .orElseThrow(() -> new ScanException("SCAN_NOT_FOUND", "Scan not found: " + scanId));

      executeStage(scan, ScanStage.CLONING, cloneService::cloneScan);
      executeStage(scan, ScanStage.SCANNING, detectionService::detectVulnerabilities);
      executeStage(scan, ScanStage.RECOMMENDING, aiService::generateRecommendations);
      executeStage(scan, ScanStage.VALIDATING, buildService::validateBuild);
      executeStage(scan, ScanStage.REPORTING, reportService::generateReport);

      scan.setCurrentStage(ScanStage.COMPLETED);
      scan.setStatusMessage("Scan completed successfully");
      repositoryScanRepository.save(scan);
      log.info("Scan completed: {}", scanId);
    } catch (Exception e) {
      log.error("Scan orchestration failed: {}", scanId, e);
      updateScanWithError(scanId, e);
    }
  }

  private void executeStage(RepositoryScan scan, ScanStage stage, StageExecutor executor) {
    try {
      scan.setCurrentStage(stage);
      scan.setStatusMessage("Running " + stage.getDisplayName());
      repositoryScanRepository.save(scan);
      log.info("Starting stage: {} for scan: {}", stage, scan.getId());

      executor.execute(scan);

      log.info("Completed stage: {} for scan: {}", stage, scan.getId());
    } catch (Exception e) {
      log.error("Stage failed: {} for scan: {}", stage, scan.getId(), e);
      throw new ScanException("STAGE_FAILED", "Stage " + stage + " failed: " + e.getMessage(), e);
    }
  }

  private void updateScanWithError(UUID scanId, Exception e) {
    try {
      RepositoryScan scan = repositoryScanRepository.findById(scanId).orElse(null);
      if (scan != null) {
        scan.setStatusMessage("Error: " + e.getMessage());
        scan.setRetryCount(scan.getRetryCount() + 1);
        repositoryScanRepository.save(scan);
      }
    } catch (Exception ex) {
      log.error("Failed to update scan error status: {}", scanId, ex);
    }
  }

  @FunctionalInterface
  private interface StageExecutor {
    void execute(RepositoryScan scan) throws Exception;
  }
}
