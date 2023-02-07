/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.physical.impl.xsort;

import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Computes the memory needs for input batches, spill batches and merge
 * batches. The key challenges that this code tries to overcome are:
 * <ul>
 * <li>Drill is not designed for the small memory allocations,
 * but the planner may provide such allocations because the memory per
 * query is divided among slices (minor fragments) and among buffering
 * operators, leaving very little per operator.</li>
 * <li>Drill does not provide the detailed memory information needed to
 * carefully manage memory in tight constraints.</li>
 * <li>But, Drill has a death penalty for going over the memory limit.</li>
 * </ul>
 * As a result, this class is a bit of a hack: it attempt to consider a
 * number of ill-defined factors in order to divide up memory use in a
 * way that prevents OOM errors.
 * <p>
 * First, it is necessary to differentiate two concepts:
 * <ul>
 * <li>The <i>data size</i> of a batch: the amount of memory needed to hold
 * the data itself. The data size is constant for any given batch.</li>
 * <li>The <i>buffer size</i> of the buffers that hold the data. The buffer
 * size varies wildly depending on how the batch was produced.</li>
 * </ul>
 * The three kinds of buffer layouts seen to date include:
 * <ul>
 * <li>One buffer per vector component (data, offsets, null flags, etc.)
 * &ndash; create by readers, project and other operators.</li>
 * <li>One buffer for the entire batch, with each vector component using
 * a slice of the overall buffer. &ndash; case for batches deserialized from
 * exchanges.</li>
 * <li>One buffer for each top-level vector, with component vectors
 * using slices of the overall vector buffer &ndash; the result of reading
 * spilled batches from disk.</li>
 * </ul>
 * In each case, buffer sizes are power-of-two rounded from the data size.
 * But since the data is grouped differently in each case, the resulting buffer
 * sizes vary considerably.
 * <p>
 * As a result, we can never be sure of the amount of memory needed for a
 * batch. So, we have to estimate based on a number of factors:
 * <ul>
 * <li>Uses the {@link org.apache.drill.exec.record.RecordBatchSizer} to estimate the data size and
 * buffer size of each incoming batch.</li>
 * <li>Estimates the internal fragmentation due to power-of-two rounding.</li>
 * <li>Configured preferences for spill and output batches.</li>
 * </ul>
 * The code handles "normal" and "low" memory conditions.
 * <ul>
 * <li>In normal memory, we simply work out the number of preferred-size
 * batches that fit in memory (based on the predicted buffer size.)</li>
 * <li>In low memory, we divide up the available memory to produce the
 * spill and merge batch sizes. The sizes will be less than the configured
 * preference.</li>
 * </ul>
 * <p>
 * The sort has two key configured parameters: the spill file size and the
 * size of the output (downstream) batch. The spill file size is chosen to
 * be large enough to ensure efficient I/O, but not so large as to overwhelm
 * any one spill directory. The output batch size is chosen to be large enough
 * to amortize the per-batch overhead over the maximum number of records, but
 * not so large as to overwhelm downstream operators. Setting these parameters
 * is a judgment call.
 * <p>
 * Under limited memory, the above sizes may be too big for the space available.
 * For example, the default spill file size is 256 MB. But, if the sort is
 * only given 50 MB, then spill files will be smaller. The default output batch
 * size is 16 MB, but if the sort is given only 20 MB, then the output batch must
 * be smaller. The low memory logic starts with the memory available and works
 * backwards to figure out spill batch size, output batch size and spill file
 * size. The sizes will be smaller than optimal, but as large as will fit in
 * the memory provided.
 */

public class SortMemoryManager {

  private static final Logger logger = LoggerFactory.getLogger(SortMemoryManager.class);

  /**
   * Estimate for typical internal fragmentation in a buffer due to power-of-two
   * rounding on vectors.
   * <p>
   * <p>
   * <pre>[____|__$__]</pre>
   * In the above, the brackets represent the whole vector. The
   * first half is always full. The $ represents the end of data.
   * When the first half filled, the second
   * half was allocated. On average, the second half will be half full.
   * This means that, on average, 1/4 of the allocated space is
   * unused (the definition of internal fragmentation.)
   */

