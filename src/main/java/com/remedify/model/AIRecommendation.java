package com.remedify.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_recommendations")
public class AIRecommendation {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vulnerability_id", nullable = false)
  private Vulnerability vulnerability;

  @Column(length = 4096, nullable = false)
  private String suggestion;

  private String estimatedEffort;

  @Column(nullable = false)
  private Boolean appliedManually = false;

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

  public Vulnerability getVulnerability() {
    return vulnerability;
  }

  public void setVulnerability(Vulnerability vulnerability) {
    this.vulnerability = vulnerability;
  }

  public String getSuggestion() {
    return suggestion;
  }

  public void setSuggestion(String suggestion) {
    this.suggestion = suggestion;
  }

  public String getEstimatedEffort() {
    return estimatedEffort;
  }

  public void setEstimatedEffort(String estimatedEffort) {
    this.estimatedEffort = estimatedEffort;
  }

  public Boolean getAppliedManually() {
    return appliedManually;
  }

  public void setAppliedManually(Boolean appliedManually) {
    this.appliedManually = appliedManually;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
