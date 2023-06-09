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

import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.common.map.CaseInsensitiveMap;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.RecordBatchSizer;
import org.apache.drill.exec.vector.IntVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

import static org.apache.drill.exec.physical.impl.join.HashJoinState.INITIALIZING;

public class HashJoinMemoryCalculatorImpl implements HashJoinMemoryCalculator {

  private final double safetyFactor;
  private final double fragmentationFactor;
  private final double hashTableDoublingFactor;
  private final String hashTableCalculatorType;
  private final boolean semiJoin;

  private boolean initialized = false;
  private boolean doMemoryCalculation;

  public HashJoinMemoryCalculatorImpl(final double safetyFactor,
                                      final double fragmentationFactor,
                                      final double hashTableDoublingFactor,
                                      final String hashTableCalculatorType,
                                      boolean semiJoin) {
    this.safetyFactor = safetyFactor;
    this.fragmentationFactor = fragmentationFactor;
    this.hashTableDoublingFactor = hashTableDoublingFactor;
    this.hashTableCalculatorType = hashTableCalculatorType;
    this.semiJoin = semiJoin;
  }

  @Override
  public void initialize(boolean doMemoryCalculation) {
    Preconditions.checkState(!initialized);
    initialized = true;
    this.doMemoryCalculation = doMemoryCalculation;
  }

  @Override
  public BuildSidePartitioning next() {
    Preconditions.checkState(initialized);

    if (doMemoryCalculation) {
      final HashTableSizeCalculator hashTableSizeCalculator;

      if (hashTableCalculatorType.equals(HashTableSizeCalculatorLeanImpl.TYPE)) {
        hashTableSizeCalculator = new HashTableSizeCalculatorLeanImpl(RecordBatch.MAX_BATCH_ROW_COUNT, hashTableDoublingFactor);
      } else if (hashTableCalculatorType.equals(HashTableSizeCalculatorConservativeImpl.TYPE)) {
        hashTableSizeCalculator = new HashTableSizeCalculatorConservativeImpl(RecordBatch.MAX_BATCH_ROW_COUNT, hashTableDoublingFactor);
      } else {
        throw new IllegalArgumentException("Invalid calc type: " + hashTableCalculatorType);
      }

      return new BuildSidePartitioningImpl(
        BatchSizePredictorImpl.Factory.INSTANCE,
        hashTableSizeCalculator,
        semiJoin ? HashJoinHelperUnusedSizeImpl.INSTANCE : HashJoinHelperSizeCalculatorImpl.INSTANCE,
        fragmentationFactor, safetyFactor, semiJoin);
    } else {
      return new NoopBuildSidePartitioningImpl();
    }
  }

  @Override
  public HashJoinState getState() {
    return INITIALIZING;
  }

  public static class NoopBuildSidePartitioningImpl implements BuildSidePartitioning {
    private int initialPartitions;
    private int recordsPerPartitionBatchProbe;

    @Override
    public void initialize(boolean firstCycle,
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
                           double loadFactor) {
      this.initialPartitions = initialPartitions;
      this.recordsPerPartitionBatchProbe = recordsPerPartitionBatchProbe;
    }

    @Override
    public void setPartitionStatSet(PartitionStatSet partitionStatSet) {
      // Do nothing
    }

    @Override
    public int getNumPartitions() {
      return initialPartitions;
    }

    @Override
    public long getBuildReservedMemory() {
      return 0;
    }

    @Override
    public long getMaxReservedMemory() {
      return 0;
    }

    @Override
    public boolean shouldSpill() {
      return false;
    }

    @Override
    public String makeDebugString() {
      return "No debugging for " + NoopBuildSidePartitioningImpl.class.getCanonicalName();
    }

    @Override
    public PostBuildCalculations next() {
      return new NoopPostBuildCalculationsImpl(recordsPerPartitionBatchProbe);
    }

    @Override
    public HashJoinState getState() {
      return HashJoinState.BUILD_SIDE_PARTITIONING;
    }
  }