  public static final double INTERNAL_FRAGMENTATION_ESTIMATE = 1.0/4.0;

  /**
   * Given a buffer, this is the assumed amount of space
   * available for data. (Adding more will double the buffer
   * size half the time.)
   */

  public static final double PAYLOAD_FROM_BUFFER = 1 - INTERNAL_FRAGMENTATION_ESTIMATE;

  /**
   * Given a data size, this is the multiplier to create the buffer
   * size estimate. (Note: since we work with aggregate batches, we
   * cannot simply round up to the next power of two: rounding is done
   * on a vector-by-vector basis. Here we need to estimate the aggregate
   * effect of rounding.
   */

  public static final double BUFFER_FROM_PAYLOAD = 3.0 / 2.0;

  /**
   * On really bad days, we will add one more byte (or value) to a vector
   * than fits in a power-of-two sized buffer, forcing a doubling. In this
   * case, half the resulting buffer is empty.
   */

  public static final double WORST_CASE_BUFFER_RATIO = 2.0;

  /**
   * Desperate attempt to keep spill batches from being too small in low memory.
   * <p>
   * The number is also used for logging: the system will log a warning if
   * batches fall below this number which may represent too little memory
   * allocated for the job at hand. (Queries operate on big data: many records.
   * Batches with too few records are a probable performance hit. But, what is
   * too few? It is a judgment call.)
   */

  public static final int MIN_ROWS_PER_SORT_BATCH = 100;
  public static final double LOW_MEMORY_MERGE_BATCH_RATIO = 0.25;

  public static class BatchSizeEstimate {
    int dataSize;
    int expectedBufferSize;
    int maxBufferSize;

    public void setFromData(int dataSize) {
      this.dataSize = dataSize;
      expectedBufferSize = multiply(dataSize, BUFFER_FROM_PAYLOAD);
      maxBufferSize = multiply(dataSize, WORST_CASE_BUFFER_RATIO);
    }

    public void setFromBuffer(int bufferSize) {
      expectedBufferSize = bufferSize;
      dataSize = multiply(bufferSize, PAYLOAD_FROM_BUFFER);
      maxBufferSize = multiply(dataSize, WORST_CASE_BUFFER_RATIO);
    }

    public void setFromWorstCaseBuffer(int bufferSize) {
      maxBufferSize = bufferSize;
      dataSize = multiply(maxBufferSize, 1 / WORST_CASE_BUFFER_RATIO);
      expectedBufferSize = multiply(dataSize, BUFFER_FROM_PAYLOAD);
    }
 }

  /**
   * Maximum memory this operator may use. Usually comes from the
   * operator definition, but may be overridden by a configuration
   * parameter for unit testing.
   */

  private final long memoryLimit;

  /**
   * Estimated size of the records for this query, updated on each
   * new batch received from upstream.
   */

  private int estimatedRowWidth;

  /**
   * Size of the merge batches that this operator produces. Generally
   * the same as the merge batch size, unless low memory forces a smaller
   * value.
   */

  private final BatchSizeEstimate mergeBatchSize = new BatchSizeEstimate();

  /**
   * Estimate of the input batch size based on the largest batch seen
   * thus far.
   */
  private final BatchSizeEstimate inputBatchSize = new BatchSizeEstimate();

  /**
   * Maximum memory level before spilling occurs. That is, we can buffer input
   * batches in memory until we reach the level given by the buffer memory pool.
   */

  private long bufferMemoryLimit;

  /**
   * Maximum memory that can hold batches during the merge
   * phase.
   */

  private long mergeMemoryLimit;

  /**
   * The target size for merge batches sent downstream.
   */

  private int preferredMergeBatchSize;

  /**
   * The configured size for each spill batch.
   */
  private int preferredSpillBatchSize;

  /**
   * Estimated number of rows that fit into a single spill batch.
   */

  private int spillBatchRowCount;

  /**
   * The estimated actual spill batch size which depends on the
   * details of the data rows for any particular query.
   */

  private final BatchSizeEstimate spillBatchSize = new BatchSizeEstimate();

  /**
   * The number of records to add to each output batch sent to the
   * downstream operator or spilled to disk.
   */

  private int mergeBatchRowCount;

  private SortConfig config;

  private boolean potentialOverflow;

