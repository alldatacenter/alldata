package com.platform.field.sources;

import org.apache.flink.util.Preconditions;

final class Throttler {

  private final long throttleBatchSize;
  private final long nanosPerBatch;

  private long endOfNextBatchNanos;
  private int currentBatch;

  Throttler(long maxRecordsPerSecond, int numberOfParallelSubtasks) {
    Preconditions.checkArgument(
        maxRecordsPerSecond == -1 || maxRecordsPerSecond > 0,
        "maxRecordsPerSecond must be positive or -1 (infinite)");
    Preconditions.checkArgument(
        numberOfParallelSubtasks > 0, "numberOfParallelSubtasks must be greater than 0");

    if (maxRecordsPerSecond == -1) {
      // unlimited speed
      throttleBatchSize = -1;
      nanosPerBatch = 0;
      endOfNextBatchNanos = System.nanoTime() + nanosPerBatch;
      currentBatch = 0;
      return;
    }
    final float ratePerSubtask = (float) maxRecordsPerSecond / numberOfParallelSubtasks;

    if (ratePerSubtask >= 10000) {
      // high rates: all throttling in intervals of 2ms
      throttleBatchSize = (int) ratePerSubtask / 500;
      nanosPerBatch = 2_000_000L;
    } else {
      throttleBatchSize = ((int) (ratePerSubtask / 20)) + 1;
      nanosPerBatch = ((int) (1_000_000_000L / ratePerSubtask)) * throttleBatchSize;
    }
    this.endOfNextBatchNanos = System.nanoTime() + nanosPerBatch;
    this.currentBatch = 0;
  }

  void throttle() throws InterruptedException {
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