  /**
   * At this point we need to reserve memory for the following:
   * <ol>
   *   <li>An incoming batch</li>
   *   <li>An incomplete batch for each partition</li>
   * </ol>
   * If there is available memory we keep the batches for each partition in memory.
   * If we run out of room and need to start spilling, we need to specify which partitions
   * need to be spilled.
   * </p>
   * <h4>Life Cycle</h4>
   * <p>
   *   <ul>
   *     <li><b>Step 0:</b> Call {@link #initialize(boolean, boolean, RecordBatch, RecordBatch, Set, boolean, long, int, int, int, int, int, int, double)}.
   *     This will initialize the StateCalculate with the additional information it needs.</li>
   *     <li><b>Step 1:</b> Call {@link #getNumPartitions()} to see the number of partitions that fit in memory.</li>
   *     <li><b>Step 2:</b> Call {@link #shouldSpill()} To determine if spilling needs to occurr.</li>
   *     <li><b>Step 3:</b> Call {@link #next()} and get the next memory calculator associated with your next state.</li>
   *   </ul>
   * </p>
   */
  public static class BuildSidePartitioningImpl implements BuildSidePartitioning {
    private static final Logger logger = LoggerFactory.getLogger(BuildSidePartitioningImpl.class);

    private final BatchSizePredictor.Factory batchSizePredictorFactory;
    private final HashTableSizeCalculator hashTableSizeCalculator;
    private final HashJoinHelperSizeCalculator hashJoinHelperSizeCalculator;
    private final double fragmentationFactor;
    private final double safetyFactor;
    private final boolean semiJoin;

    private int maxBatchNumRecordsBuild;
    private int maxBatchNumRecordsProbe;
    private long memoryAvailable;
    private long maxBuildBatchSize;
    private long maxOutputBatchSize;
    private int initialPartitions;
    private int partitions;
    private int recordsPerPartitionBatchBuild;
    private int recordsPerPartitionBatchProbe;
    private int outputBatchSize;
    private Map<String, Long> keySizes;
    private boolean firstCycle;
    private boolean reserveHash;
    private double loadFactor;

    private PartitionStatSet partitionStatsSet;
    private long partitionBuildBatchSize;
    private long partitionProbeBatchSize;
    private long reservedMemory;
    private long maxReservedMemory;

    private BatchSizePredictor buildSizePredictor;
    private BatchSizePredictor probeSizePredictor;
    private boolean firstInitialized;
    private boolean initialized;

    public BuildSidePartitioningImpl(final BatchSizePredictor.Factory batchSizePredictorFactory,
                                     final HashTableSizeCalculator hashTableSizeCalculator,
                                     final HashJoinHelperSizeCalculator hashJoinHelperSizeCalculator,
                                     final double fragmentationFactor,
                                     final double safetyFactor,
                                     boolean semiJoin) {
      this.batchSizePredictorFactory = Preconditions.checkNotNull(batchSizePredictorFactory);
      this.hashTableSizeCalculator = Preconditions.checkNotNull(hashTableSizeCalculator);
      this.hashJoinHelperSizeCalculator = Preconditions.checkNotNull(hashJoinHelperSizeCalculator);
      this.fragmentationFactor = fragmentationFactor;
      this.safetyFactor = safetyFactor;
      this.semiJoin = semiJoin;
    }

    @Override
    public void initialize(boolean firstCycle,
                           boolean reserveHash,
                           RecordBatch buildBatch,
                           RecordBatch probeBatch,
                           Set<String> joinColumns,
                           boolean probeEmpty,
                           long memoryAvailable,
                           int initialPartitions,
                           int recordsPerPartitionBatchBuild,
                           int recordsPerPartitionBatchProbe,
                           int maxBatchNumRecordsBuild,
                           int maxBatchNumRecordsProbe,
                           int outputBatchSize,
                           double loadFactor) {
      Preconditions.checkNotNull(probeBatch);
      Preconditions.checkNotNull(buildBatch);
      Preconditions.checkNotNull(joinColumns);

      final BatchSizePredictor buildSizePredictor =
        batchSizePredictorFactory.create(buildBatch, fragmentationFactor, safetyFactor);
      final BatchSizePredictor probeSizePredictor =
        batchSizePredictorFactory.create(probeBatch, fragmentationFactor, safetyFactor);

      buildSizePredictor.updateStats();
      probeSizePredictor.updateStats();

      final RecordBatchSizer buildSizer = new RecordBatchSizer(buildBatch);

      final CaseInsensitiveMap<Long> keySizes = CaseInsensitiveMap.newHashMap();

      for (String joinColumn: joinColumns) {
        final RecordBatchSizer.ColumnSize columnSize = buildSizer.columns().get(joinColumn);
        keySizes.put(joinColumn, (long)columnSize.getStdNetOrNetSizePerEntry());
      }

      initialize(firstCycle,
        reserveHash,
        keySizes,
        memoryAvailable,
        initialPartitions,
        probeEmpty,
        buildSizePredictor,
        probeSizePredictor,
        recordsPerPartitionBatchBuild,
        recordsPerPartitionBatchProbe,
        maxBatchNumRecordsBuild,
        maxBatchNumRecordsProbe,
        outputBatchSize,
        loadFactor);
    }