  private boolean isLowMemory;

  private boolean performanceWarning;

  public SortMemoryManager(SortConfig config, long opMemoryLimit) {
    this.config = config;

    // The maximum memory this operator can use as set by the
    // operator definition (propagated to the allocator.)

    final long configMemoryLimit = config.maxMemory();
    memoryLimit = (configMemoryLimit == 0) ? opMemoryLimit
                : Math.min(opMemoryLimit, configMemoryLimit);

    preferredSpillBatchSize = config.spillBatchSize();
    preferredMergeBatchSize = config.mergeBatchSize();

    // Initialize the buffer memory limit for the first batch.
    // Assume 1/2 of (allocated - spill batch size).

    bufferMemoryLimit = (memoryLimit - config.spillBatchSize()) / 2;
    if (bufferMemoryLimit < 0) {
      // Bad news: not enough for even the spill batch.
      // Assume half of memory, will adjust later.
      bufferMemoryLimit = memoryLimit / 2;
    }

    if (memoryLimit == opMemoryLimit) {
      logger.debug("Memory config: Allocator limit = {}", memoryLimit);
    } else {
      logger.debug("Memory config: Allocator limit = {}, Configured limit: {}",
                   opMemoryLimit, memoryLimit);
    }
  }

  /**
   * Update the data-driven memory use numbers including:
   * <ul>
   * <li>The average size of incoming records.</li>
   * <li>The estimated spill and output batch size.</li>
   * <li>The estimated number of average-size records per
   * spill and output batch.</li>
   * <li>The amount of memory set aside to hold the incoming
   * batches before spilling starts.</li>
   * </ul>
   * <p>
   * Under normal circumstances, the amount of memory available is much
   * larger than the input, spill or merge batch sizes. The primary question
   * is to determine how many input batches we can buffer during the load
   * phase, and how many spill batches we can merge during the merge
   * phase.
   *
   * @param batchDataSize the overall size of the current batch received from
   * upstream
   * @param batchRowWidth the average width in bytes (including overhead) of
   * rows in the current input batch
   * @param batchRowCount the number of actual (not filtered) records in
   * that upstream batch
   * @return true if the estimates changed, false if the previous estimates
   * remain valid
   */

  public boolean updateEstimates(int batchDataSize, int batchRowWidth, int batchRowCount) {

    // The record count should never be zero, but better safe than sorry...

    if (batchRowCount == 0) {
      return false; }


    // Update input batch estimates.
    // Go no further if nothing changed.

    if (! updateInputEstimates(batchDataSize, batchRowWidth, batchRowCount)) {
      return false;
    }

    updateSpillSettings();
    updateMergeSettings();
    adjustForLowMemory();
    logSettings(batchRowCount);
    return true;
  }

  private boolean updateInputEstimates(int batchDataSize, int batchRowWidth, int batchRowCount) {

    // The row width may end up as zero if all fields are nulls or some
    // other unusual situation. In this case, assume a width of 10 just
    // to avoid lots of special case code.

    if (batchRowWidth == 0) {
      batchRowWidth = 10;
    }

    // We know the batch size and number of records. Use that to estimate
    // the average record size. Since a typical batch has many records,
    // the average size is a fairly good estimator. Note that the batch
    // size includes not just the actual vector data, but any unused space
    // resulting from power-of-two allocation. This means that we don't
    // have to do size adjustments for input batches as we will do below
    // when estimating the size of other objects.

    // Record sizes may vary across batches. To be conservative, use
    // the largest size observed from incoming batches.

    int origRowEstimate = estimatedRowWidth;
    estimatedRowWidth = Math.max(estimatedRowWidth, batchRowWidth);

    // Maintain an estimate of the incoming batch size: the largest
    // batch yet seen. Used to reserve memory for the next incoming
    // batch. Because we are using the actual observed batch size,
    // the size already includes overhead due to power-of-two rounding.

    long origInputBatchSize = inputBatchSize.dataSize;
    inputBatchSize.setFromData(Math.max(inputBatchSize.dataSize, batchDataSize));

    // Return whether anything changed.

    return estimatedRowWidth > origRowEstimate ||
           inputBatchSize.dataSize > origInputBatchSize;
  }

