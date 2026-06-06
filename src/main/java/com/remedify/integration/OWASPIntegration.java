package com.remedify.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.remedify.exception.ScanException;
import com.remedify.model.Severity;
import com.remedify.model.Vulnerability;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class OWASPIntegration {

  private static final int SCAN_TIMEOUT_SECONDS = 600; // 10 minutes for OWASP scan
  private final ObjectMapper objectMapper = new ObjectMapper();

  public List<Vulnerability> scanDependencies(String repositoryPath) throws Exception {
    log.info("Running OWASP Dependency-Check on {}", repositoryPath);

    Path repoPath = Paths.get(repositoryPath);
    if (!Files.exists(repoPath)) {
      throw new ScanException("REPO_NOT_FOUND", "Repository path does not exist: " + repositoryPath);
    }

    // Check if it's a Maven project
    Path pomPath = repoPath.resolve("pom.xml");
    if (!Files.exists(pomPath)) {
      log.warn("pom.xml not found in {}. OWASP scan may not work for non-Maven projects.", repositoryPath);
      return List.of();
    }

    List<Vulnerability> vulnerabilities = new ArrayList<>();

    try {
      // Run OWASP Dependency-Check Maven plugin
      log.info("Executing OWASP Dependency-Check Maven plugin for {}", repositoryPath);
      String reportPath = runOWASPScan(repositoryPath);

      // Parse the generated report
      log.debug("Parsing OWASP report from: {}", reportPath);
      vulnerabilities = parseOWASPReport(reportPath);

      log.info("OWASP scan completed. Found {} vulnerabilities", vulnerabilities.size());
    } catch (Exception e) {
      log.error("OWASP Dependency-Check scan failed: {}", e.getMessage(), e);
      throw new ScanException("OWASP_SCAN_FAILED", "OWASP Dependency-Check failed: " + e.getMessage(), e);
    }

    return vulnerabilities;
  }

  /**
   * Execute OWASP Dependency-Check Maven plugin
   */
  private String runOWASPScan(String repositoryPath) throws Exception {
    String reportDir = repositoryPath + "/target/dependency-check";
    String reportPath = reportDir + "/dependency-check-report.json";

    // Create report directory
    Files.createDirectories(Paths.get(reportDir));

    // Command: mvn org.owasp:dependency-check-maven:check -DreportFormat=JSON
    ProcessBuilder pb = new ProcessBuilder(
        "mvn",
        "org.owasp:dependency-check-maven:check",
        "-DreportFormat=JSON",
        "-DreportOutputDirectory=" + reportDir,
        "-DskipProvidedScope=true",
        "-DskipRuntimeScope=false"
    );

    pb.directory(new File(repositoryPath));
    pb.redirectErrorStream(true);

    Process process = null;
    try {
      log.debug("Starting OWASP Maven plugin in: {}", repositoryPath);
      process = pb.start();

      // Log output
      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      String line;
      while ((line = reader.readLine()) != null) {
        log.debug("OWASP output: {}", line);
      }

      // Wait for completion with timeout
      boolean completed = process.waitFor(SCAN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
      if (!completed) {
        process.destroyForcibly();
        throw new Exception("OWASP scan timed out after " + SCAN_TIMEOUT_SECONDS + " seconds");
      }

      int exitCode = process.exitValue();
      // OWASP returns non-zero if vulnerabilities are found, but we still want to process them
      log.debug("OWASP scan exit code: {} (0=no vulns, >0=vulns found)", exitCode);

      // Check if report was generated
      if (!Files.exists(Paths.get(reportPath))) {
        log.warn("OWASP report not found at: {}", reportPath);
        return reportPath; // Return path anyway, parsing will handle missing file
      }

      log.info("OWASP scan completed successfully");
      return reportPath;

    } finally {
      if (process != null) {
        process.destroy();
      }
    }
  }

  /**
   * Parse OWASP Dependency-Check JSON report
   */
  private List<Vulnerability> parseOWASPReport(String reportPath) throws Exception {
    List<Vulnerability> vulnerabilities = new ArrayList<>();

    Path path = Paths.get(reportPath);
    if (!Files.exists(path)) {
      log.warn("OWASP report file not found: {}", reportPath);
      return vulnerabilities;
    }

    try {
      String reportContent = Files.readString(path);
      JsonNode root = objectMapper.readTree(reportContent);

      // Navigate to dependencies array
      JsonNode dependencies = root.get("dependencies");
      if (dependencies == null || !dependencies.isArray()) {
        log.debug("No dependencies found in OWASP report");
        return vulnerabilities;
      }

      // Process each dependency
      for (JsonNode dep : dependencies) {
        String fileName = dep.get("fileName").asText();
        JsonNode vulnerabilitiesNode = dep.get("vulnerabilities");

        if (vulnerabilitiesNode != null && vulnerabilitiesNode.isArray()) {
          for (JsonNode vuln : vulnerabilitiesNode) {
            Vulnerability vulnerability = parseVulnerability(vuln, fileName);
            vulnerabilities.add(vulnerability);
            log.debug("Parsed vulnerability: {} from {}", vulnerability.getType(), fileName);
          }
        }
      }

      log.info("Parsed {} vulnerabilities from OWASP report", vulnerabilities.size());
    } catch (Exception e) {
      log.error("Error parsing OWASP report: {}", e.getMessage(), e);
      throw e;
    }

    return vulnerabilities;
  }

  /**
   * Convert OWASP vulnerability JSON to Vulnerability object
   */
  private Vulnerability parseVulnerability(JsonNode vulnNode, String fileName) {
    Vulnerability vuln = new Vulnerability();

    vuln.setFilePath(fileName);
    vuln.setType(vulnNode.get("name").asText("Unknown"));
    vuln.setDescription(vulnNode.get("description").asText(""));
    vuln.setSource("OWASP");

    // Try to get CVE ID
    JsonNode cveNode = vulnNode.get("cve");
    if (cveNode != null) {
      vuln.setCveId(cveNode.asText());
    }

    // Map CVSS score to severity
    Severity severity = mapCVSSToSeverity(vulnNode);
    vuln.setSeverity(severity);

    return vuln;
  }

  /**
   * Map CVSS score to severity level
   * CVSS 3.1 Scale: 0.0 = None, 0.1-3.9 = Low, 4.0-6.9 = Medium, 7.0-8.9 = High, 9.0-10.0 = Critical
   */
  private Severity mapCVSSToSeverity(JsonNode vulnNode) {
    // Try to get CVSS score
    JsonNode cvssNode = vulnNode.get("cvssv3");
    if (cvssNode == null) {
      cvssNode = vulnNode.get("cvssv2");
    }

    float score = 0f;
    if (cvssNode != null) {
      JsonNode scoreNode = cvssNode.get("baseScore");
      if (scoreNode != null) {
        score = scoreNode.floatValue();
      }
    }

    // Map score to severity
    if (score >= 9.0) {
      return Severity.CRITICAL;
    } else if (score >= 7.0) {
      return Severity.HIGH;
    } else if (score >= 4.0) {
      return Severity.MEDIUM;
    } else {
      return Severity.LOW;
    }
  }
}
