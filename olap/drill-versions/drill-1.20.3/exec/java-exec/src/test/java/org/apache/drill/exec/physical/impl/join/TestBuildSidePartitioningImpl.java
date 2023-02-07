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
import org.apache.drill.common.map.CaseInsensitiveMap;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;

public class TestBuildSidePartitioningImpl extends BaseTest {
  @Test
  public void testSimpleReserveMemoryCalculationNoHashFirstCycle() {
    testSimpleReserveMemoryCalculationNoHashHelper(true);
  }

  @Test
  public void testSimpleReserveMemoryCalculationNoHashNotFirstCycle() {
    testSimpleReserveMemoryCalculationNoHashHelper(false);
  }

  private void testSimpleReserveMemoryCalculationNoHashHelper(final boolean firstCycle) {
    final int maxBatchNumRecords = 20;
    final double fragmentationFactor = 2.0;
    final double safetyFactor = 1.5;

    final HashJoinMemoryCalculatorImpl.BuildSidePartitioningImpl calc =
      new HashJoinMemoryCalculatorImpl.BuildSidePartitioningImpl(
        BatchSizePredictorImpl.Factory.INSTANCE,
        new HashTableSizeCalculatorConservativeImpl(RecordBatch.MAX_BATCH_ROW_COUNT, HashTableSizeCalculatorConservativeImpl.HASHTABLE_DOUBLING_FACTOR),
        HashJoinHelperSizeCalculatorImpl.INSTANCE,
        fragmentationFactor,
        safetyFactor, false);

    final CaseInsensitiveMap<Long> keySizes = CaseInsensitiveMap.newHashMap();
    final long accountedProbeBatchSize = firstCycle? 0: 10;

    calc.initialize(firstCycle,
      false,
      keySizes,
      190 + accountedProbeBatchSize,
      2,
      false,
      new MockBatchSizePredictor(20, 20, fragmentationFactor, safetyFactor),
      new MockBatchSizePredictor(10, 10, fragmentationFactor, safetyFactor),
      10,
      5,
      maxBatchNumRecords,
      maxBatchNumRecords,
      16000,
      .75);

    final HashJoinMemoryCalculator.PartitionStatSet partitionStatSet =
      new HashJoinMemoryCalculator.PartitionStatSet(new PartitionStatImpl(), new PartitionStatImpl());
    calc.setPartitionStatSet(partitionStatSet);

    long expectedReservedMemory = 60 // Max incoming batch size
      + 2 * 30 // build side batch for each spilled partition
      + accountedProbeBatchSize; // Max incoming probe batch size
    long actualReservedMemory = calc.getBuildReservedMemory();

    Assert.assertEquals(expectedReservedMemory, actualReservedMemory);
    Assert.assertEquals(2, calc.getNumPartitions());
  }

  @Test
  public void testSimpleReserveMemoryCalculationHash() {
    final int maxBatchNumRecords = 20;
    final double fragmentationFactor = 2.0;
    final double safetyFactor = 1.5;

    final HashJoinMemoryCalculatorImpl.BuildSidePartitioningImpl calc =
      new HashJoinMemoryCalculatorImpl.BuildSidePartitioningImpl(
        BatchSizePredictorImpl.Factory.INSTANCE,
        new HashTableSizeCalculatorConservativeImpl(RecordBatch.MAX_BATCH_ROW_COUNT, HashTableSizeCalculatorConservativeImpl.HASHTABLE_DOUBLING_FACTOR),
        HashJoinHelperSizeCalculatorImpl.INSTANCE,
        fragmentationFactor,
        safetyFactor, false);

    final CaseInsensitiveMap<Long> keySizes = CaseInsensitiveMap.newHashMap();

    calc.initialize(false,
      true,
      keySizes,
      350,
      2,
      false,
      new MockBatchSizePredictor(20, 20, fragmentationFactor, safetyFactor),
      new MockBatchSizePredictor(10, 10, fragmentationFactor, safetyFactor),
      10,
      5,
      maxBatchNumRecords,
      maxBatchNumRecords,
      16000,
      .75);

    final HashJoinMemoryCalculator.PartitionStatSet partitionStatSet =
      new HashJoinMemoryCalculator.PartitionStatSet(new PartitionStatImpl(), new PartitionStatImpl());
    calc.setPartitionStatSet(partitionStatSet);

    long expectedReservedMemory = 60 // Max incoming batch size
      + 2 * (/* data size for batch */ 30 + /* Space reserved for hash value vector */ 10 * 4 * 2) // build side batch for each spilled partition
      + 10; // Max incoming probe batch size
    long actualReservedMemory = calc.getBuildReservedMemory();

    Assert.assertEquals(expectedReservedMemory, actualReservedMemory);
    Assert.assertEquals(2, calc.getNumPartitions());
  }