  /**
   * Determine the number of records to spill per spill batch. The goal is to
   * spill batches of either 64K records, or as many records as fit into the
   * amount of memory dedicated to each spill batch, whichever is less.
   */

  private void updateSpillSettings() {

    spillBatchRowCount = rowsPerBatch(preferredSpillBatchSize);

    // But, don't allow spill batches to be too small; we pay too
    // much overhead cost for small row counts.

    spillBatchRowCount = Math.max(spillBatchRowCount, MIN_ROWS_PER_SORT_BATCH);

    // Compute the actual spill batch size which may be larger or smaller
    // than the preferred size depending on the row width.

    spillBatchSize.setFromData(spillBatchRowCount * estimatedRowWidth);

    // Determine the minimum memory needed for spilling. Spilling is done just
    // before accepting a spill batch, so we must spill if we don't have room for a
    // (worst case) input batch. To spill, we need room for the spill batch created
    // by merging the batches already in memory. This is a memory calculation,
    // so use the buffer size for the spill batch.

    bufferMemoryLimit = memoryLimit - 2 * spillBatchSize.maxBufferSize;
  }

  /**
   * Determine the number of records per batch per merge step. The goal is to
   * merge batches of either 64K records, or as many records as fit into the
   * amount of memory dedicated to each merge batch, whichever is less.
   */

  private void updateMergeSettings() {

    mergeBatchRowCount = rowsPerBatch(preferredMergeBatchSize);

    // But, don't allow merge batches to be too small; we pay too
    // much overhead cost for small row counts.

    mergeBatchRowCount = Math.max(mergeBatchRowCount, MIN_ROWS_PER_SORT_BATCH);

    // Compute the actual merge batch size.

    mergeBatchSize.setFromData(mergeBatchRowCount * estimatedRowWidth);

    // The merge memory pool assumes we can spill all input batches. The memory
    // available to hold spill batches for merging is total memory minus the
    // expected output batch size.

    mergeMemoryLimit = memoryLimit - mergeBatchSize.maxBufferSize;
  }

  /**
   * In a low-memory situation we have to approach the memory assignment
   * problem from a different angle. Memory is low enough that we can't
   * fit the incoming batches (of a size decided by the upstream operator)
   * and our usual spill or merge batch sizes. Instead, we have to
   * determine the largest spill and merge batch sizes possible given
   * the available memory, input batch size and row width. We shrink the
   * sizes of the batches we control to try to make things fit into limited
   * memory. At some point, however, if we cannot fit even two input
   * batches and even the smallest merge match, then we will run into an
   * out-of-memory condition and we log a warning.
   * <p>
   * Note that these calculations are a bit crazy: it is Drill that
   * decided to allocate the small memory, it is Drill that created the
   * large incoming batches, and so it is Drill that created the low
   * memory situation. Over time, a better fix for this condition is to
   * control memory usage at the query level so that the sort is guaranteed
   * to have sufficient memory. But, since we don't yet have the luxury
   * of making such changes, we just live with the situation as we find
   * it.
   */

