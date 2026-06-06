package com.remedify.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "test_results")
public class TestResult {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "scan_id", nullable = false)
  private RepositoryScan scan;

  @Column(nullable = false)
  private Boolean buildSuccess = false;

  @Column(nullable = false)
  private Integer testsPassed = 0;

  @Column(nullable = false)
  private Integer testsFailed = 0;

  @Column(length = 8192)
  private String logs;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }

  // Getters and Setters
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public RepositoryScan getScan() {
    return scan;
  }

  public void setScan(RepositoryScan scan) {
    this.scan = scan;
  }

  public Boolean getBuildSuccess() {
    return buildSuccess;
  }

  public void setBuildSuccess(Boolean buildSuccess) {
    this.buildSuccess = buildSuccess;
  }

  public Integer getTestsPassed() {
    return testsPassed;
  }

  public void setTestsPassed(Integer testsPassed) {
    this.testsPassed = testsPassed;
  }

  public Integer getTestsFailed() {
    return testsFailed;
  }

  public void setTestsFailed(Integer testsFailed) {
    this.testsFailed = testsFailed;
  }

  public String getLogs() {
    return logs;
  }

  public void setLogs(String logs) {
    this.logs = logs;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public int getTotalTests() {
    return testsPassed + testsFailed;
  }

  public double getPassRate() {
    if (getTotalTests() == 0) {
      return 0.0;
    }
    return (double) testsPassed / getTotalTests() * 100;
  }
}
