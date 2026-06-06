package com.remedify.service;

import com.remedify.exception.ScanException;
import com.remedify.model.RepositoryScan;
import com.remedify.model.TestResult;
import com.remedify.repository.RepositoryScanRepository;
import com.remedify.repository.TestResultRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class BuildValidationService {

  private static final int BUILD_TIMEOUT_SECONDS = 900; // 15 minutes
  private static final Pattern TEST_RESULTS_PATTERN =
      Pattern.compile("Tests run: (\\d+), Failures: (\\d+), Errors: (\\d+)");

  private final RepositoryScanRepository scanRepository;
  private final TestResultRepository testResultRepository;

  public BuildValidationService(
      RepositoryScanRepository scanRepository,
      TestResultRepository testResultRepository) {
    this.scanRepository = scanRepository;
    this.testResultRepository = testResultRepository;
  }

  public void validateBuild(RepositoryScan scan) throws Exception {
    log.info("Starting build validation for scan: {}", scan.getId());

    if (scan.getClonedPath() == null || scan.getClonedPath().isEmpty()) {
      throw new ScanException("REPO_NOT_CLONED",
          "Repository must be cloned before build validation");
    }

    TestResult testResult = new TestResult();
    testResult.setScan(scan);

    try {
      // Step 1: Run Maven clean compile
      log.info("Running Maven clean compile for scan: {}", scan.getId());
      buildProject(scan.getClonedPath());
      testResult.setBuildSuccess(true);
      log.info("Maven build succeeded for scan: {}", scan.getId());

      // Step 2: Run Maven tests
      log.info("Running Maven tests for scan: {}", scan.getId());
      String testOutput = runTests(scan.getClonedPath());
      testResult.setLogs(truncateLogs(testOutput));

      // Step 3: Parse test results
      parseTestResults(testOutput, testResult);
      log.info("Test results for scan {}: Passed={}, Failed={}, Total={}",
          scan.getId(), testResult.getTestsPassed(), testResult.getTestsFailed(),
          testResult.getTotalTests());

      // Step 4: Save test results
      testResultRepository.save(testResult);

      // Step 5: Update scan status
      String statusMsg = String.format(
          "Build validation completed. Tests: %d passed, %d failed (%.1f%% pass rate)",
          testResult.getTestsPassed(),
          testResult.getTestsFailed(),
          testResult.getTotalTests() > 0 ? testResult.getPassRate() : 0);
      scan.setStatusMessage(statusMsg);
      scanRepository.save(scan);

      log.info("Build validation completed for scan: {}", scan.getId());

    } catch (Exception e) {
      log.error("Build validation failed for scan {}: {}", scan.getId(), e.getMessage(), e);
      testResult.setBuildSuccess(false);
      testResult.setLogs("Build failed: " + e.getMessage());

      try {
        testResultRepository.save(testResult);
      } catch (Exception saveEx) {
        log.error("Failed to save test result for scan: {}", scan.getId(), saveEx);
      }

      throw new ScanException("BUILD_FAILED",
          "Maven build or test execution failed: " + e.getMessage(), e);
    }
  }

  /**
   * Run Maven clean compile
   */
  private void buildProject(String repositoryPath) throws Exception {
    ProcessBuilder pb = new ProcessBuilder("mvn", "clean", "compile", "-q");
    pb.directory(new File(repositoryPath));
    pb.redirectErrorStream(true);

    Process process = null;
    try {
      log.debug("Starting Maven build in: {}", repositoryPath);
      process = pb.start();

      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      String line;
      while ((line = reader.readLine()) != null) {
        log.debug("Maven build output: {}", line);
      }

      boolean completed = process.waitFor(BUILD_TIMEOUT_SECONDS, TimeUnit.SECONDS);
      if (!completed) {
        process.destroyForcibly();
        throw new Exception("Maven build timed out after " + BUILD_TIMEOUT_SECONDS + " seconds");
      }

      int exitCode = process.exitValue();
      if (exitCode != 0) {
        throw new Exception("Maven build failed with exit code " + exitCode);
      }

      log.info("Maven compile completed successfully");

    } finally {
      if (process != null) {
        process.destroy();
      }
    }
  }

  /**
   * Run Maven tests
   */
  private String runTests(String repositoryPath) throws Exception {
    StringBuilder output = new StringBuilder();
    ProcessBuilder pb = new ProcessBuilder("mvn", "test", "-q");
    pb.directory(new File(repositoryPath));
    pb.redirectErrorStream(true);

    Process process = null;
    try {
      log.debug("Starting Maven tests in: {}", repositoryPath);
      process = pb.start();

      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      String line;
      while ((line = reader.readLine()) != null) {
        output.append(line).append("\n");
        log.debug("Maven test output: {}", line);
      }

      boolean completed = process.waitFor(BUILD_TIMEOUT_SECONDS, TimeUnit.SECONDS);
      if (!completed) {
        process.destroyForcibly();
        throw new Exception("Maven tests timed out after " + BUILD_TIMEOUT_SECONDS + " seconds");
      }

      // Note: Maven returns non-zero if tests fail, but we want to capture results
      int exitCode = process.exitValue();
      log.debug("Maven test exit code: {} (0=all passed, >0=failures or errors)", exitCode);

      log.info("Maven tests completed");
      return output.toString();

    } finally {
      if (process != null) {
        process.destroy();
      }
    }
  }

  /**
   * Parse test results from Maven output
   * Looks for: "Tests run: X, Failures: Y, Errors: Z"
   */
  private void parseTestResults(String output, TestResult testResult) {
    if (output == null || output.isEmpty()) {
      log.warn("No test output to parse");
      testResult.setTestsPassed(0);
      testResult.setTestsFailed(0);
      return;
    }

    Matcher matcher = TEST_RESULTS_PATTERN.matcher(output);
    if (matcher.find()) {
      int totalRun = Integer.parseInt(matcher.group(1));
      int failures = Integer.parseInt(matcher.group(2));
      int errors = Integer.parseInt(matcher.group(3));

      int passed = totalRun - failures - errors;
      testResult.setTestsPassed(passed);
      testResult.setTestsFailed(failures + errors);

      log.info("Parsed test results: {} run, {} passed, {} failed",
          totalRun, passed, (failures + errors));
    } else {
      log.warn("Could not parse test results from Maven output");
      testResult.setTestsPassed(0);
      testResult.setTestsFailed(0);
    }
  }

  /**
   * Truncate logs to 8KB to avoid database issues
   */
  private String truncateLogs(String logs) {
    if (logs == null) {
      return "";
    }
    int maxLength = 8192;
    if (logs.length() > maxLength) {
      return logs.substring(0, maxLength) + "\n... (truncated)";
    }
    return logs;
  }
}