  private void adjustForLowMemory() {

    potentialOverflow = false;
    performanceWarning = false;

    // Input batches are assumed to have typical fragmentation. Experience
    // shows that spilled batches have close to the maximum fragmentation.

    long loadHeadroom = bufferMemoryLimit - 2 * inputBatchSize.expectedBufferSize;
    long mergeHeadroom = mergeMemoryLimit - 2 * spillBatchSize.maxBufferSize;
    isLowMemory = (loadHeadroom < 0  |  mergeHeadroom < 0);
    if (! isLowMemory) {
      return; }

    lowMemoryInternalBatchSizes();

    // Sanity check: if we've been given too little memory to make progress,
    // issue a warning but proceed anyway. Should only occur if something is
    // configured terribly wrong.

    long minNeeds = 2 * inputBatchSize.expectedBufferSize + spillBatchSize.maxBufferSize;
    if (minNeeds > memoryLimit) {
      logger.warn("Potential memory overflow during load phase! " +
          "Minimum needed = {} bytes, actual available = {} bytes",
          minNeeds, memoryLimit);
      bufferMemoryLimit = 0;
      potentialOverflow = true;
    }

    // Sanity check

    minNeeds = 2 * spillBatchSize.expectedBufferSize + mergeBatchSize.expectedBufferSize;
    if (minNeeds > memoryLimit) {
      logger.warn("Potential memory overflow during merge phase! " +
          "Minimum needed = {} bytes, actual available = {} bytes",
          minNeeds, memoryLimit);
      mergeMemoryLimit = 0;
      potentialOverflow = true;
    }

    // Performance warning

    if (potentialOverflow) {
      return;
    }
    if (spillBatchSize.dataSize < config.spillBatchSize()  &&
        spillBatchRowCount < Character.MAX_VALUE) {
      logger.warn("Potential performance degredation due to low memory. " +
                  "Preferred spill batch size: {}, actual: {}, rows per batch: {}",
                  config.spillBatchSize(), spillBatchSize.dataSize,
                  spillBatchRowCount);
      performanceWarning = true;
    }
    if (mergeBatchSize.dataSize < config.mergeBatchSize()  &&
        mergeBatchRowCount < Character.MAX_VALUE) {
      logger.warn("Potential performance degredation due to low memory. " +
                  "Preferred merge batch size: {}, actual: {}, rows per batch: {}",
                  config.mergeBatchSize(), mergeBatchSize.dataSize,
                  mergeBatchRowCount);
      performanceWarning = true;
    }
  }

  /**
   * If we are in a low-memory condition, then we might not have room for the
   * default spill batch size. In that case, pick a smaller size based on
   * the observation that we need two input batches and
   * one spill batch to make progress.
   */

  private void lowMemoryInternalBatchSizes() {

    // The "expected" size is with power-of-two rounding in some vectors.
    // We later work backwards to the row count assuming average internal
    // fragmentation.

    // Must hold two input batches. Use half of the rest for the spill batch.
    // In a really bad case, the number here may be negative. We'll fix
    // it below.

    int spillBufferSize = (int) (memoryLimit - 2 * inputBatchSize.maxBufferSize) / 2;

    // But, in the merge phase, we need two spill batches and one output batch.
    // (Assume that the spill and merge are equal sizes.)

    spillBufferSize = (int) Math.min(spillBufferSize, memoryLimit/4);

    // Compute the size from the buffer. Assume worst-case
    // fragmentation (as is typical when reading from the spill file.)

    spillBatchSize.setFromWorstCaseBuffer(spillBufferSize);

    // Must hold at least one row to spill. That is, we can make progress if we
    // create spill files that consist of single-record batches.

    int spillDataSize = Math.min(spillBatchSize.dataSize, config.spillBatchSize());
    spillDataSize = Math.max(spillDataSize, estimatedRowWidth);
    if (spillDataSize != spillBatchSize.dataSize) {
      spillBatchSize.setFromData(spillDataSize);
    }

    // Work out the spill batch count needed by the spill code. Allow room for
    // power-of-two rounding.

    spillBatchRowCount = rowsPerBatch(spillBatchSize.dataSize);

    // Finally, figure out when we must spill.

    bufferMemoryLimit = memoryLimit - 2 * spillBatchSize.maxBufferSize;
    bufferMemoryLimit = Math.max(bufferMemoryLimit, 0);

    // Assume two spill batches must be merged (plus safety margin.)
    // The rest can be give to the merge batch.

    long mergeBufferSize = memoryLimit - 2 * spillBatchSize.maxBufferSize;

    // The above calcs assume that the merge batch size is the same as
    // the spill batch size (the division by three.)
    // For merge batch, we must hold at least two spill batches and
    // one output batch, which is why we assumed 3 spill batches.

    mergeBatchSize.setFromBuffer((int) mergeBufferSize);
    int mergeDataSize = Math.min(mergeBatchSize.dataSize, config.mergeBatchSize());
    mergeDataSize = Math.max(mergeDataSize, estimatedRowWidth);
    if (mergeDataSize != mergeBatchSize.dataSize) {
      mergeBatchSize.setFromData(spillDataSize);
    }

    mergeBatchRowCount = rowsPerBatch(mergeBatchSize.dataSize);
    mergeMemoryLimit = Math.max(2 * spillBatchSize.expectedBufferSize, memoryLimit - mergeBatchSize.maxBufferSize);
  }

