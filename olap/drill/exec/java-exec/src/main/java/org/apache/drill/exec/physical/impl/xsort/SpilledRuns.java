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
import java.util.LinkedList;
import java.util.List;

import org.apache.drill.common.AutoCloseables;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.impl.spill.SpillSet;
import org.apache.drill.exec.physical.impl.xsort.SortImpl.SortResults;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorInitializer;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the set of spilled batches, including methods to spill and/or
 * merge a set of batches to produce a new spill file.
 */
public class SpilledRuns {
  private static final Logger logger = LoggerFactory.getLogger(SpilledRuns.class);

  /**
   * Manages the set of spill directories and files.
   */
  private final SpillSet spillSet;
  private final LinkedList<SpilledRun> spilledRuns = Lists.newLinkedList();

  /**
   * Manages the copier used to merge a collection of batches into
   * a new set of batches.
   */
  private final PriorityQueueCopierWrapper copierHolder;
  private BatchSchema schema;

  private final OperatorContext context;

  public SpilledRuns(OperatorContext opContext, SpillSet spillSet, PriorityQueueCopierWrapper copier) {
    this.context = opContext;
    this.spillSet = spillSet;
    copierHolder = copier;
  }

  public void setSchema(BatchSchema schema) {
    this.schema = schema;
    for (BatchGroup b : spilledRuns) {
      b.setSchema(schema);
    }
    copierHolder.close();
  }

  public int size() { return spilledRuns.size(); }
  public boolean hasSpilled() { return spillSet.hasSpilled(); }
  public long getWriteBytes() { return spillSet.getWriteBytes(); }

  public static List<BatchGroup> prepareSpillBatches(LinkedList<? extends BatchGroup> source, int spillCount) {
    List<BatchGroup> batchesToSpill = Lists.newArrayList();
    spillCount = Math.min(source.size(), spillCount);
    assert spillCount > 0 : "Spill count to mergeAndSpill must not be zero";
    for (int i = 0; i < spillCount; i++) {
      batchesToSpill.add(source.pollFirst());
    }
    return batchesToSpill;
  }

  public void mergeAndSpill(List<BatchGroup> batchesToSpill, int spillBatchRowCount, VectorInitializer allocHelper) {
    spilledRuns.add(safeMergeAndSpill(batchesToSpill, spillBatchRowCount, allocHelper));
    logger.trace("Completed spill: memory = {}",
        context.getAllocator().getAllocatedMemory());
  }

  public void mergeRuns(int targetCount, long mergeMemoryPool,
                  int spillBatchRowCount, VectorInitializer allocHelper) {

    long allocated = context.getAllocator().getAllocatedMemory();
    mergeMemoryPool -= context.getAllocator().getAllocatedMemory();
    logger.trace("Merging {} on-disk runs, alloc. memory = {}, avail. memory = {}",
        targetCount, allocated, mergeMemoryPool);

    // Determine the number of runs to merge. The count should be the
    // target count. However, to prevent possible memory overrun, we
    // double-check with actual spill batch size and only spill as much
    // as fits in the merge memory pool.

    int mergeCount = 0;
    long mergeSize = 0;
    for (SpilledRun run : spilledRuns) {
      long batchSize = run.getBatchSize();
      if (mergeSize + batchSize > mergeMemoryPool) {
        break;
      }
      mergeSize += batchSize;
      mergeCount++;
      if (mergeCount == targetCount) {
        break;
      }
    }

    // Must always spill at least 2, even if this creates an over-size
    // spill file. But, if this is a final consolidation, we may have only
    // a single batch.

    mergeCount = Math.max(mergeCount, 2);
    mergeCount = Math.min(mergeCount, spilledRuns.size());

    // Do the actual spill.

    List<BatchGroup> batchesToSpill = prepareSpillBatches(spilledRuns, mergeCount);
    mergeAndSpill(batchesToSpill, spillBatchRowCount, allocHelper);
  }

  private SpilledRun safeMergeAndSpill(List<? extends BatchGroup> batchesToSpill, int spillBatchRowCount, VectorInitializer allocHelper) {
    try {
      return doMergeAndSpill(batchesToSpill, spillBatchRowCount, allocHelper);
    }
    // If error is a User Exception, just use as is.

    catch (UserException ue) { throw ue; }
    catch (Throwable ex) {
      throw UserException.resourceError(ex)
            .message("External Sort encountered an error while spilling to disk")
            .build(logger);
    }
  }

  private SpilledRun doMergeAndSpill(List<? extends BatchGroup> batchesToSpill,
                        int spillBatchRowCount, VectorInitializer allocHelper) throws Throwable {

    // Merge the selected set of matches and write them to the
    // spill file. After each write, we release the memory associated
    // with the just-written batch.

    String outputFile = spillSet.getNextSpillFile();
    SpilledRun newGroup = null;
    VectorContainer dest = new VectorContainer();
    try (AutoCloseable ignored = AutoCloseables.all(batchesToSpill);
         PriorityQueueCopierWrapper.BatchMerger merger = copierHolder.startMerge(schema, batchesToSpill,
                                         dest, spillBatchRowCount, allocHelper)) {
      newGroup = new SpilledRun(spillSet, outputFile, context.getAllocator());
      logger.trace("Spilling {} batches, into spill batches of {} rows, to {}",
          batchesToSpill.size(), spillBatchRowCount, outputFile);

      // The copier will merge records from the buffered batches into
      // the outputContainer up to targetRecordCount number of rows.
      // The actual count may be less if fewer records are available.

      while (merger.next()) {

        // Add a new batch of records (given by merger.getOutput()) to the spill
        // file.
        //
        // note that addBatch also clears the merger's output container

        newGroup.spillBatch(dest);
      }
      context.injectChecked(ExternalSortBatch.INTERRUPTION_WHILE_SPILLING, IOException.class);
      newGroup.closeWriter();
      logger.trace("Spilled {} output batches, each of {} bytes, {} records, to {}",
                   merger.getBatchCount(), merger.getEstBatchSize(),
                   spillBatchRowCount, outputFile);
      newGroup.setBatchSize(merger.getEstBatchSize());
      return newGroup;
    } catch (Throwable e) {
      // we only need to clean up newGroup if spill failed
      try {
        if (newGroup != null) {
          AutoCloseables.close(e, newGroup);
        }
      } catch (Throwable t) { /* close() may hit the same IO issue; just ignore */ }

      throw e;
    }
  }

  public SortResults finalMerge(List<? extends BatchGroup> bufferedBatches,
                    VectorContainer container, int mergeRowCount, VectorInitializer allocHelper) {
    List<BatchGroup> allBatches = new LinkedList<>();
    allBatches.addAll(bufferedBatches);
    bufferedBatches.clear();
    allBatches.addAll(spilledRuns);
    spilledRuns.clear();
    logger.debug("Starting merge phase. Runs = {}, Alloc. memory = {}",
        allBatches.size(), context.getAllocator().getAllocatedMemory());
    return copierHolder.startMerge(schema, allBatches, container, mergeRowCount, allocHelper);
  }

  public void close() {
    if (spillSet.getWriteBytes() > 0) {
      logger.debug("End of sort. Total write bytes: {}, Total read bytes: {}",
                   spillSet.getWriteBytes(), spillSet.getWriteBytes());
    }
    AutoCloseables.closeWithUserException(() -> BatchGroup.closeAll(spilledRuns),
        copierHolder::close, spillSet::close);
  }
}