  @Test
  public void testAdjustInitialPartitions() {
    final int maxBatchNumRecords = 20;
    final double fragmentationFactor = 2.0;
    final double safetyFactor = 1.5;

    final HashJoinMemoryCalculatorImpl.BuildSidePartitioningImpl calc =
      new HashJoinMemoryCalculatorImpl.BuildSidePartitioningImpl(
        BatchSizePredictorImpl.Factory.INSTANCE,
        new HashTableSizeCalculatorConservativeImpl(RecordBatch.MAX_BATCH_ROW_COUNT, HashTableSizeCalculatorConservativeImpl.HASHTABLE_DOUBLING_FACTOR),
        HashJoinHelperSizeCalculatorImpl.INSTANCE,
        fragmentationFactor,
        safetyFactor, false);

    final CaseInsensitiveMap<Long> keySizes = CaseInsensitiveMap.newHashMap();

    calc.initialize(
      true,
      false,
      keySizes,
      200,
      4,
      false,
      new MockBatchSizePredictor(20, 20, fragmentationFactor, safetyFactor),
      new MockBatchSizePredictor(10, 10, fragmentationFactor, safetyFactor),
      10,
      5,
      maxBatchNumRecords,
      maxBatchNumRecords,
      16000,
      .75);

    final HashJoinMemoryCalculator.PartitionStatSet partitionStatSet =
      new HashJoinMemoryCalculator.PartitionStatSet(new PartitionStatImpl(), new PartitionStatImpl(),
        new PartitionStatImpl(), new PartitionStatImpl());
    calc.setPartitionStatSet(partitionStatSet);

    long expectedReservedMemory = 60 // Max incoming batch size
      + 2 * 30; // build side batch for each spilled partition
    long actualReservedMemory = calc.getBuildReservedMemory();

    Assert.assertEquals(expectedReservedMemory, actualReservedMemory);
    Assert.assertEquals(2, calc.getNumPartitions());
  }

  @Test
  public void testDontAdjustInitialPartitions() {
    final int maxBatchNumRecords = 20;
    final double fragmentationFactor = 2.0;
    final double safetyFactor = 1.5;

    final HashJoinMemoryCalculatorImpl.BuildSidePartitioningImpl calc =
      new HashJoinMemoryCalculatorImpl.BuildSidePartitioningImpl(
        BatchSizePredictorImpl.Factory.INSTANCE,
        new HashTableSizeCalculatorConservativeImpl(RecordBatch.MAX_BATCH_ROW_COUNT, HashTableSizeCalculatorConservativeImpl.HASHTABLE_DOUBLING_FACTOR),
        HashJoinHelperSizeCalculatorImpl.INSTANCE,
        fragmentationFactor,
        safetyFactor, false);

    final CaseInsensitiveMap<Long> keySizes = CaseInsensitiveMap.newHashMap();

    calc.initialize(
      false,
      false,
      keySizes,
      200,
      4,
      false,
      new MockBatchSizePredictor(20, 20, fragmentationFactor, safetyFactor),
      new MockBatchSizePredictor(10, 10, fragmentationFactor, safetyFactor),
      10,
      5,
      maxBatchNumRecords,
      maxBatchNumRecords,
      16000,
      .75);

    final HashJoinMemoryCalculator.PartitionStatSet partitionStatSet =
      new HashJoinMemoryCalculator.PartitionStatSet(new PartitionStatImpl(), new PartitionStatImpl(),
        new PartitionStatImpl(), new PartitionStatImpl());
    calc.setPartitionStatSet(partitionStatSet);

    long expectedReservedMemory = 60 // Max incoming batch size
      + 4 * 30 // build side batch for each spilled partition
      + 10; // Max incoming probe batch size
    long actualReservedMemory = calc.getBuildReservedMemory();

    Assert.assertEquals(expectedReservedMemory, actualReservedMemory);
    Assert.assertEquals(4, calc.getNumPartitions());
  }