    @VisibleForTesting
    protected void initialize(boolean firstCycle,
                              boolean reserveHash,
                              CaseInsensitiveMap<Long> keySizes,
                              long memoryAvailable,
                              int initialPartitions,
                              boolean probeEmpty,
                              BatchSizePredictor buildSizePredictor,
                              BatchSizePredictor probeSizePredictor,
                              int recordsPerPartitionBatchBuild,
                              int recordsPerPartitionBatchProbe,
                              int maxBatchNumRecordsBuild,
                              int maxBatchNumRecordsProbe,
                              int outputBatchSize,
                              double loadFactor) {
      Preconditions.checkState(!firstInitialized);
      Preconditions.checkArgument(initialPartitions >= 1);
      // If we had probe data before there should still be probe data now.
      // If we didn't have probe data before we could get some new data now.
      Preconditions.checkState(!(probeEmpty && probeSizePredictor.hadDataLastTime()));
      firstInitialized = true;

      this.loadFactor = loadFactor;
      this.firstCycle = firstCycle;
      this.reserveHash = reserveHash;
      this.keySizes = Preconditions.checkNotNull(keySizes);
      this.memoryAvailable = memoryAvailable;
      this.buildSizePredictor = buildSizePredictor;
      this.probeSizePredictor = probeSizePredictor;
      this.initialPartitions = initialPartitions;
      this.recordsPerPartitionBatchBuild = recordsPerPartitionBatchBuild;
      this.recordsPerPartitionBatchProbe = recordsPerPartitionBatchProbe;
      this.maxBatchNumRecordsBuild = maxBatchNumRecordsBuild;
      this.maxBatchNumRecordsProbe = maxBatchNumRecordsProbe;
      this.outputBatchSize = outputBatchSize;

      calculateMemoryUsage();

      logger.debug("Creating {} partitions when {} initial partitions configured.", partitions, initialPartitions);
    }

    @Override
    public void setPartitionStatSet(final PartitionStatSet partitionStatSet) {
      Preconditions.checkState(!initialized);
      initialized = true;

      partitionStatsSet = Preconditions.checkNotNull(partitionStatSet);
    }

    @Override
    public int getNumPartitions() {
      return partitions;
    }

    @Override
    public long getBuildReservedMemory() {
      Preconditions.checkState(firstInitialized);
      return reservedMemory;
    }

    @Override
    public long getMaxReservedMemory() {
      Preconditions.checkState(firstInitialized);
      return maxReservedMemory;
    }

