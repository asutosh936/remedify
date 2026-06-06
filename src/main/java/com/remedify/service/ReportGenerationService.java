package com.remedify.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.remedify.exception.ScanException;
import com.remedify.model.RepositoryScan;
import com.remedify.model.ScanReport;
import com.remedify.model.Severity;
import com.remedify.model.TestResult;
import com.remedify.model.Vulnerability;
import com.remedify.repository.RepositoryScanRepository;
import com.remedify.repository.ScanReportRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReportGenerationService {

  private final RepositoryScanRepository scanRepository;
  private final ScanReportRepository reportRepository;
  private final ObjectMapper objectMapper;

  public ReportGenerationService(
      RepositoryScanRepository scanRepository,
      ScanReportRepository reportRepository,
      ObjectMapper objectMapper) {
    this.scanRepository = scanRepository;
    this.reportRepository = reportRepository;
    this.objectMapper = objectMapper;
  }

  public void generateReport(RepositoryScan scan) throws Exception {
    log.info("Starting report generation for scan: {}", scan.getId());

    try {
      // Refresh scan to get all related data
      final RepositoryScan finalScan = scanRepository.findById(scan.getId())
          .orElseThrow(() -> new ScanException("SCAN_NOT_FOUND", "Scan not found: " + scan.getId()));

      // Generate both HTML and JSON reports
      log.debug("Generating HTML report for scan: {}", finalScan.getId());
      String htmlReport = generateHTMLReport(finalScan);

      log.debug("Generating JSON report for scan: {}", finalScan.getId());
      String jsonReport = generateJSONReport(finalScan);

      // Save report to database
      ScanReport report = new ScanReport();
      report.setScan(finalScan);
      report.setHtmlReport(htmlReport);
      report.setJsonReport(jsonReport);
      reportRepository.save(report);

      // Update scan status
      finalScan.setStatusMessage("Report generation completed");
      scanRepository.save(finalScan);

      log.info("Report generation completed for scan: {}. HTML: {} bytes, JSON: {} bytes",
          finalScan.getId(), htmlReport.length(), jsonReport.length());

    } catch (Exception e) {
      log.error("Report generation failed for scan {}: {}", scan.getId(), e.getMessage(), e);
      throw new ScanException("REPORT_GENERATION_FAILED",
          "Failed to generate report: " + e.getMessage(), e);
    }
  }

  /**
   * Generate HTML report
   */
  private String generateHTMLReport(RepositoryScan scan) {
    StringBuilder html = new StringBuilder();

    html.append("<!DOCTYPE html>\n");
    html.append("<html lang=\"en\">\n");
    html.append("<head>\n");
    html.append("  <meta charset=\"UTF-8\">\n");
    html.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
    html.append("  <title>Remedify Scan Report - ").append(scan.getRepositoryName()).append("</title>\n");
    html.append("  <style>\n");
    html.append(getHTMLStyles());
    html.append("  </style>\n");
    html.append("</head>\n");
    html.append("<body>\n");

    // Header
    html.append("<div class=\"container\">\n");
    html.append("  <header>\n");
    html.append("    <h1>Remedify Security Scan Report</h1>\n");
    html.append("    <p class=\"scan-id\">Scan ID: ").append(scan.getId()).append("</p>\n");
    html.append("  </header>\n");

    // Executive Summary
    html.append("  <section class=\"summary\">\n");
    html.append("    <h2>Executive Summary</h2>\n");
    html.append("    <div class=\"summary-grid\">\n");
    html.append("      <div class=\"summary-item\">\n");
    html.append("        <span class=\"label\">Repository</span>\n");
    html.append("        <span class=\"value\">").append(scan.getRepositoryName()).append("</span>\n");
    html.append("      </div>\n");
    html.append("      <div class=\"summary-item\">\n");
    html.append("        <span class=\"label\">URL</span>\n");
    html.append("        <span class=\"value\">").append(scan.getGitHubUrl()).append("</span>\n");
    html.append("      </div>\n");
    html.append("      <div class=\"summary-item\">\n");
    html.append("        <span class=\"label\">Scan Date</span>\n");
    html.append("        <span class=\"value\">").append(formatDate(scan.getCreatedAt())).append("</span>\n");
    html.append("      </div>\n");
    html.append("    </div>\n");
    html.append("  </section>\n");

    // Vulnerability Summary
    html.append(getVulnerabilitySection(scan));

    // Test Results
    html.append(getTestResultsSection(scan));

    // Detailed Findings
    html.append(getDetailedFindingsSection(scan));

    // Footer
    html.append("  <footer>\n");
    html.append("    <p>Generated by Remedify</p>\n");
    html.append("  </footer>\n");
    html.append("</div>\n");
    html.append("</body>\n");
    html.append("</html>\n");

    return html.toString();
  }

  /**
   * Generate JSON report
   */
  private String generateJSONReport(RepositoryScan scan) throws Exception {
    ObjectNode root = objectMapper.createObjectNode();

    root.put("scanId", scan.getId().toString());
    root.put("repositoryName", scan.getRepositoryName());
    root.put("repositoryUrl", scan.getGitHubUrl());
    root.put("scanDate", scan.getCreatedAt().toString());

    // Summary
    ObjectNode summary = root.putObject("summary");
    summary.put("totalVulnerabilities", scan.getVulnerabilities().size());
    summary.put("criticalCount", countBySeverity(scan, Severity.CRITICAL));
    summary.put("highCount", countBySeverity(scan, Severity.HIGH));
    summary.put("mediumCount", countBySeverity(scan, Severity.MEDIUM));
    summary.put("lowCount", countBySeverity(scan, Severity.LOW));

    // Test Results
    if (scan.getTestResult() != null) {
      ObjectNode testResults = root.putObject("testResults");
      testResults.put("buildSuccess", scan.getTestResult().getBuildSuccess());
      testResults.put("testsPassed", scan.getTestResult().getTestsPassed());
      testResults.put("testsFailed", scan.getTestResult().getTestsFailed());
      testResults.put("passRate", scan.getTestResult().getPassRate());
    }

    // Vulnerabilities
    ArrayNode vulns = root.putArray("vulnerabilities");
    for (Vulnerability vuln : scan.getVulnerabilities()) {
      ObjectNode vulnObj = vulns.addObject();
      vulnObj.put("id", vuln.getId().toString());
      vulnObj.put("type", vuln.getType());
      vulnObj.put("severity", vuln.getSeverity().toString());
      vulnObj.put("filePath", vuln.getFilePath());
      vulnObj.put("description", vuln.getDescription());
      vulnObj.put("cveId", vuln.getCveId());
      vulnObj.put("source", vuln.getSource());

      if (vuln.getAiRecommendation() != null) {
        ObjectNode rec = vulnObj.putObject("recommendation");
        rec.put("suggestion", vuln.getAiRecommendation().getSuggestion());
        rec.put("estimatedEffort", vuln.getAiRecommendation().getEstimatedEffort());
      }
    }

    return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
  }

  /**
   * Count vulnerabilities by severity
   */
  private int countBySeverity(RepositoryScan scan, Severity severity) {
    return (int) scan.getVulnerabilities().stream()
        .filter(v -> v.getSeverity() == severity)
        .count();
  }

  /**
   * Generate vulnerability section HTML
   */
  private String getVulnerabilitySection(RepositoryScan scan) {
    StringBuilder html = new StringBuilder();
    html.append("  <section class=\"vulnerabilities\">\n");
    html.append("    <h2>Vulnerability Summary</h2>\n");
    html.append("    <div class=\"stats-grid\">\n");

    Map<Severity, Long> vulnBySeverity = scan.getVulnerabilities().stream()
        .collect(Collectors.groupingBy(Vulnerability::getSeverity, Collectors.counting()));

    for (Severity severity : Severity.values()) {
      long count = vulnBySeverity.getOrDefault(severity, 0L);
      String colorClass = severity == Severity.CRITICAL ? "critical" :
          severity == Severity.HIGH ? "high" :
          severity == Severity.MEDIUM ? "medium" : "low";
      html.append("      <div class=\"stat ").append(colorClass).append("\">\n");
      html.append("        <span class=\"severity\">").append(severity).append("</span>\n");
      html.append("        <span class=\"count\">").append(count).append("</span>\n");
      html.append("      </div>\n");
    }

    html.append("    </div>\n");
    html.append("  </section>\n");
    return html.toString();
  }

  /**
   * Generate test results section HTML
   */
  private String getTestResultsSection(RepositoryScan scan) {
    StringBuilder html = new StringBuilder();
    TestResult testResult = scan.getTestResult();

    html.append("  <section class=\"tests\">\n");
    html.append("    <h2>Build & Test Results</h2>\n");

    if (testResult != null) {
      String buildStatus = testResult.getBuildSuccess() ? "✓ Success" : "✗ Failed";
      String buildClass = testResult.getBuildSuccess() ? "success" : "failed";
      html.append("    <div class=\"test-item\">\n");
      html.append("      <span class=\"label\">Build Status:</span>\n");
      html.append("      <span class=\"value ").append(buildClass).append("\">").append(buildStatus).append("</span>\n");
      html.append("    </div>\n");

      html.append("    <div class=\"test-item\">\n");
      html.append("      <span class=\"label\">Tests Passed:</span>\n");
      html.append("      <span class=\"value\">").append(testResult.getTestsPassed()).append("</span>\n");
      html.append("    </div>\n");

      html.append("    <div class=\"test-item\">\n");
      html.append("      <span class=\"label\">Tests Failed:</span>\n");
      html.append("      <span class=\"value\">").append(testResult.getTestsFailed()).append("</span>\n");
      html.append("    </div>\n");

      html.append("    <div class=\"test-item\">\n");
      html.append("      <span class=\"label\">Pass Rate:</span>\n");
      html.append("      <span class=\"value\">").append(String.format("%.1f%%", testResult.getPassRate())).append("</span>\n");
      html.append("    </div>\n");
    } else {
      html.append("    <p>No test results available</p>\n");
    }

    html.append("  </section>\n");
    return html.toString();
  }

  /**
   * Generate detailed findings section HTML
   */
  private String getDetailedFindingsSection(RepositoryScan scan) {
    StringBuilder html = new StringBuilder();
    html.append("  <section class=\"findings\">\n");
    html.append("    <h2>Detailed Findings</h2>\n");

    if (scan.getVulnerabilities().isEmpty()) {
      html.append("    <p>No vulnerabilities found</p>\n");
    } else {
      for (Vulnerability vuln : scan.getVulnerabilities()) {
        String severityClass = vuln.getSeverity().toString().toLowerCase();
        html.append("    <div class=\"finding ").append(severityClass).append("\">\n");
        html.append("      <h3>").append(vuln.getType()).append("</h3>\n");
        html.append("      <p><strong>Severity:</strong> ").append(vuln.getSeverity()).append("</p>\n");
        html.append("      <p><strong>File:</strong> ").append(vuln.getFilePath()).append("</p>\n");
        if (vuln.getCveId() != null) {
          html.append("      <p><strong>CVE:</strong> ").append(vuln.getCveId()).append("</p>\n");
        }
        html.append("      <p><strong>Description:</strong> ").append(vuln.getDescription()).append("</p>\n");

        if (vuln.getAiRecommendation() != null) {
          html.append("      <div class=\"recommendation\">\n");
          html.append("        <h4>AI-Recommended Fix</h4>\n");
          html.append("        <p>").append(vuln.getAiRecommendation().getSuggestion()).append("</p>\n");
          html.append("      </div>\n");
        }
        html.append("    </div>\n");
      }
    }

    html.append("  </section>\n");
    return html.toString();
  }

  /**
   * CSS styles for HTML report
   */
  private String getHTMLStyles() {
    return "body {\n" +
        "  font-family: -apple-system, system-ui, 'Segoe UI', Roboto, sans-serif;\n" +
        "  background: #010102;\n" +
        "  color: #f7f8f8;\n" +
        "  line-height: 1.6;\n" +
        "}\n" +
        ".container {\n" +
        "  max-width: 1200px;\n" +
        "  margin: 0 auto;\n" +
        "  padding: 24px;\n" +
        "}\n" +
        "header {\n" +
        "  border-bottom: 1px solid #23252a;\n" +
        "  padding-bottom: 24px;\n" +
        "  margin-bottom: 24px;\n" +
        "}\n" +
        "h1, h2, h3, h4 {\n" +
        "  margin-top: 24px;\n" +
        "  color: #f7f8f8;\n" +
        "}\n" +
        "section {\n" +
        "  background: #1a1a1f;\n" +
        "  border: 1px solid #23252a;\n" +
        "  border-radius: 12px;\n" +
        "  padding: 24px;\n" +
        "  margin-bottom: 24px;\n" +
        "}\n" +
        ".stats-grid {\n" +
        "  display: grid;\n" +
        "  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));\n" +
        "  gap: 16px;\n" +
        "}\n" +
        ".stat {\n" +
        "  background: #25252d;\n" +
        "  padding: 16px;\n" +
        "  border-radius: 8px;\n" +
        "  text-align: center;\n" +
        "}\n" +
        ".stat.critical { border-left: 4px solid #d1333d; }\n" +
        ".stat.high { border-left: 4px solid #ff7043; }\n" +
        ".stat.medium { border-left: 4px solid #ffa726; }\n" +
        ".stat.low { border-left: 4px solid #66bb6a; }\n" +
        ".count {\n" +
        "  display: block;\n" +
        "  font-size: 32px;\n" +
        "  font-weight: bold;\n" +
        "}\n" +
        ".finding {\n" +
        "  background: #25252d;\n" +
        "  padding: 16px;\n" +
        "  margin-bottom: 16px;\n" +
        "  border-radius: 8px;\n" +
        "  border-left: 4px solid;\n" +
        "}\n" +
        ".finding.critical { border-left-color: #d1333d; }\n" +
        ".finding.high { border-left-color: #ff7043; }\n" +
        ".finding.medium { border-left-color: #ffa726; }\n" +
        ".finding.low { border-left-color: #66bb6a; }\n" +
        ".recommendation {\n" +
        "  background: #1a1a1f;\n" +
        "  border-left: 2px solid #5e6ad2;\n" +
        "  padding: 12px;\n" +
        "  margin-top: 12px;\n" +
        "}\n" +
        "footer {\n" +
        "  text-align: center;\n" +
        "  margin-top: 48px;\n" +
        "  padding-top: 24px;\n" +
        "  border-top: 1px solid #23252a;\n" +
        "  color: #8a8f98;\n" +
        "}\n";
  }

  /**
   * Format date for display
   */
  private String formatDate(LocalDateTime dateTime) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    return dateTime.format(formatter);
  }
}