  @Test(expected = IllegalStateException.class)
  public void testHasDataProbeEmpty() {
    final int maxBatchNumRecords = 20;
    final double fragmentationFactor = 2.0;
    final double safetyFactor = 1.5;

    final HashJoinMemoryCalculatorImpl.BuildSidePartitioningImpl calc =
      new HashJoinMemoryCalculatorImpl.BuildSidePartitioningImpl(
        BatchSizePredictorImpl.Factory.INSTANCE,
        new HashTableSizeCalculatorConservativeImpl(RecordBatch.MAX_BATCH_ROW_COUNT, HashTableSizeCalculatorConservativeImpl.HASHTABLE_DOUBLING_FACTOR),
        HashJoinHelperSizeCalculatorImpl.INSTANCE,
        fragmentationFactor,
        safetyFactor, false);

    final CaseInsensitiveMap<Long> keySizes = CaseInsensitiveMap.newHashMap();

    calc.initialize(
      true,
      false,
      keySizes,
      240,
      4,
      true,
      new MockBatchSizePredictor(20, 20, fragmentationFactor, safetyFactor),
      new MockBatchSizePredictor(10, 10, fragmentationFactor, safetyFactor),
      10,
      5,
      maxBatchNumRecords,
      maxBatchNumRecords,
      16000,
      .75);
  }

  @Test
  public void testNoProbeDataForStats() {
    final int maxBatchNumRecords = 20;
    final double fragmentationFactor = 2.0;
    final double safetyFactor = 1.5;

    final HashJoinMemoryCalculatorImpl.BuildSidePartitioningImpl calc =
      new HashJoinMemoryCalculatorImpl.BuildSidePartitioningImpl(
        BatchSizePredictorImpl.Factory.INSTANCE,
        new HashTableSizeCalculatorConservativeImpl(RecordBatch.MAX_BATCH_ROW_COUNT, HashTableSizeCalculatorConservativeImpl.HASHTABLE_DOUBLING_FACTOR),
        HashJoinHelperSizeCalculatorImpl.INSTANCE,
        fragmentationFactor,
        safetyFactor, false);

    final CaseInsensitiveMap<Long> keySizes = CaseInsensitiveMap.newHashMap();

    calc.initialize(
      true,
      false,
      keySizes,
      240,
      4,
      false,
      new MockBatchSizePredictor(20, 20, fragmentationFactor, safetyFactor),
      new MockBatchSizePredictor(),
      10,
      5,
      maxBatchNumRecords,
      maxBatchNumRecords,
      16000,
      .75);

    final HashJoinMemoryCalculator.PartitionStatSet partitionStatSet =
      new HashJoinMemoryCalculator.PartitionStatSet(new PartitionStatImpl(), new PartitionStatImpl());
    calc.setPartitionStatSet(partitionStatSet);

    long expectedReservedMemory = 60 // Max incoming batch size
      + 4 * 30; // build side batch for each spilled partition
    long actualReservedMemory = calc.getBuildReservedMemory();

    Assert.assertEquals(expectedReservedMemory, actualReservedMemory);
    Assert.assertEquals(4, calc.getNumPartitions());
  }