  /**
   * Log the calculated values. Turn this on if things seem amiss.
   * Message will appear only when the values change.
   */

  private void logSettings(int actualRecordCount) {

    logger.debug("Input Batch Estimates: record size = {} bytes; net = {} bytes, gross = {}, records = {}",
                 estimatedRowWidth, inputBatchSize.dataSize,
                 inputBatchSize.expectedBufferSize, actualRecordCount);
    logger.debug("Spill batch size: net = {} bytes, gross = {} bytes, records = {}; spill file = {} bytes",
                 spillBatchSize.dataSize, spillBatchSize.expectedBufferSize,
                 spillBatchRowCount, config.spillFileSize());
    logger.debug("Output batch size: net = {} bytes, gross = {} bytes, records = {}",
                 mergeBatchSize.dataSize, mergeBatchSize.expectedBufferSize,
                 mergeBatchRowCount);
    logger.debug("Available memory: {}, buffer memory = {}, merge memory = {}",
                 memoryLimit, bufferMemoryLimit, mergeMemoryLimit);

    // Performance warnings due to low row counts per batch.
    // Low row counts cause excessive per-batch overhead and hurt
    // performance.

    if (spillBatchRowCount < MIN_ROWS_PER_SORT_BATCH) {
      logger.warn("Potential performance degredation due to low memory or large input row. " +
                  "Preferred spill batch row count: {}, actual: {}",
                  MIN_ROWS_PER_SORT_BATCH, spillBatchRowCount);
      performanceWarning = true;
    }
    if (mergeBatchRowCount < MIN_ROWS_PER_SORT_BATCH) {
      logger.warn("Potential performance degredation due to low memory or large input row. " +
                  "Preferred merge batch row count: {}, actual: {}",
                  MIN_ROWS_PER_SORT_BATCH, mergeBatchRowCount);
      performanceWarning = true;
    }
  }

  public enum MergeAction { SPILL, MERGE, NONE }

  public static class MergeTask {
    public MergeAction action;
    public int count;

    public MergeTask(MergeAction action, int count) {
      this.action = action;
      this.count = count;
    }
  }

  /**
   * Choose a consolidation option during the merge phase depending on memory
   * available. Preference is given to moving directly onto merging (with no
   * additional spilling) when possible. But, if memory pressures don't allow
   * this, we must spill batches and/or merge on-disk spilled runs, to reduce
   * the final set of runs to something that can be merged in the available
   * memory.
   * <p>
   * Logic is here (returning an enum) rather than in the merge code to allow
   * unit testing without actually needing batches in memory.
   *
   * @param allocMemory
   *          amount of memory currently allocated (this class knows the total
   *          memory available)
   * @param inMemCount
   *          number of incoming batches in memory (the number is important, not
   *          the in-memory size; we get the memory size from
   *          <tt>allocMemory</tt>)
   * @param spilledRunsCount
   *          the number of runs sitting on disk to be merged
   * @return whether to <tt>SPILL</tt> in-memory batches, whether to
   *         <tt>MERGE<tt> on-disk batches to create a new, larger run, or whether
   *         to do nothing (<tt>NONE</tt>) and instead advance to the final merge
   */

