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

import java.io.IOException;
import java.util.List;

import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.impl.xsort.SortMemoryManager.MergeTask;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.RecordBatch.IterOutcome;
import org.apache.drill.exec.record.RecordBatchSizer;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.VectorAccessibleUtilities;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorInitializer;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.exec.record.selection.SelectionVector4;

import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import org.apache.drill.exec.vector.ValueVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.drill.exec.record.RecordBatch.IterOutcome.EMIT;

/**
 * Implementation of the external sort which is wrapped into the Drill
 * "next" protocol by the {@link ExternalSortBatch} class.
 * <p>
 * Accepts incoming batches. Sorts each and will spill to disk as needed.
 * When all input is delivered, can either do an in-memory merge or a
 * merge from disk. If runs spilled, may have to do one or more "consolidation"
 * passes to reduce the number of runs to the level that will fit in memory.
 */

public class SortImpl {

  private static final Logger logger = LoggerFactory.getLogger(SortImpl.class);

  /**
   * Iterates over the final sorted results. Implemented differently
   * depending on whether the results are in-memory or spilled to
   * disk.
   */

  public interface SortResults {
    /**
     * Container into which results are delivered. May the
     * the original operator container, or may be a different
     * one. This is the container that should be sent
     * downstream. This is a fixed value for all returned
     * results.
     * @return
     */
    VectorContainer getContainer();
    boolean next();
    void close();
    int getBatchCount();
    int getRecordCount();
    SelectionVector2 getSv2();
    SelectionVector4 getSv4();
    void updateOutputContainer(VectorContainer container, SelectionVector4 sv4,
                               IterOutcome outcome, BatchSchema schema);
  }

  public static class EmptyResults implements SortResults {

    private final VectorContainer dest;

    public EmptyResults(VectorContainer dest) {
      dest.setRecordCount(0);
      dest.buildSchema(SelectionVectorMode.NONE);
      this.dest = dest;
    }

    @Override
    public boolean next() { return false; }

    @Override
    public void close() { }

    @Override
    public int getBatchCount() { return 0; }

    @Override
    public int getRecordCount() { return 0; }

    @Override
    public SelectionVector4 getSv4() { return null; }

    @Override
    public SelectionVector2 getSv2() { return null; }

    @Override
    public VectorContainer getContainer() { return dest; }

    @Override
    public void updateOutputContainer(VectorContainer container, SelectionVector4 sv4,
                                      IterOutcome outcome, BatchSchema schema) {

      // First output batch of current schema, populate container with ValueVectors
      if (container.getNumberOfColumns() == 0) {
        for (MaterializedField field : schema) {
          final ValueVector vv = TypeHelper.getNewVector(field, container.getAllocator());
          vv.clear();
          final ValueVector[] hyperVector = { vv };
          container.add(hyperVector, true);
        }
        container.buildSchema(SelectionVectorMode.FOUR_BYTE);
      } // since it's an empty batch no need to do anything in else

      sv4.clear();
      container.zeroVectors();
      container.setRecordCount(0);
    }
  }

  /**
   * Return results for a single input batch. No merge is needed;
   * the original (sorted) input batch is simply passed as the result.
   * Note that this version requires replacing the operator output
   * container with the batch container. (Vector ownership transfer
   * was already done when accepting the input batch.)
   */

  public static class SingleBatchResults implements SortResults {

    private boolean done;
    private final VectorContainer outputContainer;
    private final InputBatch batch;

    public SingleBatchResults(InputBatch batch, VectorContainer outputContainer) {
      this.batch = batch;
      this.outputContainer = outputContainer;
    }

    @Override
    public boolean next() {
      if (done) {
        return false;
      }

      // The following implementation is wrong. Must transfer buffers,
      // not vectors. The output container already contains vectors
      // for the output schema.

      for (VectorWrapper<?> vw : batch.getContainer()) {
        outputContainer.add(vw.getValueVector());
      }
      outputContainer.buildSchema(SelectionVectorMode.TWO_BYTE);
      outputContainer.setRecordCount(batch.getRecordCount());
      done = true;
      return true;
    }

    @Override
    public void close() {
      try {
        batch.close();
      } catch (IOException e) {
        // Should never occur for an input batch
        throw new IllegalStateException(e);
      }
    }

    @Override
    public int getBatchCount() { return 1; }

    @Override
    public int getRecordCount() { return outputContainer.getRecordCount(); }

    @Override
    public SelectionVector4 getSv4() { return null; }

