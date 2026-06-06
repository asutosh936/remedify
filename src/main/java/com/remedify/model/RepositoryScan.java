package com.remedify.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "repository_scans")
public class RepositoryScan {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private String gitHubUrl;

  @Column(nullable = false)
  private String repositoryName;

  private String clonedPath;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ScanStage currentStage = ScanStage.CLONING;

  @Column(length = 1024)
  private String statusMessage;

  @Column(nullable = false)
  private Integer retryCount = 0;

  @OneToMany(mappedBy = "scan", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Vulnerability> vulnerabilities = new ArrayList<>();

  @OneToOne(mappedBy = "scan", cascade = CascadeType.ALL)
  private TestResult testResult;

  @OneToOne(mappedBy = "scan", cascade = CascadeType.ALL)
  private ScanReport scanReport;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  // Getters and Setters
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getGitHubUrl() {
    return gitHubUrl;
  }

  public void setGitHubUrl(String gitHubUrl) {
    this.gitHubUrl = gitHubUrl;
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public void setRepositoryName(String repositoryName) {
    this.repositoryName = repositoryName;
  }

  public String getClonedPath() {
    return clonedPath;
  }

  public void setClonedPath(String clonedPath) {
    this.clonedPath = clonedPath;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public ScanStage getCurrentStage() {
    return currentStage;
  }

  public void setCurrentStage(ScanStage currentStage) {
    this.currentStage = currentStage;
  }

  public String getStatusMessage() {
    return statusMessage;
  }

  public void setStatusMessage(String statusMessage) {
    this.statusMessage = statusMessage;
  }

  public Integer getRetryCount() {
    return retryCount;
  }

  public void setRetryCount(Integer retryCount) {
    this.retryCount = retryCount;
  }

  public List<Vulnerability> getVulnerabilities() {
    return vulnerabilities;
  }

  public void setVulnerabilities(List<Vulnerability> vulnerabilities) {
    this.vulnerabilities = vulnerabilities;
  }

  public TestResult getTestResult() {
    return testResult;
  }

  public void setTestResult(TestResult testResult) {
    this.testResult = testResult;
  }

  public ScanReport getScanReport() {
    return scanReport;
  }

  public void setScanReport(ScanReport scanReport) {
    this.scanReport = scanReport;
  }

  public int getHighSeverityCount() {
    return (int)
        vulnerabilities.stream()
            .filter(v -> v.getSeverity().isHighSeverity())
            .count();
  }

  public int getTotalVulnerabilityCount() {
    return vulnerabilities.size();
  }
}