    /**
     * Calculates the amount of memory we need to reserve while partitioning. It also
     * calculates the size of a partition batch.
     */
    private void calculateMemoryUsage()
    {
      // Adjust based on number of records
      maxBuildBatchSize = buildSizePredictor.predictBatchSize(maxBatchNumRecordsBuild, false);
      partitionBuildBatchSize = buildSizePredictor.predictBatchSize(recordsPerPartitionBatchBuild, reserveHash);

      if (probeSizePredictor.hadDataLastTime()) {
        partitionProbeBatchSize = probeSizePredictor.predictBatchSize(recordsPerPartitionBatchProbe, reserveHash);
      }

      maxOutputBatchSize = (long) (outputBatchSize * fragmentationFactor * safetyFactor);

      long probeReservedMemory = 0;

      for (partitions = initialPartitions;; partitions /= 2) {
        // The total amount of memory to reserve for incomplete batches across all partitions
        long incompletePartitionsBatchSizes = (partitions) * partitionBuildBatchSize;
        // We need to reserve all the space for incomplete batches, and the incoming batch as well as the
        // probe batch we sniffed.
        reservedMemory = incompletePartitionsBatchSizes + maxBuildBatchSize;

        if (!firstCycle) {
          // If this is NOT the first cycle the HashJoin operator owns the probe batch and we need to reserve space for it.
          reservedMemory += probeSizePredictor.getBatchSize();
        }

        if (probeSizePredictor.hadDataLastTime()) {
          // If we have probe data, use it in our memory reservation calculations.
          probeReservedMemory = PostBuildCalculationsImpl.calculateReservedMemory(
            partitions,
            probeSizePredictor.getBatchSize(),
            maxOutputBatchSize,
            partitionProbeBatchSize);

          maxReservedMemory = Math.max(reservedMemory, probeReservedMemory);
        } else {
          // If we do not have probe data, do our best effort at estimating the number of partitions without it.
          maxReservedMemory = reservedMemory;
        }

        if (!firstCycle || maxReservedMemory <= memoryAvailable) {
          // Stop the tuning loop if we are not doing auto tuning, or if we are living within our memory limit
          break;
        }

        if (partitions == 2) {
          // Can't have fewer than 2 partitions
          break;
        }
      }

      if (maxReservedMemory > memoryAvailable) {
        // We don't have enough memory we need to fail or warn

        String message = String.format("HashJoin needs to reserve %d bytes of memory but there are " +
          "only %d bytes available. Using %d num partitions with %d initial partitions. Additional info:\n" +
          "buildBatchSize = %d\n" +
          "buildNumRecords = %d\n" +
          "partitionBuildBatchSize = %d\n" +
          "recordsPerPartitionBatchBuild = %d\n" +
          "probeBatchSize = %d\n" +
          "probeNumRecords = %d\n" +
          "partitionProbeBatchSize = %d\n" +
          "recordsPerPartitionBatchProbe = %d\n",
          reservedMemory, memoryAvailable, partitions, initialPartitions,
          buildSizePredictor.getBatchSize(),
          buildSizePredictor.getNumRecords(),
          partitionBuildBatchSize,
          recordsPerPartitionBatchBuild,
          probeSizePredictor.getBatchSize(),
          probeSizePredictor.getNumRecords(),
          partitionProbeBatchSize,
          recordsPerPartitionBatchProbe);

        String phase = "Probe phase: ";

        if (reservedMemory > memoryAvailable) {
          if (probeSizePredictor.hadDataLastTime() && probeReservedMemory > memoryAvailable) {
            phase = "Build and Probe phases: ";
          } else {
            phase = "Build phase: ";
          }
        }

        message = phase + message;
        logger.warn(message);
      }
    }

    @Override
    public boolean shouldSpill() {
      Preconditions.checkState(initialized);

      long consumedMemory = reservedMemory;

      if (reserveHash) {
        // Include the hash sizes for the batch
        consumedMemory += (IntVector.VALUE_WIDTH) * partitionStatsSet.getNumInMemoryRecords();
      }

      consumedMemory += RecordBatchSizer.multiplyByFactor(partitionStatsSet.getConsumedMemory(), fragmentationFactor);
      return consumedMemory > memoryAvailable;
    }

    @Override
    public PostBuildCalculations next() {
      Preconditions.checkState(initialized);

      return new PostBuildCalculationsImpl(
        firstCycle,
        probeSizePredictor,
        memoryAvailable,
        maxOutputBatchSize,
        maxBatchNumRecordsProbe,
        recordsPerPartitionBatchProbe,
        partitionStatsSet,
        keySizes,
        hashTableSizeCalculator,
        hashJoinHelperSizeCalculator,
        fragmentationFactor,
        safetyFactor,
        loadFactor,
        reserveHash,
        semiJoin);
    }

    @Override
    public HashJoinState getState() {
      return HashJoinState.BUILD_SIDE_PARTITIONING;
    }

    @Override
    public String makeDebugString() {
      final String calcVars = String.format(
        "Build side calculator vars:\n" +
        "memoryAvailable = %s\n" +
        "maxBuildBatchSize = %s\n" +
        "maxOutputBatchSize = %s\n",
        PartitionStatSet.prettyPrintBytes(memoryAvailable),
        PartitionStatSet.prettyPrintBytes(maxBuildBatchSize),
        PartitionStatSet.prettyPrintBytes(maxOutputBatchSize));

      String partitionStatDebugString = "";

      if (partitionStatsSet != null) {
        partitionStatDebugString = partitionStatsSet.makeDebugString();
      }

      return calcVars + "\n" + partitionStatDebugString;
    }
  }

  public static class NoopPostBuildCalculationsImpl implements PostBuildCalculations {
    private final int recordsPerPartitionBatchProbe;