    @Override
    public SelectionVector2 getSv2() { return batch.getSv2(); }

    @Override
    public VectorContainer getContainer() { return outputContainer; }

    @Override
    public void updateOutputContainer(VectorContainer container, SelectionVector4 sv4,
                                      IterOutcome outcome, BatchSchema schema) {
      if (outcome == EMIT) {
        throw new UnsupportedOperationException("SingleBatchResults for sort with SV2 is currently not supported with" +
          " EMIT outcome");
      }
      // Not used in Sort so don't need to do anything for now
    }
  }

  private final SortConfig config;
  private final SortMetrics metrics;
  private final SortMemoryManager memManager;
  private final VectorContainer outputBatch;
  private final OperatorContext context;

  /**
   * Memory allocator for this operator itself. Incoming batches are
   * transferred into this allocator. Intermediate batches used during
   * merge also reside here.
   */

  private final BufferAllocator allocator;

  private final SpilledRuns spilledRuns;

  private final BufferedBatches bufferedBatches;

  private RecordBatchSizer sizer;

  private VectorInitializer allocHelper;

  public SortImpl(OperatorContext opContext, SortConfig sortConfig,
                  SpilledRuns spilledRuns, VectorContainer batch) {
    this.context = opContext;
    outputBatch = batch;
    this.spilledRuns = spilledRuns;
    allocator = opContext.getAllocator();
    config = sortConfig;
    memManager = new SortMemoryManager(config, allocator.getLimit());
    metrics = new SortMetrics(opContext.getStats());
    bufferedBatches = new BufferedBatches(opContext);

    // Request leniency from the allocator. Leniency
    // will reduce the probability that random chance causes the allocator
    // to kill the query because of a small, spurious over-allocation.

//    long maxMem = memManager.getMemoryLimit();
//    long newMax = (long)(maxMem * 1.10);
//    allocator.setLimit(newMax);
//    logger.debug("Config: Resetting allocator to 10% safety margin: {}", newMax);
    boolean allowed = allocator.setLenient();
    logger.debug("Config: Is allocator lenient? {}", allowed);
  }

  @VisibleForTesting
  public OperatorContext opContext() { return context; }

  public void setSchema(BatchSchema schema) {
    bufferedBatches.setSchema(schema);
    spilledRuns.setSchema(schema);
  }

  public boolean forceSpill() {
    if (bufferedBatches.size() < 2) {
      return false;
    }
    spillFromMemory();
    return true;
  }

  /**
   * Process the converted incoming batch by adding it to the in-memory store
   * of data, or spilling data to disk when necessary.
   * @param incoming
   */

  public void addBatch(VectorAccessible incoming) {

    // Skip empty batches (such as the first one.)

    if (incoming.getRecordCount() == 0) {
      VectorAccessibleUtilities.clear(incoming);
      return;
    }

    // Determine actual sizes of the incoming batch before taking
    // ownership. Allows us to figure out if we need to spill first,
    // to avoid overflowing memory simply due to ownership transfer.

   analyzeIncomingBatch(incoming);

    // The heart of the external sort operator: spill to disk when
    // the in-memory generation exceeds the allowed memory limit.
    // Preemptively spill BEFORE accepting the new batch into our memory
    // pool. Although the allocator will allow us to exceed the memory limit
    // during the transfer, we immediately follow the transfer with an SV2
    // allocation that will fail if we are over the allocation limit.

    if (isSpillNeeded(sizer.getActualSize())) {
      spillFromMemory();
    }

    // Sanity check. We should now be below the buffer memory maximum.

    long startMem = allocator.getAllocatedMemory();
    bufferedBatches.add(incoming, sizer.getNetBatchSize());

    // Compute batch size, including allocation of an sv2.

    long endMem = allocator.getAllocatedMemory();
    long batchSize = endMem - startMem;

    // Update the minimum buffer space metric.

    metrics.updateInputMetrics(sizer.rowCount(), sizer.getActualSize());
    metrics.updateMemory(memManager.freeMemory(endMem));
    metrics.updatePeakBatches(bufferedBatches.size());

    // Update the size based on the actual record count, not
    // the effective count as given by the selection vector
    // (which may exclude some records due to filtering.)

    validateBatchSize(sizer.getActualSize(), batchSize);
    if (memManager.updateEstimates((int) batchSize, sizer.getNetRowWidth(), sizer.rowCount())) {

      // If estimates changed, discard the helper based on the old estimates.

      allocHelper = null;
    }
  }

