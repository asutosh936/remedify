package com.remedify.model;

public enum ScanStage {
  CLONING("Repository Clone"),
  SCANNING("Vulnerability Detection"),
  RECOMMENDING("AI Recommendations"),
  VALIDATING("Build & Test Validation"),
  REPORTING("Report Generation"),
  COMPLETED("Completed");

  private final String displayName;

  ScanStage(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
