package com.remedify.controller;

import com.remedify.model.RepositoryScan;
import com.remedify.repository.RepositoryScanRepository;
import com.remedify.service.ScanOrchestrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/scans")
public class ScanController {

  private final RepositoryScanRepository scanRepository;
  private final ScanOrchestrationService orchestrationService;
  private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

  public ScanController(
      RepositoryScanRepository scanRepository,
      ScanOrchestrationService orchestrationService) {
    this.scanRepository = scanRepository;
    this.orchestrationService = orchestrationService;
  }

  @PostMapping
  public ResponseEntity<RepositoryScan> createScan(
      @RequestBody CreateScanRequest request) {
    log.info("Creating new scan for repository: {}", request.getGitHubUrl());

    // TODO: Validate GitHub URL format
    RepositoryScan scan = new RepositoryScan();
    scan.setGitHubUrl(request.getGitHubUrl());
    scan.setRepositoryName(extractRepoName(request.getGitHubUrl()));
    scan = scanRepository.save(scan);

    // Start async orchestration
    orchestrationService.orchestrateScan(scan.getId());

    return ResponseEntity.status(HttpStatus.CREATED).body(scan);
  }

  @GetMapping
  public ResponseEntity<Page<RepositoryScan>> listScans(Pageable pageable) {
    log.debug("Listing scans");
    Page<RepositoryScan> scans = scanRepository.findByOrderByCreatedAtDesc(pageable);
    return ResponseEntity.ok(scans);
  }

  @GetMapping("/{scanId}")
  public ResponseEntity<RepositoryScan> getScan(@PathVariable UUID scanId) {
    log.debug("Fetching scan: {}", scanId);
    return scanRepository.findById(scanId)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @DeleteMapping("/{scanId}")
  public ResponseEntity<Void> deleteScan(@PathVariable UUID scanId) {
    log.info("Deleting scan: {}", scanId);
    // TODO: Cleanup cloned repository and temp files
    scanRepository.deleteById(scanId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{scanId}/sse")
  public SseEmitter getScanProgress(@PathVariable UUID scanId) {
    log.debug("SSE connection for scan: {}", scanId);
    SseEmitter emitter = new SseEmitter(300000L); // 5 minute timeout
    emitters.put(scanId, emitter);

    emitter.onCompletion(() -> emitters.remove(scanId));
    emitter.onTimeout(() -> emitters.remove(scanId));

    try {
      // Send initial state
      sendUpdate(scanId, "connected", "Connected to scan progress updates");
    } catch (IOException e) {
      log.error("Failed to send initial SSE message", e);
      emitters.remove(scanId);
    }

    return emitter;
  }

  @PostMapping("/{scanId}/retry")
  public ResponseEntity<RepositoryScan> retryScan(@PathVariable UUID scanId) {
    log.info("Retrying scan: {}", scanId);
    return scanRepository.findById(scanId)
        .map(scan -> {
          orchestrationService.orchestrateScan(scanId);
          return ResponseEntity.ok(scan);
        })
        .orElse(ResponseEntity.notFound().build());
  }

  public void sendUpdate(UUID scanId, String stage, String message) throws IOException {
    SseEmitter emitter = emitters.get(scanId);
    if (emitter != null) {
      Map<String, String> data = new HashMap<>();
      data.put("stage", stage);
      data.put("message", message);
      try {
        emitter.send(SseEmitter.event().data(data).build());
      } catch (IOException e) {
        emitters.remove(scanId);
        throw e;
      }
    }
  }

  private String extractRepoName(String gitHubUrl) {
    // Extract repo name from URL like https://github.com/owner/repo
    String[] parts = gitHubUrl.split("/");
    return parts.length > 0 ? parts[parts.length - 1].replace(".git", "") : "unknown";
  }

  public static class CreateScanRequest {
    public String gitHubUrl;

    public CreateScanRequest() {}

    public CreateScanRequest(String gitHubUrl) {
      this.gitHubUrl = gitHubUrl;
    }

    public String getGitHubUrl() {
      return gitHubUrl;
    }

    public void setGitHubUrl(String gitHubUrl) {
      this.gitHubUrl = gitHubUrl;
    }
  }
}
