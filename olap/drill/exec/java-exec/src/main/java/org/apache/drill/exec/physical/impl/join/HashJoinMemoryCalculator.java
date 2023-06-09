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
package org.apache.drill.exec.physical.impl.join;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.drill.exec.record.RecordBatch;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * This class is responsible for managing the memory calculations for the HashJoin operator.
 * Since the HashJoin operator has different phases of execution, this class needs to perform
 * different memory calculations at each phase. The phases of execution have been broken down
 * into an explicit state machine diagram below. What ocurrs in each state is described in
 * the documentation of the {@link HashJoinState} class below. <b>Note:</b> the transition from Probing
 * and Partitioning back to Build Side Partitioning. This happens when we had to spill probe side
 * partitions and we needed to recursively process spilled partitions. This recursion is
 * described in more detail in the example below.
 * </p>
 * <p>
 *
 *                                  +--------------+ <-------+
 *                                  |  Build Side  |         |
 *                                  |  Partitioning|         |
 *                                  |              |         |
 *                                  +------+-------+         |
 *                                         |                 |
 *                                         |                 |
 *                                         v                 |
 *                                  +--------------+         |
 *                                  |Probing and   |         |
 *                                  |Partitioning  |         |
 *                                  |              |         |
 *                                  +--------------+         |
 *                                          |                |
 *                                          +----------------+
 *                                          |
 *                                          v
 *                                        Done
 * </p>
 * <p>
 * An overview of how these states interact can be summarized with the following example.<br/><br/>
 *
 * Consider the case where we have 4 partition configured initially.<br/><br/>
 *
 * <ol>
 *   <li>We first start consuming build side batches and putting their records into one of 4 build side partitions.</li>
 *   <li>Once we run out of memory we start spilling build side partition one by one</li>
 *   <li>We keep partitioning build side batches until all the build side batches are consumed.</li>
 *   <li>After we have consumed the build side we prepare to probe by building hashtables for the partitions
 *   we have in memory. If we don't have enough room for all the hashtables in memory we spill build side
 *   partitions until we do have enough room.</li>
 *   <li>We now start processing the probe side. For each probe record we determine its build partition. If
 *   the build partition is in memory we do the join for the record and emit it. If the build partition is
 *   not in memory we spill the probe record. We continue this process until all the probe side records are consumed.</li>
 *   <li>If we didn't spill any probe side partitions because all the build side partition were in memory, our join
 *   operation is done. If we did spill probe side partitions we have to recursively repeat this whole process for each
 *   spilled probe and build side partition pair.</li>
 * </ol>
 * </p>
*/
public interface HashJoinMemoryCalculator extends HashJoinStateCalculator<HashJoinMemoryCalculator.BuildSidePartitioning> {
  void initialize(boolean doMemoryCalc);

  /**
   * The interface representing the {@link HashJoinStateCalculator} corresponding to the
   * {@link HashJoinState#BUILD_SIDE_PARTITIONING} state.
   *
   * <h4>Invariants</h4>
   * <ul>
   *   <li>
   *     This calculator will only be used when there is build side data. If there is no build side data, the caller
   *     should not invoke this calculator.
   *   </li>
   * </ul>
   */
  interface BuildSidePartitioning extends HashJoinStateCalculator<PostBuildCalculations> {
    void initialize(boolean firstCycle,
                    boolean reserveHash,
                    RecordBatch buildSideBatch,
                    RecordBatch probeSideBatch,
                    Set<String> joinColumns,
                    boolean probeEmpty,
                    long memoryAvailable,
                    int initialPartitions,
                    int recordsPerPartitionBatchBuild,
                    int recordsPerPartitionBatchProbe,
                    int maxBatchNumRecordsBuild,
                    int maxBatchNumRecordsProbe,
                    int outputBatchSize,
                    double loadFactor);

    void setPartitionStatSet(PartitionStatSet partitionStatSet);

    int getNumPartitions();

    long getBuildReservedMemory();

    long getMaxReservedMemory();

    boolean shouldSpill();

    String makeDebugString();
  }

  /**
   * The interface representing the {@link HashJoinStateCalculator} corresponding to the
   * {@link HashJoinState#POST_BUILD_CALCULATIONS} state.
   */
  interface PostBuildCalculations extends HashJoinStateCalculator<HashJoinMemoryCalculator> {
    /**
     * Initializes the calculator with additional information needed.
     * @param probeEmty True if the probe is empty. False otherwise.
     */
    void initialize(boolean probeEmty);

    int getProbeRecordsPerBatch();

    boolean shouldSpill();

    String makeDebugString();
  }

  interface PartitionStat {
    List<BatchStat> getInMemoryBatches();

    int getNumInMemoryBatches();

    boolean isSpilled();

    long getNumInMemoryRecords();

