package com.platform.backend.datasource;

import com.google.common.base.Preconditions;

/** Utility to throttle a thread to a given number of executions (records) per second. */
final class Throttler {

  private long throttleBatchSize;
  private long nanosPerBatch;

  private long endOfNextBatchNanos;
  private int currentBatch;

  Throttler(long maxRecordsPerSecond) {
    setup(maxRecordsPerSecond);
  }

  public void adjustMaxRecordsPerSecond(long maxRecordsPerSecond) {
    setup(maxRecordsPerSecond);
  }

  private synchronized void setup(long maxRecordsPerSecond) {
    Preconditions.checkArgument(
        maxRecordsPerSecond == -1 || maxRecordsPerSecond > 0,
        "maxRecordsPerSecond must be positive or -1 (infinite)");

    if (maxRecordsPerSecond == -1) {
      // unlimited speed
      throttleBatchSize = -1;
      nanosPerBatch = 0;
      endOfNextBatchNanos = System.nanoTime() + nanosPerBatch;
      currentBatch = 0;
      return;
    }

    if (maxRecordsPerSecond >= 10000) {
      // high rates: all throttling in intervals of 2ms
      throttleBatchSize = (int) maxRecordsPerSecond / 500;
      nanosPerBatch = 2_000_000L;
    } else {
      throttleBatchSize = ((int) (maxRecordsPerSecond / 20)) + 1;
      nanosPerBatch = ((int) (1_000_000_000L / maxRecordsPerSecond)) * throttleBatchSize;
    }
    this.endOfNextBatchNanos = System.nanoTime() + nanosPerBatch;
    this.currentBatch = 0;
  }

  synchronized void throttle() throws InterruptedException {
    if (throttleBatchSize == -1) {
      return;
    }
    if (++currentBatch != throttleBatchSize) {
      return;
    }
    currentBatch = 0;

    final long now = System.nanoTime();
    final int millisRemaining = (int) ((endOfNextBatchNanos - now) / 1_000_000);

    if (millisRemaining > 0) {
      endOfNextBatchNanos += nanosPerBatch;
      Thread.sleep(millisRemaining);
    } else {
      endOfNextBatchNanos = now + nanosPerBatch;
    }
  }
}
