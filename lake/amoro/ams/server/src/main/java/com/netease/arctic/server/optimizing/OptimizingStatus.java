package com.netease.arctic.server.optimizing;

public enum OptimizingStatus {
  FULL_OPTIMIZING("full", true),
  MAJOR_OPTIMIZING("major", true),
  MINOR_OPTIMIZING("minor", true),
  COMMITTING("committing", true),
  PENDING("pending", false),
  IDLE("idle", false);
  private String displayValue;

  private boolean isProcessing;

  OptimizingStatus(String displayValue, boolean isProcessing) {
    this.displayValue = displayValue;
    this.isProcessing = isProcessing;
  }

  public boolean isProcessing() {
    return isProcessing;
  }

  public String displayValue() {
    return displayValue;
  }
}