    long getInMemorySize();
  }

  /**
   * This class represents the memory size statistics for an entire set of partitions.
   */
  class PartitionStatSet {

    private final PartitionStat[] partitionStats;

    public PartitionStatSet(final PartitionStat... partitionStats) {
      this.partitionStats = Preconditions.checkNotNull(partitionStats);

      for (PartitionStat partitionStat: partitionStats) {
        Preconditions.checkNotNull(partitionStat);
      }
    }

    public PartitionStat get(int partitionIndex) {
      return partitionStats[partitionIndex];
    }

    public int getSize() {
      return partitionStats.length;
    }

    // Somewhat inefficient but not a big deal since we don't deal with that many partitions
    public long getNumInMemoryRecords() {
      long numRecords = 0L;

      for (final PartitionStat partitionStat: partitionStats) {
        numRecords += partitionStat.getNumInMemoryRecords();
      }

      return numRecords;
    }

    public int getNumInMemoryBatches() {
      int numBatches = 0;

      for (final PartitionStat partitionStat: partitionStats) {
        numBatches += partitionStat.getNumInMemoryBatches();
      }

      return numBatches;
    }

    // Somewhat inefficient but not a big deal since we don't deal with that many partitions
    public long getConsumedMemory() {
      long consumedMemory = 0L;

      for (final PartitionStat partitionStat: partitionStats) {
        consumedMemory += partitionStat.getInMemorySize();
      }

      return consumedMemory;
    }

    public List<Integer> getSpilledPartitions() {
      return getPartitions(true);
    }

    public List<Integer> getInMemoryPartitions() {
      return getPartitions(false);
    }

    public List<Integer> getPartitions(boolean spilled) {
      List<Integer> partitionIndices = Lists.newArrayList();

      for (int partitionIndex = 0; partitionIndex < partitionStats.length; partitionIndex++) {
        final PartitionStat partitionStat = partitionStats[partitionIndex];

        if (partitionStat.isSpilled() == spilled) {
          partitionIndices.add(partitionIndex);
        }
      }

      return partitionIndices;
    }

    public int getNumInMemoryPartitions() {
      return getInMemoryPartitions().size();
    }

    public int getNumSpilledPartitions() {
      return getSpilledPartitions().size();
    }

    public boolean allSpilled() {
      return getSize() == getNumSpilledPartitions();
    }

    public boolean noneSpilled() {
      return getSize() == getNumInMemoryPartitions();
    }

    public String makeDebugString() {
      final StringBuilder sizeSb = new StringBuilder("Partition Sizes:\n");
      final StringBuilder batchCountSb = new StringBuilder("Partition Batch Counts:\n");
      final StringBuilder recordCountSb = new StringBuilder("Partition Record Counts:\n");

      for (int partitionIndex = 0; partitionIndex < partitionStats.length; partitionIndex++) {
        final PartitionStat partitionStat = partitionStats[partitionIndex];
        final String partitionPrefix = partitionIndex + ": ";

        sizeSb.append(partitionPrefix);
        batchCountSb.append(partitionPrefix);
        recordCountSb.append(partitionPrefix);

        if (partitionStat.isSpilled()) {
          sizeSb.append("Spilled");
          batchCountSb.append("Spilled");
          recordCountSb.append("Spilled");
        } else if (partitionStat.getNumInMemoryRecords() == 0) {
          sizeSb.append("Empty");
          batchCountSb.append("Empty");
          recordCountSb.append("Empty");
        } else {
          sizeSb.append(prettyPrintBytes(partitionStat.getInMemorySize()));
          batchCountSb.append(partitionStat.getNumInMemoryBatches());
          recordCountSb.append(partitionStat.getNumInMemoryRecords());
        }

        sizeSb.append("\n");
        batchCountSb.append("\n");
        recordCountSb.append("\n");
      }

      return sizeSb.toString() + "\n" + batchCountSb.toString() + "\n" + recordCountSb.toString();
    }

    public static String prettyPrintBytes(long byteCount) {
      return String.format("%d (%s)", byteCount, FileUtils.byteCountToDisplaySize(byteCount));
    }
  }

  class BatchStat {
    private int numRecords;
    private long batchSize;

    public BatchStat(int numRecords, long batchSize) {
      this.numRecords = numRecords;
      this.batchSize = batchSize;
    }

    public long getNumRecords()
    {
      return numRecords;
    }

    public long getBatchSize()
    {
      return batchSize;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }

      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      BatchStat batchStat = (BatchStat) o;

      if (numRecords != batchStat.numRecords) {
        return false;
      }

      return batchSize == batchStat.batchSize;
    }

    @Override
    public int hashCode() {
      int result = numRecords;
      result = 31 * result + (int) (batchSize ^ (batchSize >>> 32));
      return result;
    }
  }
}
