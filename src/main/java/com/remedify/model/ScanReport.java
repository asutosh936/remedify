package com.remedify.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "scan_reports")
public class ScanReport {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "scan_id", nullable = false)
  private RepositoryScan scan;

  @Column(columnDefinition = "LONGTEXT")
  private String htmlReport;

  @Column(columnDefinition = "LONGTEXT")
  private String jsonReport;

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

  public String getHtmlReport() {
    return htmlReport;
  }

  public void setHtmlReport(String htmlReport) {
    this.htmlReport = htmlReport;
  }

  public String getJsonReport() {
    return jsonReport;
  }

  public void setJsonReport(String jsonReport) {
    this.jsonReport = jsonReport;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