  @Test
  public void testProbeEmpty() {
    final int maxBatchNumRecords = 20;
    final double fragmentationFactor = 2.0;
    final double safetyFactor = 1.5;

    final HashJoinMemoryCalculatorImpl.BuildSidePartitioningImpl calc =
      new HashJoinMemoryCalculatorImpl.BuildSidePartitioningImpl(
        BatchSizePredictorImpl.Factory.INSTANCE,
        new HashTableSizeCalculatorConservativeImpl(RecordBatch.MAX_BATCH_ROW_COUNT, HashTableSizeCalculatorConservativeImpl.HASHTABLE_DOUBLING_FACTOR),
        HashJoinHelperSizeCalculatorImpl.INSTANCE,
        fragmentationFactor,
        safetyFactor, false);

    final CaseInsensitiveMap<Long> keySizes = CaseInsensitiveMap.newHashMap();

    calc.initialize(
      true,
      false,
      keySizes,
      200,
      4,
      true,
      new MockBatchSizePredictor(20, 20, fragmentationFactor, safetyFactor),
      new MockBatchSizePredictor(),
      10,
      5,
      maxBatchNumRecords,
      maxBatchNumRecords,
      16000,
      .75);

    final HashJoinMemoryCalculator.PartitionStatSet partitionStatSet =
      new HashJoinMemoryCalculator.PartitionStatSet(new PartitionStatImpl(), new PartitionStatImpl(),
        new PartitionStatImpl(), new PartitionStatImpl());
    calc.setPartitionStatSet(partitionStatSet);

    long expectedReservedMemory = 60 // Max incoming batch size
      + 4 * 30; // build side batch for each spilled partition
    long actualReservedMemory = calc.getBuildReservedMemory();

    Assert.assertEquals(expectedReservedMemory, actualReservedMemory);
    Assert.assertEquals(4, calc.getNumPartitions());
  }

  @Test
  public void testNoRoomInMemoryForBatch1FirstCycle() {
    testNoRoomInMemoryForBatch1Helper(true);
  }

  @Test
  public void testNoRoomInMemoryForBatch1NotFirstCycle() {
    testNoRoomInMemoryForBatch1Helper(false);
  }

  private void testNoRoomInMemoryForBatch1Helper(final boolean firstCycle) {
    final int maxBatchNumRecords = 20;
    final double fragmentationFactor = 2.0;
    final double safetyFactor = 1.5;
    final long accountedProbeBatchSize = firstCycle? 0: 10;

    final HashJoinMemoryCalculatorImpl.BuildSidePartitioningImpl calc =
      new HashJoinMemoryCalculatorImpl.BuildSidePartitioningImpl(
        BatchSizePredictorImpl.Factory.INSTANCE,
        new HashTableSizeCalculatorConservativeImpl(RecordBatch.MAX_BATCH_ROW_COUNT, HashTableSizeCalculatorConservativeImpl.HASHTABLE_DOUBLING_FACTOR),
        HashJoinHelperSizeCalculatorImpl.INSTANCE,
        fragmentationFactor,
        safetyFactor, false);

    final CaseInsensitiveMap<Long> keySizes = CaseInsensitiveMap.newHashMap();

    calc.initialize(
      firstCycle,
      false,
      keySizes,
      120 + accountedProbeBatchSize,
      2,
      false,
      new MockBatchSizePredictor(20, 20, fragmentationFactor, safetyFactor),
      new MockBatchSizePredictor(10, 10, fragmentationFactor, safetyFactor),
      10,
      5,
      maxBatchNumRecords,
      maxBatchNumRecords,
      16000,
      .75);

    final PartitionStatImpl partition1 = new PartitionStatImpl();
    final PartitionStatImpl partition2 = new PartitionStatImpl();
    final HashJoinMemoryCalculator.PartitionStatSet partitionStatSet =
      new HashJoinMemoryCalculator.PartitionStatSet(partition1, partition2);
    calc.setPartitionStatSet(partitionStatSet);

    long expectedReservedMemory = 60 // Max incoming batch size
      + 2 * 30 // build side batch for each spilled partition
      + accountedProbeBatchSize; // Max incoming probe batch size
    long actualReservedMemory = calc.getBuildReservedMemory();

    Assert.assertEquals(expectedReservedMemory, actualReservedMemory);
    Assert.assertEquals(2, calc.getNumPartitions());

    partition1.add(new HashJoinMemoryCalculator.BatchStat(10, 8));
    Assert.assertTrue(calc.shouldSpill());
  }

