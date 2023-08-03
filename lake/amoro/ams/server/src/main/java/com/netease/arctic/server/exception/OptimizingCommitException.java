package com.netease.arctic.server.exception;

public class OptimizingCommitException extends Exception {

  private final boolean retryable;

  public OptimizingCommitException(String message, boolean retryable) {
    super(message);
    this.retryable = retryable;
  }

  public OptimizingCommitException(String message, Throwable cause) {
    super(message, cause);
    this.retryable = false;
  }


  public boolean isRetryable() {
    return retryable;
  }
}
