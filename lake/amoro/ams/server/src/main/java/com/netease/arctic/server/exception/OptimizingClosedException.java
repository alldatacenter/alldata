package com.netease.arctic.server.exception;

public class OptimizingClosedException extends ArcticRuntimeException {

  private long processId;

  public OptimizingClosedException(long processId) {
    super("Optimizing process already closed, ignore " + processId);
    this.processId = processId;
  }

  public long getProcessId() {
    return processId;
  }
}
