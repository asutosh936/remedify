package com.remedify.service;

import com.remedify.exception.ScanException;
import com.remedify.integration.ClaudeAIIntegration;
import com.remedify.model.AIRecommendation;
import com.remedify.model.RepositoryScan;
import com.remedify.model.Severity;
import com.remedify.model.Vulnerability;
import com.remedify.repository.AIRecommendationRepository;
import com.remedify.repository.RepositoryScanRepository;
import com.remedify.repository.VulnerabilityRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@ConditionalOnProperty(name = "spring.ai.anthropic.api-key")
public class AIRecommendationService {

  private final VulnerabilityRepository vulnerabilityRepository;
  private final AIRecommendationRepository aiRecommendationRepository;
  private final RepositoryScanRepository scanRepository;
  private final ClaudeAIIntegration claudeAIIntegration;

  public AIRecommendationService(
      VulnerabilityRepository vulnerabilityRepository,
      AIRecommendationRepository aiRecommendationRepository,
      RepositoryScanRepository scanRepository,
      ClaudeAIIntegration claudeAIIntegration) {
    this.vulnerabilityRepository = vulnerabilityRepository;
    this.aiRecommendationRepository = aiRecommendationRepository;
    this.scanRepository = scanRepository;
    this.claudeAIIntegration = claudeAIIntegration;
  }

  public void generateRecommendations(RepositoryScan scan) throws Exception {
    log.info("Starting AI recommendation generation for scan: {}", scan.getId());

    // Step 1: Query only HIGH and CRITICAL severity vulnerabilities (token efficiency)
    List<Vulnerability> highSeverityVulns = vulnerabilityRepository.findByScanIdAndSeverityIn(
        scan.getId(),
        Arrays.asList(Severity.CRITICAL, Severity.HIGH));

    log.info("Found {} high-severity vulnerabilities for scan: {}", highSeverityVulns.size(), scan.getId());

    if (highSeverityVulns.isEmpty()) {
      log.info("No high-severity vulnerabilities found for scan: {}. Skipping AI recommendations.",
          scan.getId());
      scan.setStatusMessage("No high-severity vulnerabilities to recommend fixes for");
      scanRepository.save(scan);
      return;
    }

    // Validate Claude API is configured when high-severity vulns are found
    if (claudeAIIntegration == null) {
      log.warn("Claude API not configured (ANTHROPIC_API_KEY not set). Cannot generate AI recommendations for high-severity vulnerabilities.");
      scan.setStatusMessage("High-severity vulnerabilities found, but Claude API is not configured. Set ANTHROPIC_API_KEY to enable recommendations.");
      scanRepository.save(scan);
      return;
    }

    try {
      // Step 2: Group vulnerabilities by type for batch processing
      // This reduces API calls and improves token efficiency
      Map<String, List<Vulnerability>> vulnsByType = highSeverityVulns.stream()
          .collect(Collectors.groupingBy(Vulnerability::getType));

      log.debug("Grouped {} vulnerabilities into {} types", highSeverityVulns.size(), vulnsByType.size());

      // Step 3: Generate recommendations for each vulnerability type
      List<AIRecommendation> allRecommendations = new ArrayList<>();
      int processedTypes = 0;

      for (Map.Entry<String, List<Vulnerability>> entry : vulnsByType.entrySet()) {
        String vulnerabilityType = entry.getKey();
        List<Vulnerability> typeVulns = entry.getValue();

        try {
          log.info("Generating AI recommendation for vulnerability type: {} ({} instances)",
              vulnerabilityType, typeVulns.size());

          String recommendation = claudeAIIntegration.generateRecommendation(
              vulnerabilityType,
              typeVulns
          );

          // Step 4: Create AIRecommendation records for each vulnerability
          for (Vulnerability vuln : typeVulns) {
            AIRecommendation aiRec = new AIRecommendation();
            aiRec.setVulnerability(vuln);
            aiRec.setSuggestion(recommendation);
            aiRec.setEstimatedEffort(estimateEffort(vulnerabilityType));
            aiRec.setAppliedManually(false);

            AIRecommendation saved = aiRecommendationRepository.save(aiRec);
            allRecommendations.add(saved);
            log.debug("Saved AI recommendation for vulnerability: {}", vuln.getId());
          }

          processedTypes++;

        } catch (Exception e) {
          log.error("Failed to generate recommendation for type {}: {}",
              vulnerabilityType, e.getMessage());
          // Continue with other types, don't fail the entire scan
          scan.setStatusMessage(
              "Partial AI recommendations generated. Failed for: " + vulnerabilityType);
        }
      }

      // Step 5: Update scan status
      String statusMsg = String.format(
          "AI recommendations generated for %d vulnerability types (%d total recommendations)",
          processedTypes, allRecommendations.size());
      scan.setStatusMessage(statusMsg);
      scanRepository.save(scan);

      log.info("AI recommendation generation completed for scan: {}. Processed {} types, {} total recommendations",
          scan.getId(), processedTypes, allRecommendations.size());

    } catch (Exception e) {
      log.error("Unexpected error during AI recommendation generation for scan {}: {}",
          scan.getId(), e.getMessage(), e);
      throw new ScanException("AI_GENERATION_FAILED",
          "Failed to generate AI recommendations: " + e.getMessage(), e);
    }
  }

  /**
   * Estimate effort required to fix a vulnerability type
   */
  private String estimateEffort(String vulnerabilityType) {
    String typeLower = vulnerabilityType.toLowerCase();

    // Quick heuristics for effort estimation
    if (typeLower.contains("dependency") || typeLower.contains("version")) {
      return "Low"; // Usually just a version bump
    } else if (typeLower.contains("injection") || typeLower.contains("rce")) {
      return "Medium"; // Requires code changes
    } else if (typeLower.contains("authentication") || typeLower.contains("encryption")) {
      return "High"; // Complex security changes
    } else {
      return "Medium"; // Default
    }
  }
}
