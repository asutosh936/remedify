package com.remedify.service;

import com.remedify.model.RepositoryScan;
import com.remedify.model.Severity;
import com.remedify.repository.RepositoryScanRepository;
import com.remedify.repository.VulnerabilityRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Arrays;

@Slf4j
@Service
public class AIRecommendationService {

  private final VulnerabilityRepository vulnerabilityRepository;
  private final RepositoryScanRepository scanRepository;

  public AIRecommendationService(
      VulnerabilityRepository vulnerabilityRepository,
      RepositoryScanRepository scanRepository) {
    this.vulnerabilityRepository = vulnerabilityRepository;
    this.scanRepository = scanRepository;
  }

  public void generateRecommendations(RepositoryScan scan) throws Exception {
    log.info("Generating AI recommendations for scan: {}", scan.getId());

    // Query only high-severity vulnerabilities
    var highSeverityVulns = vulnerabilityRepository.findByScanIdAndSeverityIn(
        scan.getId(),
        Arrays.asList(Severity.CRITICAL, Severity.HIGH));

    if (highSeverityVulns.isEmpty()) {
      log.info("No high-severity vulnerabilities found. Skipping AI recommendations.");
      return;
    }

    log.info("Processing {} high-severity vulnerabilities", highSeverityVulns.size());
    // TODO: Implement Claude API integration
    // 1. Group vulnerabilities by type
    // 2. For each group, build a prompt with multiple similar issues
    // 3. Call Claude API via Spring AI
    // 4. Store recommendations in AIRecommendation table
    // 5. Use prompt caching to reduce token usage
    log.debug("AI recommendations generated for scan: {}", scan.getId());
  }
}