    public NoopPostBuildCalculationsImpl(final int recordsPerPartitionBatchProbe) {
      this.recordsPerPartitionBatchProbe = recordsPerPartitionBatchProbe;
    }

    @Override
    public void initialize(boolean hasProbeData) {
    }

    @Override
    public int getProbeRecordsPerBatch() {
      return recordsPerPartitionBatchProbe;
    }

    @Override
    public boolean shouldSpill() {
      return false;
    }

    @Override
    public HashJoinMemoryCalculator next() {
      return null;
    }

    @Override
    public HashJoinState getState() {
      return HashJoinState.POST_BUILD_CALCULATIONS;
    }

    @Override
    public String makeDebugString() {
      return "Noop " + NoopPostBuildCalculationsImpl.class.getCanonicalName() + " calculator.";
    }
  }

  /**
   * <p>
   *   In this state, we need to make sure there is enough room to spill probe side batches, if
   *   spilling is necessary. If there is not enough room, we have to evict build side partitions.
   *   If we don't have to evict build side partitions in this state, then we are done. If we do have
   *   to evict build side partitions then we have to recursively repeat the process.
   * </p>
   * <h4>Lifecycle</h4>
   * <p>
   *   <ul>
   *     <li><b>Step 1:</b> Call {@link #initialize(boolean)}. This
   *     gives the {@link HashJoinStateCalculator} additional information it needs to compute memory requirements.</li>
   *     <li><b>Step 2:</b> Call {@link #shouldSpill()}. This tells
   *     you which build side partitions need to be spilled in order to make room for probing.</li>
   *     <li><b>Step 3:</b> Call {@link #next()}. After you are done probing
   *     and partitioning the probe side, get the next calculator.</li>
   *   </ul>
   * </p>
   */
  public static class PostBuildCalculationsImpl implements PostBuildCalculations {

    private static final Logger logger = LoggerFactory.getLogger(PostBuildCalculationsImpl.class);

    public static final int MIN_RECORDS_PER_PARTITION_BATCH_PROBE = 10;

    private final boolean firstCycle;
    private final BatchSizePredictor probeSizePredictor;
    private final long memoryAvailable;
    private final long maxOutputBatchSize;
    private final int recordsPerPartitionBatchProbe;
    private final PartitionStatSet buildPartitionStatSet;
    private final Map<String, Long> keySizes;
    private final HashTableSizeCalculator hashTableSizeCalculator;
    private final HashJoinHelperSizeCalculator hashJoinHelperSizeCalculator;
    private final double fragmentationFactor;
    private final double safetyFactor;
    private final double loadFactor;
    private final boolean reserveHash;
    private final boolean semiJoin;

    private boolean initialized;
    private long consumedMemory;
    private boolean probeEmpty;
    private long partitionProbeBatchSize;
    private int computedProbeRecordsPerBatch;

    @VisibleForTesting
    public PostBuildCalculationsImpl(final boolean firstCycle,
                                     final BatchSizePredictor probeSizePredictor,
                                     final long memoryAvailable,
                                     final long maxOutputBatchSize,
                                     final int maxBatchNumRecordsProbe,
                                     final int recordsPerPartitionBatchProbe,
                                     final PartitionStatSet buildPartitionStatSet,
                                     final Map<String, Long> keySizes,
                                     final HashTableSizeCalculator hashTableSizeCalculator,
                                     final HashJoinHelperSizeCalculator hashJoinHelperSizeCalculator,
                                     final double fragmentationFactor,
                                     final double safetyFactor,
                                     final double loadFactor,
                                     final boolean reserveHash,
                                     boolean semiJoin) {
      this.firstCycle = firstCycle;
      this.probeSizePredictor = Preconditions.checkNotNull(probeSizePredictor);
      this.memoryAvailable = memoryAvailable;
      this.maxOutputBatchSize = maxOutputBatchSize;
      this.buildPartitionStatSet = Preconditions.checkNotNull(buildPartitionStatSet);
      this.keySizes = Preconditions.checkNotNull(keySizes);
      this.hashTableSizeCalculator = Preconditions.checkNotNull(hashTableSizeCalculator);
      this.hashJoinHelperSizeCalculator = Preconditions.checkNotNull(hashJoinHelperSizeCalculator);
      this.fragmentationFactor = fragmentationFactor;
      this.safetyFactor = safetyFactor;
      this.loadFactor = loadFactor;
      this.reserveHash = reserveHash;
      this.semiJoin = semiJoin;
      this.recordsPerPartitionBatchProbe = recordsPerPartitionBatchProbe;
      this.computedProbeRecordsPerBatch = recordsPerPartitionBatchProbe;
    }