  /**
   * Scan the vectors in the incoming batch to determine batch size.
   *
   * @return an analysis of the incoming batch
   */

  private void analyzeIncomingBatch(VectorAccessible incoming) {
    sizer = new RecordBatchSizer(incoming);
    sizer.applySv2();
    if (metrics.getInputBatchCount() == 0) {
      logger.debug("{}", sizer.toString());
    }
  }

  /**
   * Determine if spill is needed before receiving the new record batch.
   * Spilling is driven purely by memory availability (and an optional
   * batch limit for testing.)
   *
   * @return true if spilling is needed (and possible), false otherwise
   */

  private boolean isSpillNeeded(long incomingSize) {

    if (bufferedBatches.size() >= config.getBufferedBatchLimit()) {
      return true;
    }

    // Can't spill if less than two batches else the merge
    // can't make progress.

    final boolean spillNeeded = memManager.isSpillNeeded(allocator.getAllocatedMemory(), incomingSize);
    if (bufferedBatches.size() < 2) {

      // If we can't fit the batch into memory, then place a definite error
      // message into the log to simplify debugging.

      if (spillNeeded) {
        logger.error("Insufficient memory to merge two batches. Incoming batch size: {}, available memory: {}",
                     incomingSize, memManager.freeMemory(allocator.getAllocatedMemory()));
      }
      return false;
    }

    return spillNeeded;
  }

  private void validateBatchSize(long actualBatchSize, long memoryDelta) {
    if (actualBatchSize != memoryDelta) {
      logger.debug("Memory delta: {}, actual batch size: {}, Diff: {}",
        memoryDelta, actualBatchSize, memoryDelta - actualBatchSize);
    }
  }

  /**
   * This operator has accumulated a set of sorted incoming record batches.
   * We wish to spill some of them to disk. To do this, a "copier"
   * merges the target batches to produce a stream of new (merged) batches
   * which are then written to disk.
   * <p>
   * This method spills only half the accumulated batches
   * minimizing unnecessary disk writes. The exact count must lie between
   * the minimum and maximum spill counts.
   */

  private void spillFromMemory() {
    int startCount = bufferedBatches.size();
    List<BatchGroup> batchesToSpill = bufferedBatches.prepareSpill(config.spillFileSize());

    // Do the actual spill.

    logger.trace("Spilling {} of {} batches, allocated memory = {} bytes",
        batchesToSpill.size(), startCount,
        allocator.getAllocatedMemory());
    int spillBatchRowCount = memManager.getSpillBatchRowCount();
    spilledRuns.mergeAndSpill(batchesToSpill, spillBatchRowCount, allocHelper());
    metrics.incrSpillCount();
  }

  private VectorInitializer allocHelper() {
    if (allocHelper == null) {
      allocHelper = sizer.buildVectorInitializer();
    }
    return allocHelper;
  }

  public SortMetrics getMetrics() { return metrics; }

  public SortResults startMerge() {
    if (metrics.getInputRowCount() == 0) {
      return new EmptyResults(outputBatch);
    }

    logger.debug("Completed load phase: read {} batches, spilled {} times, total input bytes: {}",
        metrics.getInputBatchCount(), spilledRuns.size(),
        metrics.getInputBytes());

    // Do the merge of the loaded batches. The merge can be done entirely in
    // memory if the results fit; else we have to do a disk-based merge of
    // pre-sorted spilled batches. Special case the single-batch query;
    // this accelerates small, quick queries.
    //
    // Note: disabling this optimization because it turns out to be
    // quite hard to transfer a set of vectors from one place to another.

    /* if (metrics.getInputBatchCount() == 1) {
      return singleBatchResult();
    } else */ if (canUseMemoryMerge()) {
      return mergeInMemory();
    } else {
      return mergeSpilledRuns();
    }
  }
  /**
   * Input consists of a single batch. Just return that batch as
   * the output.
   * @return results iterator over the single input batch
   */

  // Disabled temporarily

  @SuppressWarnings("unused")
  private SortResults singleBatchResult() {
    List<InputBatch> batches = bufferedBatches.removeAll();
    return new SingleBatchResults(batches.get(0), outputBatch);
  }

  /**
   * All data has been read from the upstream batch. Determine if we
   * can use a fast in-memory sort, or must use a merge (which typically,
   * but not always, involves spilled batches.)
   *
   * @return whether sufficient resources exist to do an in-memory sort
   * if all batches are still in memory
   */