  public MergeTask consolidateBatches(long allocMemory, int inMemCount, int spilledRunsCount) {

    assert allocMemory == 0 || inMemCount > 0;
    assert inMemCount + spilledRunsCount > 0;

    // If only one spilled run, then merging is not productive regardless
    // of memory limits.

    if (inMemCount == 0 && spilledRunsCount <= 1) {
      return new MergeTask(MergeAction.NONE, 0);
    }

    // If memory is above the merge memory limit, then must spill
    // merge to create room for a merge batch.

    if (allocMemory > mergeMemoryLimit) {
      return new MergeTask(MergeAction.SPILL, 0);
    }

    // Determine additional memory needed to hold one batch from each
    // spilled run.

    // Maximum spill batches that fit into available memory.
    // Use the maximum buffer size since spill batches seem to
    // be read with almost 50% internal fragmentation.

    int memMergeLimit = (int) ((mergeMemoryLimit - allocMemory) /
                                spillBatchSize.maxBufferSize);
    memMergeLimit = Math.max(0, memMergeLimit);

    // If batches are in memory, and final merge count will exceed
    // merge limit or we need more memory to merge them all than is
    // actually available, then spill some in-memory batches.

    if (inMemCount > 0  &&  ((inMemCount + spilledRunsCount) > config.mergeLimit() || memMergeLimit < spilledRunsCount)) {
      return new MergeTask(MergeAction.SPILL, 0);
    }

    // If all batches fit in memory, then no need for a second-generation
    // merge/spill.

    memMergeLimit = Math.min(memMergeLimit, config.mergeLimit());
    int mergeRunCount = spilledRunsCount - memMergeLimit;
    if (mergeRunCount <= 0) {
      return new MergeTask(MergeAction.NONE, 0);
    }

    // We need a second generation load-merge-spill cycle
    // to reduce the number of spilled runs to a smaller set
    // that will fit in memory.

    // Merging creates another batch. Include one more run
    // in the merge to create space for the new run.

    mergeRunCount += 1;

    // Merge only as many batches as fit in memory.
    // Use all memory for this process; no need to reserve space for a
    // merge output batch. Assume worst case since we are forced to
    // accept spilled batches blind: we can't limit reading based on memory
    // limits. Subtract one to allow for the output spill batch.

    memMergeLimit = (int)(memoryLimit / spillBatchSize.maxBufferSize) - 1;
    mergeRunCount = Math.min(mergeRunCount, memMergeLimit);

    // Must merge at least 2 batches to make progress.
    // We know we have at least two because of the check done above.

    mergeRunCount = Math.max(mergeRunCount, 2);

    // Can't merge more than the merge limit.

    mergeRunCount = Math.min(mergeRunCount, config.mergeLimit());

    return new MergeTask(MergeAction.MERGE, mergeRunCount);
  }

  /**
   * Compute the number of rows that fit into a given batch data size.
   *
   * @param batchSize expected batch size, including internal fragmentation
   * @return number of rows that fit into the batch
   */

  private int rowsPerBatch(int batchSize) {
    int rowCount = batchSize / estimatedRowWidth;
    return Math.max(1, Math.min(rowCount, Character.MAX_VALUE));
  }

  public static int multiply(int byteSize, double multiplier) {
    return (int) Math.floor(byteSize * multiplier);
  }

  // Must spill if we are below the spill point (the amount of memory
  // needed to do the minimal spill.)

  public boolean isSpillNeeded(long allocatedBytes, long incomingSize) {
    return allocatedBytes + incomingSize >= bufferMemoryLimit;
  }

  public boolean hasMemoryMergeCapacity(long allocatedBytes, long neededForInMemorySort) {
    return (freeMemory(allocatedBytes) >= neededForInMemorySort);
  }

  public long freeMemory(long allocatedBytes) {
    return memoryLimit - allocatedBytes;
  }

  public long getMergeMemoryLimit() { return mergeMemoryLimit; }
  public int getSpillBatchRowCount() { return spillBatchRowCount; }
  public int getMergeBatchRowCount() { return mergeBatchRowCount; }

  // Primarily for testing

  @VisibleForTesting
  public long getMemoryLimit() { return memoryLimit; }
  @VisibleForTesting
  public int getRowWidth() { return estimatedRowWidth; }
  @VisibleForTesting
  public BatchSizeEstimate getInputBatchSize() { return inputBatchSize; }
  @VisibleForTesting
  public int getPreferredSpillBatchSize() { return preferredSpillBatchSize; }
  @VisibleForTesting
  public int getPreferredMergeBatchSize() { return preferredMergeBatchSize; }
  @VisibleForTesting
  public BatchSizeEstimate getSpillBatchSize() { return spillBatchSize; }
  @VisibleForTesting
  public BatchSizeEstimate getMergeBatchSize() { return mergeBatchSize; }
  @VisibleForTesting
  public long getBufferMemoryLimit() { return bufferMemoryLimit; }
  @VisibleForTesting
  public boolean mayOverflow() { return potentialOverflow; }
  @VisibleForTesting
  public boolean isLowMemory() { return isLowMemory; }
  @VisibleForTesting
  public boolean hasPerformanceWarning() { return performanceWarning; }
}
