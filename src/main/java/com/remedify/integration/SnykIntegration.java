package com.remedify.integration;

import com.remedify.model.Vulnerability;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(name = "remedify.snyk.enabled", havingValue = "true")
public class SnykIntegration {

  @Value("${remedify.snyk.api-key:}")
  private String apiKey;

  @Value("${remedify.snyk.base-url}")
  private String baseUrl;

  public List<Vulnerability> scanRepository(String gitHubUrl) throws Exception {
    log.info("Scanning repository with Snyk: {}", gitHubUrl);
    // TODO: Implement Snyk API integration
    // 1. Call Snyk API with repository URL
    // 2. Parse vulnerability findings
    // 3. Convert to Vulnerability objects
    return List.of();
  }

  public boolean isEnabled() {
    return apiKey != null && !apiKey.isEmpty();
  }
}