    @Override
    public void initialize(boolean probeEmpty) {
      Preconditions.checkState(!initialized);
      // If we had probe data before there should still be probe data now.
      // If we didn't have probe data before we could get some new data now.
      Preconditions.checkState(!(probeEmpty && probeSizePredictor.hadDataLastTime()));
      initialized = true;
      this.probeEmpty = probeEmpty;

      if (probeEmpty) {
        // We know there is no probe side data, so we don't need to calculate anything.
        return;
      }

      // We need to compute sizes of probe side data.
      if (!probeSizePredictor.hadDataLastTime()) {
        probeSizePredictor.updateStats();
      }

      partitionProbeBatchSize = probeSizePredictor.predictBatchSize(recordsPerPartitionBatchProbe, reserveHash);

      long worstCaseProbeMemory = calculateReservedMemory(
        buildPartitionStatSet.getSize(),
        getIncomingProbeBatchReservedSpace(),
        maxOutputBatchSize,
        partitionProbeBatchSize);

      if (worstCaseProbeMemory > memoryAvailable) {
        // We don't have enough memory for the probe data if all the partitions are spilled, we need to adjust the records
        // per probe partition batch in order to make this work.

        computedProbeRecordsPerBatch = computeProbeRecordsPerBatch(memoryAvailable,
          buildPartitionStatSet.getSize(),
          recordsPerPartitionBatchProbe,
          MIN_RECORDS_PER_PARTITION_BATCH_PROBE,
          getIncomingProbeBatchReservedSpace(),
          maxOutputBatchSize,
          partitionProbeBatchSize);

        partitionProbeBatchSize = probeSizePredictor.predictBatchSize(computedProbeRecordsPerBatch, reserveHash);
      }
    }

    @Override
    public int getProbeRecordsPerBatch() {
      Preconditions.checkState(initialized);
      return computedProbeRecordsPerBatch;
    }

    public long getIncomingProbeBatchReservedSpace() {
      Preconditions.checkState(initialized);

      if (firstCycle) {
        return 0;
      } else {
        return probeSizePredictor.getBatchSize();
      }
    }

    @VisibleForTesting
    public long getPartitionProbeBatchSize() {
      return partitionProbeBatchSize;
    }

    public long getConsumedMemory() {
      Preconditions.checkState(initialized);
      return consumedMemory;
    }

    public static int computeProbeRecordsPerBatch(final long memoryAvailable,
                                                  final int numPartitions,
                                                  final int defaultProbeRecordsPerBatch,
                                                  final int minProbeRecordsPerBatch,
                                                  final long maxProbeBatchSize,
                                                  final long maxOutputBatchSize,
                                                  final long defaultPartitionProbeBatchSize) {
      long memoryForPartitionBatches = memoryAvailable - maxProbeBatchSize - maxOutputBatchSize;

      if (memoryForPartitionBatches < 0) {
        // We just don't have enough memory. We should do our best though by using the minimum batch size.
        logger.warn("Not enough memory for probing:\n" +
          "Memory available: {}\n" +
          "Max probe batch size: {}\n" +
          "Max output batch size: {}",
          memoryAvailable,
          maxProbeBatchSize,
          maxOutputBatchSize);
        return minProbeRecordsPerBatch;
      }

      long memoryForPartitionBatch = (memoryForPartitionBatches + numPartitions - 1) / numPartitions;
      long scaleFactor = (defaultPartitionProbeBatchSize + memoryForPartitionBatch - 1) / memoryForPartitionBatch;
      return Math.max((int) (defaultProbeRecordsPerBatch / scaleFactor), minProbeRecordsPerBatch);
    }

    public static long calculateReservedMemory(final int numSpilledPartitions,
                                               final long maxProbeBatchSize,
                                               final long maxOutputBatchSize,
                                               final long partitionProbeBatchSize) {
      // We need to have enough space for the incoming batch, as well as a batch for each probe side
      // partition that is being spilled. And enough space for the output batch
      return maxProbeBatchSize
        + maxOutputBatchSize
        + partitionProbeBatchSize * numSpilledPartitions;
    }