  @Test
  public void testCompleteLifeCycle() {
    final int maxBatchNumRecords = 20;
    final double fragmentationFactor = 2.0;
    final double safetyFactor = 1.5;

    final HashJoinMemoryCalculatorImpl.BuildSidePartitioningImpl calc =
      new HashJoinMemoryCalculatorImpl.BuildSidePartitioningImpl(
        BatchSizePredictorImpl.Factory.INSTANCE,
        new HashTableSizeCalculatorConservativeImpl(RecordBatch.MAX_BATCH_ROW_COUNT, HashTableSizeCalculatorConservativeImpl.HASHTABLE_DOUBLING_FACTOR),
        HashJoinHelperSizeCalculatorImpl.INSTANCE,
        fragmentationFactor,
        safetyFactor, false);

    final CaseInsensitiveMap<Long> keySizes = CaseInsensitiveMap.newHashMap();

    calc.initialize(
      true,
      false,
      keySizes,
      160,
      2,
      false,
      new MockBatchSizePredictor(20, 20, fragmentationFactor, safetyFactor),
      new MockBatchSizePredictor(10, 10, fragmentationFactor, safetyFactor),
      10,
      5,
      maxBatchNumRecords,
      maxBatchNumRecords,
      16000,
      .75);

    final PartitionStatImpl partition1 = new PartitionStatImpl();
    final PartitionStatImpl partition2 = new PartitionStatImpl();
    final HashJoinMemoryCalculator.PartitionStatSet partitionStatSet =
      new HashJoinMemoryCalculator.PartitionStatSet(partition1, partition2);
    calc.setPartitionStatSet(partitionStatSet);


    // Add to partition 1, no spill needed
    {
      partition1.add(new HashJoinMemoryCalculator.BatchStat(10, 7));
      Assert.assertFalse(calc.shouldSpill());
    }

    // Add to partition 2, no spill needed
    {
      partition2.add(new HashJoinMemoryCalculator.BatchStat(10, 8));
      Assert.assertFalse(calc.shouldSpill());
    }

    // Add to partition 1, and partition 1 spilled
    {
      partition1.add(new HashJoinMemoryCalculator.BatchStat(10, 8));
      Assert.assertTrue(calc.shouldSpill());
      partition1.spill();
    }

    // Add to partition 2, no spill needed
    {
      partition2.add(new HashJoinMemoryCalculator.BatchStat(10, 7));
      Assert.assertFalse(calc.shouldSpill());
    }

    // Add to partition 2, and partition 2 spilled
    {
      partition2.add(new HashJoinMemoryCalculator.BatchStat(10, 8));
      Assert.assertTrue(calc.shouldSpill());
      partition2.spill();
    }

    Assert.assertNotNull(calc.next());
  }

  public static class MockBatchSizePredictor implements BatchSizePredictor {
    private final boolean hasData;
    private final long batchSize;
    private final int numRecords;
    private final double fragmentationFactor;
    private final double safetyFactor;

    public MockBatchSizePredictor() {
      hasData = false;
      batchSize = 0;
      numRecords = 0;
      fragmentationFactor = 0;
      safetyFactor = 0;
    }

    public MockBatchSizePredictor(final long batchSize,
                                  final int numRecords,
                                  final double fragmentationFactor,
                                  final double safetyFactor) {
      hasData = true;
      this.batchSize = batchSize;
      this.numRecords = numRecords;
      this.fragmentationFactor = fragmentationFactor;
      this.safetyFactor = safetyFactor;
    }

    @Override
    public long getBatchSize() {
      return batchSize;
    }

    @Override
    public int getNumRecords() {
      return numRecords;
    }

    @Override
    public boolean hadDataLastTime() {
      return hasData;
    }

    @Override
    public void updateStats() {
    }

    @Override
    public long predictBatchSize(int desiredNumRecords, boolean reserveHash) {
      Preconditions.checkState(hasData);
      return BatchSizePredictorImpl.computeMaxBatchSize(batchSize,
        numRecords,
        desiredNumRecords,
        fragmentationFactor,
        safetyFactor,
        reserveHash);
    }
  }
}
