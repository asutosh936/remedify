package com.remedify.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class GitHubIntegration {

  @Value("${remedify.github.base-url}")
  private String baseUrl;

  @Value("${remedify.github.api-url}")
  private String apiUrl;

  @Value("${remedify.scanning.clone-timeout-seconds:300}")
  private int cloneTimeoutSeconds;

  public static class ProjectMetadata {
    public String buildTool;
    public String projectType;
    public boolean hasTests;
    public String language;

    public ProjectMetadata(String buildTool, String projectType, boolean hasTests, String language) {
      this.buildTool = buildTool;
      this.projectType = projectType;
      this.hasTests = hasTests;
      this.language = language;
    }

    @Override
    public String toString() {
      return "ProjectMetadata{" +
          "buildTool='" + buildTool + '\'' +
          ", projectType='" + projectType + '\'' +
          ", hasTests=" + hasTests +
          ", language='" + language + '\'' +
          '}';
    }
  }

  public void cloneRepository(String gitHubUrl, String targetPath) throws Exception {
    validateGitHubUrl(gitHubUrl);
    log.info("Cloning repository from {} to {}", gitHubUrl, targetPath);

    // Create target directory
    Path targetDir = Paths.get(targetPath);
    Files.createDirectories(targetDir);

    ProcessBuilder pb = new ProcessBuilder("git", "clone", gitHubUrl, targetPath);
    pb.directory(new File(targetPath).getParentFile());
    pb.redirectErrorStream(true);

    Process process = null;
    try {
      process = pb.start();

      // Read output for logging
      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      String line;
      while ((line = reader.readLine()) != null) {
        log.debug("Git clone output: {}", line);
      }

      // Wait for process with timeout
      boolean completed = process.waitFor(cloneTimeoutSeconds, TimeUnit.SECONDS);
      if (!completed) {
        process.destroyForcibly();
        throw new Exception("Git clone timed out after " + cloneTimeoutSeconds + " seconds");
      }

      int exitCode = process.exitValue();
      if (exitCode != 0) {
        throw new Exception("Git clone failed with exit code " + exitCode);
      }

      log.info("Successfully cloned repository to {}", targetPath);
    } catch (Exception e) {
      log.error("Failed to clone repository: {}", e.getMessage(), e);
      // Cleanup on failure
      try {
        Files.walk(targetDir)
            .sorted(java.util.Comparator.reverseOrder())
            .forEach(path -> {
              try {
                Files.delete(path);
              } catch (Exception ex) {
                log.warn("Failed to delete: {}", path, ex);
              }
            });
      } catch (Exception cleanupEx) {
        log.warn("Failed to cleanup directory after clone failure", cleanupEx);
      }
      throw e;
    } finally {
      if (process != null) {
        process.destroy();
      }
    }
  }

  public ProjectMetadata extractRepositoryMetadata(String targetPath) throws Exception {
    log.info("Extracting metadata from repository at {}", targetPath);
    Path repoPath = Paths.get(targetPath);

    if (!Files.exists(repoPath)) {
      throw new Exception("Repository path does not exist: " + targetPath);
    }

    String buildTool = "unknown";
    String projectType = "unknown";
    boolean hasTests = false;
    String language = "unknown";

    // Detect build tool and language
    if (Files.exists(repoPath.resolve("pom.xml"))) {
      buildTool = "Maven";
      projectType = "Java";
      language = "Java";
      hasTests = Files.exists(repoPath.resolve("src/test"));
    } else if (Files.exists(repoPath.resolve("build.gradle")) || Files.exists(repoPath.resolve("build.gradle.kts"))) {
      buildTool = "Gradle";
      projectType = "Java";
      language = "Java";
      hasTests = Files.exists(repoPath.resolve("src/test"));
    } else if (Files.exists(repoPath.resolve("package.json"))) {
      buildTool = "npm";
      projectType = "Node.js";
      language = "JavaScript/TypeScript";
      hasTests = Files.exists(repoPath.resolve("test")) || Files.exists(repoPath.resolve("__tests__"));
    } else if (Files.exists(repoPath.resolve("requirements.txt")) || Files.exists(repoPath.resolve("setup.py"))) {
      buildTool = "pip/setuptools";
      projectType = "Python";
      language = "Python";
      hasTests = Files.exists(repoPath.resolve("tests"));
    } else if (Files.exists(repoPath.resolve("go.mod"))) {
      buildTool = "Go";
      projectType = "Go";
      language = "Go";
      hasTests = Files.walk(repoPath)
          .anyMatch(p -> p.getFileName().toString().endsWith("_test.go"));
    }

    ProjectMetadata metadata = new ProjectMetadata(buildTool, projectType, hasTests, language);
    log.info("Extracted repository metadata: {}", metadata);
    return metadata;
  }

  public void validateGitHubUrl(String url) throws Exception {
    log.debug("Validating GitHub URL: {}", url);

    if (url == null || url.trim().isEmpty()) {
      throw new IllegalArgumentException("GitHub URL cannot be empty");
    }

    // Support both HTTPS and SSH URLs
    if (url.startsWith("https://github.com/") || url.startsWith("git@github.com:")) {
      // Extract owner/repo
      String[] parts;
      if (url.startsWith("https://")) {
        parts = url.replace("https://github.com/", "").replace(".git", "").split("/");
      } else {
        parts = url.replace("git@github.com:", "").replace(".git", "").split("/");
      }

      if (parts.length < 2) {
        throw new IllegalArgumentException("Invalid GitHub URL format: " + url);
      }

      log.debug("GitHub URL validated. Owner: {}, Repo: {}", parts[0], parts[1]);
    } else {
      throw new IllegalArgumentException(
          "Invalid GitHub URL. Must start with 'https://github.com/' or 'git@github.com:': " + url);
    }
  }
}