    @Override
    public boolean shouldSpill() {
      Preconditions.checkState(initialized);

      if (probeEmpty) {
        // If the probe is empty, we should not trigger any spills.
        return false;
      }

      long reservedMemory = calculateReservedMemory(
        buildPartitionStatSet.getNumSpilledPartitions(),
        getIncomingProbeBatchReservedSpace(),
        maxOutputBatchSize,
        partitionProbeBatchSize);

      // We are consuming our reserved memory plus the amount of memory for each build side
      // batch and the size of the hashtables and the size of the join helpers
      consumedMemory = reservedMemory + RecordBatchSizer.multiplyByFactor(buildPartitionStatSet.getConsumedMemory(), fragmentationFactor);

      // Handle early completion conditions
      if (buildPartitionStatSet.allSpilled()) {
        // All build side partitions are spilled so our memory calculation is complete
        return false;
      }

      for (int partitionIndex: buildPartitionStatSet.getInMemoryPartitions()) {
        final PartitionStat partitionStat = buildPartitionStatSet.get(partitionIndex);

        if (partitionStat.getNumInMemoryRecords() == 0) {
          // TODO Hash hoin still allocates empty hash tables and hash join helpers. We should fix hash join
          // not to allocate empty tables and helpers.
          continue;
        }

        long hashTableSize = hashTableSizeCalculator.calculateSize(partitionStat, keySizes, loadFactor, safetyFactor, fragmentationFactor);
        long hashJoinHelperSize = hashJoinHelperSizeCalculator.calculateSize(partitionStat, fragmentationFactor);

        consumedMemory += hashTableSize + hashJoinHelperSize;
      }

      return consumedMemory > memoryAvailable;
    }

    @Override
    public HashJoinMemoryCalculator next() {
      Preconditions.checkState(initialized);

      if (buildPartitionStatSet.noneSpilled()) {
        // If none of our partitions were spilled then we were able to probe everything and we are done
        return null;
      }

      // Some of our probe side batches were spilled so we have to recursively process the partitions.
      return new HashJoinMemoryCalculatorImpl(
        safetyFactor, fragmentationFactor, hashTableSizeCalculator.getDoublingFactor(), hashTableSizeCalculator.getType(), semiJoin);
    }

    @Override
    public HashJoinState getState() {
      return HashJoinState.POST_BUILD_CALCULATIONS;
    }

    @Override
    public String makeDebugString() {
      Preconditions.checkState(initialized);

      String calcVars = String.format(
        "Mem calc stats:\n" +
        "memoryLimit = %s\n" +
        "consumedMemory = %s\n" +
        "maxIncomingProbeBatchReservedSpace = %s\n" +
        "maxOutputBatchSize = %s\n",
        PartitionStatSet.prettyPrintBytes(memoryAvailable),
        PartitionStatSet.prettyPrintBytes(consumedMemory),
        PartitionStatSet.prettyPrintBytes(getIncomingProbeBatchReservedSpace()),
        PartitionStatSet.prettyPrintBytes(maxOutputBatchSize));

      StringBuilder hashJoinHelperSb = new StringBuilder("Partition Hash Join Helpers\n");
      StringBuilder hashTableSb = new StringBuilder("Partition Hash Tables\n");

      for (int partitionIndex: buildPartitionStatSet.getInMemoryPartitions()) {
        final PartitionStat partitionStat = buildPartitionStatSet.get(partitionIndex);
        final String partitionPrefix = partitionIndex + ": ";

        hashJoinHelperSb.append(partitionPrefix);
        hashTableSb.append(partitionPrefix);

        if (partitionStat.getNumInMemoryBatches() == 0) {
          hashJoinHelperSb.append("Empty");
          hashTableSb.append("Empty");
        } else {
          long hashJoinHelperSize = hashJoinHelperSizeCalculator.calculateSize(partitionStat, fragmentationFactor);
          long hashTableSize = hashTableSizeCalculator.calculateSize(partitionStat, keySizes, loadFactor, safetyFactor, fragmentationFactor);

          hashJoinHelperSb.append(PartitionStatSet.prettyPrintBytes(hashJoinHelperSize));
          hashTableSb.append(PartitionStatSet.prettyPrintBytes(hashTableSize));
        }

        hashJoinHelperSb.append("\n");
        hashTableSb.append("\n");
      }

      return calcVars
        + "\n" + buildPartitionStatSet.makeDebugString()
        + "\n" + hashJoinHelperSb.toString()
        + "\n" + hashTableSb.toString();
    }
  }
}
