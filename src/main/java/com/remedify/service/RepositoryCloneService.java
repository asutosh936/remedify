package com.remedify.service;

import com.remedify.exception.ScanException;
import com.remedify.integration.GitHubIntegration;
import com.remedify.model.RepositoryScan;
import com.remedify.repository.RepositoryScanRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
public class RepositoryCloneService {

  private final GitHubIntegration gitHubIntegration;
  private final RepositoryScanRepository scanRepository;

  @Value("${remedify.scanning.temp-dir:/tmp/remedify-scans}")
  private String tempDir;

  @Value("${remedify.scanning.clone-timeout-seconds:300}")
  private int cloneTimeoutSeconds;

  public RepositoryCloneService(GitHubIntegration gitHubIntegration,
      RepositoryScanRepository scanRepository) {
    this.gitHubIntegration = gitHubIntegration;
    this.scanRepository = scanRepository;
  }

  public void cloneScan(RepositoryScan scan) throws Exception {
    log.info("Starting repository clone for scan: {} from URL: {}", scan.getId(), scan.getGitHubUrl());

    String clonePath = null;
    try {
      // Step 1: Validate GitHub URL
      log.debug("Validating GitHub URL: {}", scan.getGitHubUrl());
      gitHubIntegration.validateGitHubUrl(scan.getGitHubUrl());

      // Step 2: Generate unique clone path
      clonePath = generateClonePath(scan.getId());
      log.debug("Generated clone path: {}", clonePath);

      // Step 3: Clone repository
      log.info("Cloning repository for scan: {} to path: {}", scan.getId(), clonePath);
      gitHubIntegration.cloneRepository(scan.getGitHubUrl(), clonePath);

      // Step 4: Extract project metadata
      log.debug("Extracting project metadata for scan: {}", scan.getId());
      GitHubIntegration.ProjectMetadata metadata = gitHubIntegration
          .extractRepositoryMetadata(clonePath);
      log.info("Detected project metadata: {} for scan: {}", metadata, scan.getId());

      // Step 5: Verify it's a supported project type
      if ("unknown".equals(metadata.projectType)) {
        log.warn("Unknown project type for scan: {}. Metadata: {}", scan.getId(), metadata);
        throw new ScanException("UNSUPPORTED_PROJECT",
            "Project type not recognized. Supported: Java (Maven/Gradle), Node.js, Python, Go");
      }

      // Step 6: Update scan with cloned path
      scan.setClonedPath(clonePath);
      scan.setStatusMessage(
          "Repository cloned successfully. Detected: " + metadata.language + " (" + metadata.buildTool + ")");
      scanRepository.save(scan);

      log.info("Successfully cloned repository for scan: {}. Project type: {}", scan.getId(),
          metadata.projectType);

    } catch (ScanException e) {
      log.error("Scan exception during clone for scan {}: {}", scan.getId(), e.getMessage());
      cleanupClonedDirectory(clonePath);
      throw e;
    } catch (Exception e) {
      log.error("Unexpected error during repository clone for scan {}: {}", scan.getId(), e.getMessage(), e);
      cleanupClonedDirectory(clonePath);
      throw new ScanException("CLONE_FAILED",
          "Failed to clone repository: " + e.getMessage(), e);
    }
  }

  /**
   * Generate a unique path for cloning the repository
   */
  private String generateClonePath(java.util.UUID scanId) {
    // Create directory structure: /tmp/remedify-scans/550e8400-e29b-41d4-a716-446655440000
    return Paths.get(tempDir, scanId.toString()).toString();
  }

  /**
   * Clean up cloned directory on error
   */
  private void cleanupClonedDirectory(String clonePath) {
    if (clonePath == null) {
      return;
    }

    try {
      Path path = Paths.get(clonePath);
      if (Files.exists(path)) {
        log.debug("Cleaning up cloned directory: {}", clonePath);
        Files.walk(path)
            .sorted(java.util.Comparator.reverseOrder())
            .forEach(p -> {
              try {
                Files.delete(p);
                log.debug("Deleted: {}", p);
              } catch (Exception e) {
                log.warn("Failed to delete: {}", p, e);
              }
            });
        log.info("Cleaned up cloned directory: {}", clonePath);
      }
    } catch (Exception e) {
      log.error("Error during cleanup of cloned directory: {}", clonePath, e);
    }
  }
}