  private boolean canUseMemoryMerge() {
    if (spilledRuns.hasSpilled()) {
      return false; }

    // Do we have enough memory for MSorter (the in-memory sorter)?

    if (! memManager.hasMemoryMergeCapacity(allocator.getAllocatedMemory(), MSortTemplate.memoryNeeded(metrics.getInputRowCount()))) {
      return false; }

    // Make sure we don't exceed the maximum number of batches SV4 can address.

    if (bufferedBatches.size() > Character.MAX_VALUE) {
      return false; }

    // We can do an in-memory merge.

    return true;
  }

  /**
   * Perform an in-memory sort of the buffered batches. Obviously can
   * be used only for the non-spilling case.
   *
   * @return DONE if no rows, OK_NEW_SCHEMA if at least one row
   */

  private SortResults mergeInMemory() {
    logger.debug("Starting in-memory sort. Batches = {}, Records = {}, Memory = {}",
                 bufferedBatches.size(), metrics.getInputRowCount(),
                 allocator.getAllocatedMemory());

    // Note the difference between how we handle batches here and in the spill/merge
    // case. In the spill/merge case, this class decides on the batch size to send
    // downstream. However, in the in-memory case, we must pass along all batches
    // in a single SV4. Attempts to do paging will result in errors. In the memory
    // merge case, the downstream Selection Vector Remover will split the one
    // big SV4 into multiple smaller batches to send further downstream.

    // If the sort fails or is empty, clean up here. Otherwise, cleanup is done
    // by closing the resultsIterator after all results are returned downstream.

    MergeSortWrapper memoryMerge = new MergeSortWrapper(context, outputBatch);
    try {
      memoryMerge.merge(bufferedBatches.removeAll(), config.getMSortBatchSize());
    } catch (Throwable t) {
      memoryMerge.close();
      throw t;
    }
    logger.debug("Completed in-memory sort. Memory = {}",
                 allocator.getAllocatedMemory());
    return memoryMerge;
  }

  /**
   * Perform merging of (typically spilled) batches. First consolidates batches
   * as needed, then performs a final merge that is read one batch at a time
   * to deliver batches to the downstream operator.
   *
   * @return an iterator over the merged batches
   */

  private SortResults mergeSpilledRuns() {
    logger.debug("Starting consolidate phase. Batches = {}, Records = {}, Memory = {}, In-memory batches {}, spilled runs {}",
                 metrics.getInputBatchCount(), metrics.getInputRowCount(),
                 allocator.getAllocatedMemory(),
                 bufferedBatches.size(), spilledRuns.size());

    // Consolidate batches to a number that can be merged in
    // a single last pass.

    loop:
    while (true) {
      MergeTask task = memManager.consolidateBatches(
          allocator.getAllocatedMemory(),
          bufferedBatches.size(),
          spilledRuns.size());
      switch (task.action) {
      case SPILL:
        logger.debug("Consolidate: spill");
        spillFromMemory();
        break;
      case MERGE:
        logger.debug("Consolidate: merge {} batches", task.count);
        mergeRuns(task.count);
        break;
      case NONE:
        break loop;
      default:
        throw new IllegalStateException("Unexpected action: " + task.action);
      }
    }

    int mergeRowCount = memManager.getMergeBatchRowCount();
    return spilledRuns.finalMerge(bufferedBatches.removeAll(), outputBatch, mergeRowCount, allocHelper);
  }

  private void mergeRuns(int targetCount) {
    long mergeMemoryPool = memManager.getMergeMemoryLimit();
    int spillBatchRowCount = memManager.getSpillBatchRowCount();
    spilledRuns.mergeRuns(targetCount, mergeMemoryPool, spillBatchRowCount, allocHelper);
    metrics.incrMergeCount();
  }

  public void close() {
    metrics.updateWriteBytes(spilledRuns.getWriteBytes());
    RuntimeException ex = null;
    try {
      spilledRuns.close();
    } catch (RuntimeException e) {
      ex = e;
    }
    try {
      bufferedBatches.close();
    } catch (RuntimeException e) {
      ex = ex == null ? e : ex;
    }

    // Note: don't close the operator context here. It must
    // remain open until all containers are cleared, which
    // is done in the ExternalSortBatch class.

    if (ex != null) {
      throw ex;
    }
  }

  @Override
  public String toString() {
    return "SortImpl[config=" + config
        + ", outputBatch=" + outputBatch
        + ", sizer=" + sizer
        + "]";
  }
}
