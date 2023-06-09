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
import org.apache.drill.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestPostBuildCalculationsImpl extends BaseTest {
  @Test
  public void testProbeTooBig() {
    final int minProbeRecordsPerBatch = 10;

    final int computedProbeRecordsPerBatch =
      HashJoinMemoryCalculatorImpl.PostBuildCalculationsImpl.computeProbeRecordsPerBatch(
        100,
        2,
        100,
        minProbeRecordsPerBatch,
        70,
        40,
        200);

    Assert.assertEquals(minProbeRecordsPerBatch, computedProbeRecordsPerBatch);
  }

  @Test
  public void testComputedShouldBeMin() {
    final int minProbeRecordsPerBatch = 10;

    final int computedProbeRecordsPerBatch =
      HashJoinMemoryCalculatorImpl.PostBuildCalculationsImpl.computeProbeRecordsPerBatch(
        100,
        2,
        100,
        minProbeRecordsPerBatch,
        50,
        40,
        200);

    Assert.assertEquals(minProbeRecordsPerBatch, computedProbeRecordsPerBatch);
  }

  @Test
  public void testComputedProbeRecordsPerBatch() {
    final int minProbeRecordsPerBatch = 10;

    final int computedProbeRecordsPerBatch =
      HashJoinMemoryCalculatorImpl.PostBuildCalculationsImpl.computeProbeRecordsPerBatch(
        200,
        2,
        100,
        minProbeRecordsPerBatch,
        50,
        50,
        200);

    Assert.assertEquals(25, computedProbeRecordsPerBatch);
  }

  @Test
  public void testComputedProbeRecordsPerBatchRoundUp() {
    final int minProbeRecordsPerBatch = 10;

    final int computedProbeRecordsPerBatch =
      HashJoinMemoryCalculatorImpl.PostBuildCalculationsImpl.computeProbeRecordsPerBatch(
        200,
        2,
        100,
        minProbeRecordsPerBatch,
        50,
        51,
        199);

    Assert.assertEquals(25, computedProbeRecordsPerBatch);
  }

  @Test(expected = IllegalStateException.class)
  public void testHasProbeDataButProbeEmpty() {
    final Map<String, Long> keySizes = org.apache.drill.common.map.CaseInsensitiveMap.newHashMap();

    final PartitionStatImpl partition1 = new PartitionStatImpl();
    final PartitionStatImpl partition2 = new PartitionStatImpl();
    final HashJoinMemoryCalculator.PartitionStatSet buildPartitionStatSet =
      new HashJoinMemoryCalculator.PartitionStatSet(partition1, partition2);

    final int recordsPerPartitionBatchBuild = 10;

    addBatches(partition1, recordsPerPartitionBatchBuild,
      10, 4);
    addBatches(partition2, recordsPerPartitionBatchBuild,
      10, 4);

    final double fragmentationFactor = 2.0;
    final double safetyFactor = 1.5;

    final int maxBatchNumRecordsProbe = 3;
    final int recordsPerPartitionBatchProbe = 5;
    final long partitionProbeBatchSize = 15;
    final long maxProbeBatchSize = 60;

    final HashJoinMemoryCalculatorImpl.PostBuildCalculationsImpl calc =
      new HashJoinMemoryCalculatorImpl.PostBuildCalculationsImpl(
        true,
        new ConditionalMockBatchSizePredictor(
          Lists.newArrayList(maxBatchNumRecordsProbe, recordsPerPartitionBatchProbe),
          Lists.newArrayList(maxProbeBatchSize, partitionProbeBatchSize),
          true),
        290, // memoryAvailable
        20, // maxOutputBatchSize
        maxBatchNumRecordsProbe,
        recordsPerPartitionBatchProbe,
        buildPartitionStatSet, // buildPartitionStatSet
        keySizes, // keySizes
        new MockHashTableSizeCalculator(10), // hashTableSizeCalculator
        new MockHashJoinHelperSizeCalculator(10), // hashJoinHelperSizeCalculator
        fragmentationFactor, // fragmentationFactor
        safetyFactor, // safetyFactor
        .75, // loadFactor
        false, false); // reserveHash

    calc.initialize(true);
  }

  @Test
  public void testProbeEmpty() {
    final Map<String, Long> keySizes = org.apache.drill.common.map.CaseInsensitiveMap.newHashMap();

    final PartitionStatImpl partition1 = new PartitionStatImpl();
    final PartitionStatImpl partition2 = new PartitionStatImpl();
    final HashJoinMemoryCalculator.PartitionStatSet buildPartitionStatSet =
      new HashJoinMemoryCalculator.PartitionStatSet(partition1, partition2);

    final int recordsPerPartitionBatchBuild = 10;

    addBatches(partition1, recordsPerPartitionBatchBuild,
      10, 4);
    addBatches(partition2, recordsPerPartitionBatchBuild,
      10, 4);

    final double fragmentationFactor = 2.0;
    final double safetyFactor = 1.5;

    final int maxBatchNumRecordsProbe = 3;
    final int recordsPerPartitionBatchProbe = 5;
    final long partitionProbeBatchSize = 40;
    final long maxProbeBatchSize = 10000;

    final HashJoinMemoryCalculatorImpl.PostBuildCalculationsImpl calc =
      new HashJoinMemoryCalculatorImpl.PostBuildCalculationsImpl(
        true,
        new ConditionalMockBatchSizePredictor(),
        50,
        1000,
        maxBatchNumRecordsProbe,
        recordsPerPartitionBatchProbe,
        buildPartitionStatSet,
        keySizes,
        new MockHashTableSizeCalculator(10),
        new MockHashJoinHelperSizeCalculator(10),
        fragmentationFactor,
        safetyFactor,
        .75,
        true, false);

    calc.initialize(true);

    Assert.assertFalse(calc.shouldSpill());
    Assert.assertFalse(calc.shouldSpill());
  }

  @Test
  public void testHasNoProbeDataButProbeNonEmptyFirstCycle() {
    testHasNoProbeDataButProbeNonEmptyHelper(true);
  }

  @Test
  public void testHasNoProbeDataButProbeNonEmptyNotFirstCycle() {
    testHasNoProbeDataButProbeNonEmptyHelper(false);
  }

  private void testHasNoProbeDataButProbeNonEmptyHelper(final boolean firstCycle) {
    final Map<String, Long> keySizes = org.apache.drill.common.map.CaseInsensitiveMap.newHashMap();

    final PartitionStatImpl partition1 = new PartitionStatImpl();
    final PartitionStatImpl partition2 = new PartitionStatImpl();
    final HashJoinMemoryCalculator.PartitionStatSet buildPartitionStatSet =
      new HashJoinMemoryCalculator.PartitionStatSet(partition1, partition2);

    final int recordsPerPartitionBatchBuild = 10;

    addBatches(partition1, recordsPerPartitionBatchBuild,
      10, 4);
    addBatches(partition2, recordsPerPartitionBatchBuild,
      10, 4);

    final double fragmentationFactor = 2.0;
    final double safetyFactor = 1.5;

    final int maxBatchNumRecordsProbe = 3;
    final int recordsPerPartitionBatchProbe = 5;
    final long partitionProbeBatchSize = 15;
    final long maxProbeBatchSize = 60;
    final long accountedProbeBatchSize = (firstCycle? 0: maxProbeBatchSize);

    final HashJoinMemoryCalculatorImpl.PostBuildCalculationsImpl calc =
      new HashJoinMemoryCalculatorImpl.PostBuildCalculationsImpl(
        firstCycle,
        new ConditionalMockBatchSizePredictor(
          Lists.newArrayList(maxBatchNumRecordsProbe, recordsPerPartitionBatchProbe),
          Lists.newArrayList(maxProbeBatchSize, partitionProbeBatchSize),
          false),
        230 + accountedProbeBatchSize,
        20,
        maxBatchNumRecordsProbe,
        recordsPerPartitionBatchProbe,
        buildPartitionStatSet,
        keySizes,
        new MockHashTableSizeCalculator(10),
        new MockHashJoinHelperSizeCalculator(10),
        fragmentationFactor,
        safetyFactor,
        .75,
        false, false);

    calc.initialize(false);

    long expected = accountedProbeBatchSize
      + 160 // in memory partitions
      + 20 // max output batch size
      + 2 * 10 // Hash Table
      + 2 * 10; // Hash join helper
    Assert.assertFalse(calc.shouldSpill());
    Assert.assertEquals(expected, calc.getConsumedMemory());
    Assert.assertNull(calc.next());
  }

  @Test
  public void testProbingAndPartitioningBuildAllInMemoryNoSpillFirstCycle() {
    testProbingAndPartitioningBuildAllInMemoryNoSpillHelper(true);
  }

  @Test
  public void testProbingAndPartitioningBuildAllInMemoryNoSpillNotFirstCycle() {
    testProbingAndPartitioningBuildAllInMemoryNoSpillHelper(false);
  }

  private void testProbingAndPartitioningBuildAllInMemoryNoSpillHelper(final boolean firstCycle) {
    final Map<String, Long> keySizes = org.apache.drill.common.map.CaseInsensitiveMap.newHashMap();

    final PartitionStatImpl partition1 = new PartitionStatImpl();
    final PartitionStatImpl partition2 = new PartitionStatImpl();
    final HashJoinMemoryCalculator.PartitionStatSet buildPartitionStatSet =
      new HashJoinMemoryCalculator.PartitionStatSet(partition1, partition2);

    final int recordsPerPartitionBatchBuild = 10;

    addBatches(partition1, recordsPerPartitionBatchBuild,
      10, 4);
    addBatches(partition2, recordsPerPartitionBatchBuild,
      10, 4);

    final double fragmentationFactor = 2.0;
    final double safetyFactor = 1.5;

    final int maxBatchNumRecordsProbe = 3;
    final int recordsPerPartitionBatchProbe = 5;
    final long partitionProbeBatchSize = 15;
    final long maxProbeBatchSize = 60;
    final long accountedProbeBatchSize = (firstCycle? 0: maxProbeBatchSize);

    final HashJoinMemoryCalculatorImpl.PostBuildCalculationsImpl calc =
      new HashJoinMemoryCalculatorImpl.PostBuildCalculationsImpl(
        firstCycle,
        new ConditionalMockBatchSizePredictor(
          Lists.newArrayList(maxBatchNumRecordsProbe, recordsPerPartitionBatchProbe),
          Lists.newArrayList(maxProbeBatchSize, partitionProbeBatchSize),
          true),
        230 + accountedProbeBatchSize,
        20,
        maxBatchNumRecordsProbe,
        recordsPerPartitionBatchProbe,
        buildPartitionStatSet,
        keySizes,
        new MockHashTableSizeCalculator(10),
        new MockHashJoinHelperSizeCalculator(10),
        fragmentationFactor,
        safetyFactor,
        .75,
        false, false);

    calc.initialize(false);

    long expected = accountedProbeBatchSize
      + 160 // in memory partitions
      + 20 // max output batch size
      + 2 * 10 // Hash Table
      + 2 * 10; // Hash join helper
    Assert.assertFalse(calc.shouldSpill());
    Assert.assertEquals(expected, calc.getConsumedMemory());
    Assert.assertNull(calc.next());
  }

  @Test
  public void testProbingAndPartitioningBuildAllInMemorySpillFirstCycle() {
    testProbingAndPartitioningBuildAllInMemorySpillHelper(true);
  }

  @Test
  public void testProbingAndPartitioningBuildAllInMemorySpillNotFirstCycle() {
    testProbingAndPartitioningBuildAllInMemorySpillHelper(false);
  }

  private void testProbingAndPartitioningBuildAllInMemorySpillHelper(final boolean firstCycle) {
    final Map<String, Long> keySizes = org.apache.drill.common.map.CaseInsensitiveMap.newHashMap();

    final PartitionStatImpl partition1 = new PartitionStatImpl();
    final PartitionStatImpl partition2 = new PartitionStatImpl();
    final HashJoinMemoryCalculator.PartitionStatSet buildPartitionStatSet =
      new HashJoinMemoryCalculator.PartitionStatSet(partition1, partition2);

    final int recordsPerPartitionBatchBuild = 10;

    addBatches(partition1, recordsPerPartitionBatchBuild,
      10, 4);
    addBatches(partition2, recordsPerPartitionBatchBuild,
      10, 4);

    final double fragmentationFactor = 2.0;
    final double safetyFactor = 1.5;

    final int maxBatchNumRecordsProbe = 3;
    final int recordsPerPartitionBatchProbe = 5;
    final long partitionProbeBatchSize = 15;
    final long maxProbeBatchSize = 60;
    final long accountedProbeBatchSize = (firstCycle? 0: maxProbeBatchSize);

    HashJoinMemoryCalculatorImpl.PostBuildCalculationsImpl calc =
      new HashJoinMemoryCalculatorImpl.PostBuildCalculationsImpl(
        firstCycle,
        new ConditionalMockBatchSizePredictor(
          Lists.newArrayList(maxBatchNumRecordsProbe, recordsPerPartitionBatchProbe),
          Lists.newArrayList(maxProbeBatchSize, partitionProbeBatchSize),
          true),
        210 + accountedProbeBatchSize,
        20,
        maxBatchNumRecordsProbe,
        recordsPerPartitionBatchProbe,
        buildPartitionStatSet,
        keySizes,
        new MockHashTableSizeCalculator(10),
        new MockHashJoinHelperSizeCalculator(10),
        fragmentationFactor,
        safetyFactor,
        .75,
        false, false);

    calc.initialize(false);

    long expected = accountedProbeBatchSize
      + 160 // in memory partitions
      + 20 // max output batch size
      + 2 * 10 // Hash Table
      + 2 * 10; // Hash join helper
    Assert.assertTrue(calc.shouldSpill());
    Assert.assertEquals(expected, calc.getConsumedMemory());
    partition1.spill();

    expected = accountedProbeBatchSize
      + 80 // in memory partitions
      + 20 // max output batch size
      + 10 // Hash Table
      + 10 // Hash join helper
      + 15; // partition batch size
    Assert.assertFalse(calc.shouldSpill());
    Assert.assertEquals(expected, calc.getConsumedMemory());
    Assert.assertNotNull(calc.next());
  }

  @Test
  public void testProbingAndPartitioningBuildAllInMemoryNoSpillWithHashFirstCycle() {
    testProbingAndPartitioningBuildAllInMemoryNoSpillWithHashHelper(true);
  }

  @Test
  public void testProbingAndPartitioningBuildAllInMemoryNoSpillWithHashNotFirstCycle() {
    testProbingAndPartitioningBuildAllInMemoryNoSpillWithHashHelper(false);
  }

  private void testProbingAndPartitioningBuildAllInMemoryNoSpillWithHashHelper(final boolean firstCycle) {
    final Map<String, Long> keySizes = org.apache.drill.common.map.CaseInsensitiveMap.newHashMap();

    final PartitionStatImpl partition1 = new PartitionStatImpl();
    final PartitionStatImpl partition2 = new PartitionStatImpl();
    final HashJoinMemoryCalculator.PartitionStatSet buildPartitionStatSet =
      new HashJoinMemoryCalculator.PartitionStatSet(partition1, partition2);

    partition1.spill();
    partition2.spill();

    final double fragmentationFactor = 2.0;
    final double safetyFactor = 1.5;

    final int maxBatchNumRecordsProbe = 3;
    final int recordsPerPartitionBatchProbe = 5;
    final long partitionProbeBatchSize = 15;
    final long maxProbeBatchSize = 60;
    final long accountedProbeBatchSize = (firstCycle? 0: maxProbeBatchSize);

    HashJoinMemoryCalculatorImpl.PostBuildCalculationsImpl calc =
      new HashJoinMemoryCalculatorImpl.PostBuildCalculationsImpl(
        firstCycle,
        new ConditionalMockBatchSizePredictor(
          Lists.newArrayList(maxBatchNumRecordsProbe, recordsPerPartitionBatchProbe),
          Lists.newArrayList(maxProbeBatchSize, partitionProbeBatchSize),
          true),
        120 + accountedProbeBatchSize,
        20,
        maxBatchNumRecordsProbe,
        recordsPerPartitionBatchProbe,
        buildPartitionStatSet,
        keySizes,
        new MockHashTableSizeCalculator(10),
        new MockHashJoinHelperSizeCalculator(10),
        fragmentationFactor,
        safetyFactor,
        .75,
        true, false);

    calc.initialize(false);

    long expected = accountedProbeBatchSize // probe batch
      + 2 * 5 * 3 // partition batches
      + 20; // max output batch size
    Assert.assertFalse(calc.shouldSpill());
    Assert.assertEquals(expected, calc.getConsumedMemory());
    Assert.assertNotNull(calc.next());
  }

  @Test
  public void testProbingAndPartitioningBuildAllInMemoryWithSpillFirstCycle() {
    testProbingAndPartitioningBuildAllInMemoryWithSpillHelper(true);
  }

  @Test
  public void testProbingAndPartitioningBuildAllInMemoryWithSpillNotFirstCycle() {
    testProbingAndPartitioningBuildAllInMemoryWithSpillHelper(false);
  }

  private void testProbingAndPartitioningBuildAllInMemoryWithSpillHelper(final boolean firstCycle) {
    final Map<String, Long> keySizes = org.apache.drill.common.map.CaseInsensitiveMap.newHashMap();

    final PartitionStatImpl partition1 = new PartitionStatImpl();
    final PartitionStatImpl partition2 = new PartitionStatImpl();
    final HashJoinMemoryCalculator.PartitionStatSet buildPartitionStatSet =
      new HashJoinMemoryCalculator.PartitionStatSet(partition1, partition2);

    final int recordsPerPartitionBatchBuild = 10;

    addBatches(partition1, recordsPerPartitionBatchBuild, 10, 4);
    addBatches(partition2, recordsPerPartitionBatchBuild, 10, 4);

    final double fragmentationFactor = 2.0;
    final double safetyFactor = 1.5;

    final long hashTableSize = 10;
    final long hashJoinHelperSize = 10;
    final int maxBatchNumRecordsProbe = 3;
    final int recordsPerPartitionBatchProbe = 5;
    final long partitionProbeBatchSize = 15;
    final long maxProbeBatchSize = 60;
    final long accountedProbeBatchSize = (firstCycle? 0: maxProbeBatchSize);

    HashJoinMemoryCalculatorImpl.PostBuildCalculationsImpl calc =
      new HashJoinMemoryCalculatorImpl.PostBuildCalculationsImpl(
        firstCycle,
        new ConditionalMockBatchSizePredictor(
          Lists.newArrayList(maxBatchNumRecordsProbe, recordsPerPartitionBatchProbe),
          Lists.newArrayList(maxProbeBatchSize, partitionProbeBatchSize),
          true),
        140 + accountedProbeBatchSize,
        20,
        maxBatchNumRecordsProbe,
        recordsPerPartitionBatchProbe,
        buildPartitionStatSet,
        keySizes,
        new MockHashTableSizeCalculator(hashTableSize),
        new MockHashJoinHelperSizeCalculator(hashJoinHelperSize),
        fragmentationFactor,
        safetyFactor,
        .75,
        false, false);

    calc.initialize(false);

    long expected = accountedProbeBatchSize
      + 80 // in memory partition
      + 10 // hash table size
      + 10 // hash join helper size
      + 15 // max partition probe batch size
      + 20; // outgoing batch size

    Assert.assertTrue(calc.shouldSpill());
    partition1.spill();
    Assert.assertFalse(calc.shouldSpill());
    Assert.assertEquals(expected, calc.getConsumedMemory());
    Assert.assertNotNull(calc.next());
  }

  @Test
  public void testProbingAndPartitioningBuildSomeInMemoryFirstCycle() {
    testProbingAndPartitioningBuildSomeInMemoryHelper(true);
  }

  @Test
  public void testProbingAndPartitioningBuildSomeInMemoryNotFirstCycle() {
    testProbingAndPartitioningBuildSomeInMemoryHelper(false);
  }

  private void testProbingAndPartitioningBuildSomeInMemoryHelper(final boolean firstCycle) {
    final Map<String, Long> keySizes = org.apache.drill.common.map.CaseInsensitiveMap.newHashMap();

    final PartitionStatImpl partition1 = new PartitionStatImpl();
    final PartitionStatImpl partition2 = new PartitionStatImpl();
    final PartitionStatImpl partition3 = new PartitionStatImpl();
    final PartitionStatImpl partition4 = new PartitionStatImpl();
    final HashJoinMemoryCalculator.PartitionStatSet buildPartitionStatSet =
      new HashJoinMemoryCalculator.PartitionStatSet(partition1, partition2, partition3, partition4);

    final int recordsPerPartitionBatchBuild = 10;

    partition1.spill();
    partition2.spill();
    addBatches(partition3, recordsPerPartitionBatchBuild, 10, 4);
    addBatches(partition4, recordsPerPartitionBatchBuild, 10, 4);

    final double fragmentationFactor = 2.0;
    final double safetyFactor = 1.5;

    final long hashTableSize = 10;
    final long hashJoinHelperSize = 10;
    final int maxBatchNumRecordsProbe = 3;
    final int recordsPerPartitionBatchProbe = 5;
    final long partitionProbeBatchSize = 15;
    final long maxProbeBatchSize = 60;
    final long accountedProbeBatchSize = (firstCycle? 0: maxProbeBatchSize);

    HashJoinMemoryCalculatorImpl.PostBuildCalculationsImpl calc =
      new HashJoinMemoryCalculatorImpl.PostBuildCalculationsImpl(
        firstCycle,
        new ConditionalMockBatchSizePredictor(
          Lists.newArrayList(maxBatchNumRecordsProbe, recordsPerPartitionBatchProbe),
          Lists.newArrayList(maxProbeBatchSize, partitionProbeBatchSize),
          true),
        170 + accountedProbeBatchSize,
        20,
        maxBatchNumRecordsProbe,
        recordsPerPartitionBatchProbe,
        buildPartitionStatSet,
        keySizes,
        new MockHashTableSizeCalculator(hashTableSize),
        new MockHashJoinHelperSizeCalculator(hashJoinHelperSize),
        fragmentationFactor,
        safetyFactor,
        .75,
        false, false);

    calc.initialize(false);

    long expected = accountedProbeBatchSize
      + 80 // in memory partition
      + 10 // hash table size
      + 10 // hash join helper size
      + 15 * 3 // max batch size for each spill probe partition
      + 20;
    Assert.assertTrue(calc.shouldSpill());
    partition3.spill();
    Assert.assertFalse(calc.shouldSpill());
    Assert.assertEquals(expected, calc.getConsumedMemory());
    Assert.assertNotNull(calc.next());
  }

  @Test
  public void testProbingAndPartitioningBuildNoneInMemoryFirstCycle() {
    testProbingAndPartitioningBuildNoneInMemoryHelper(true);
  }

  @Test
  public void testProbingAndPartitioningBuildNoneInMemoryNotFirstCycle() {
    testProbingAndPartitioningBuildNoneInMemoryHelper(false);
  }

  private void testProbingAndPartitioningBuildNoneInMemoryHelper(final boolean firstCycle) {
    final Map<String, Long> keySizes = org.apache.drill.common.map.CaseInsensitiveMap.newHashMap();

    final PartitionStatImpl partition1 = new PartitionStatImpl();
    final PartitionStatImpl partition2 = new PartitionStatImpl();
    final HashJoinMemoryCalculator.PartitionStatSet buildPartitionStatSet =
      new HashJoinMemoryCalculator.PartitionStatSet(partition1, partition2);

    partition1.spill();
    partition2.spill();

    final double fragmentationFactor = 2.0;
    final double safetyFactor = 1.5;

    final long hashTableSize = 10;
    final long hashJoinHelperSize = 10;
    final int maxBatchNumRecordsProbe = 3;
    final int recordsPerPartitionBatchProbe = 5;
    final long partitionProbeBatchSize = 15;
    final long maxProbeBatchSize = 60;
    final long accountedProbeBatchSize = (firstCycle? 0: maxProbeBatchSize);

    HashJoinMemoryCalculatorImpl.PostBuildCalculationsImpl calc =
      new HashJoinMemoryCalculatorImpl.PostBuildCalculationsImpl(
        firstCycle,
        new ConditionalMockBatchSizePredictor(
          Lists.newArrayList(maxBatchNumRecordsProbe, recordsPerPartitionBatchProbe),
          Lists.newArrayList(maxProbeBatchSize, partitionProbeBatchSize),
          true),
        110 + accountedProbeBatchSize,
        20,
        maxBatchNumRecordsProbe,
        recordsPerPartitionBatchProbe,
        buildPartitionStatSet,
        keySizes,
        new MockHashTableSizeCalculator(hashTableSize),
        new MockHashJoinHelperSizeCalculator(hashJoinHelperSize),
        fragmentationFactor,
        safetyFactor,
        .75,
        false, false);

    calc.initialize(false);
    Assert.assertFalse(calc.shouldSpill());
    Assert.assertEquals(50 + accountedProbeBatchSize, calc.getConsumedMemory());
    Assert.assertNotNull(calc.next());
  }

  @Test // Make sure I don't fail
  public void testMakeDebugString()
  {
    final Map<String, Long> keySizes = org.apache.drill.common.map.CaseInsensitiveMap.newHashMap();

    final PartitionStatImpl partition1 = new PartitionStatImpl();
    final PartitionStatImpl partition2 = new PartitionStatImpl();
    final PartitionStatImpl partition3 = new PartitionStatImpl();
    final PartitionStatImpl partition4 = new PartitionStatImpl();
    final HashJoinMemoryCalculator.PartitionStatSet buildPartitionStatSet =
      new HashJoinMemoryCalculator.PartitionStatSet(partition1, partition2, partition3, partition4);

    final int recordsPerPartitionBatchBuild = 10;

    partition1.spill();
    partition2.spill();
    addBatches(partition3, recordsPerPartitionBatchBuild, 10, 4);
    addBatches(partition4, recordsPerPartitionBatchBuild, 10, 4);

    final double fragmentationFactor = 2.0;
    final double safetyFactor = 1.5;

    final long hashTableSize = 10;
    final long hashJoinHelperSize = 10;
    final int maxBatchNumRecordsProbe = 3;
    final int recordsPerPartitionBatchProbe = 5;
    final long partitionProbeBatchSize = 15;
    final long maxProbeBatchSize = 60;

    HashJoinMemoryCalculatorImpl.PostBuildCalculationsImpl calc =
      new HashJoinMemoryCalculatorImpl.PostBuildCalculationsImpl(
        true,
        new ConditionalMockBatchSizePredictor(
          Lists.newArrayList(maxBatchNumRecordsProbe, recordsPerPartitionBatchProbe),
          Lists.newArrayList(maxProbeBatchSize, partitionProbeBatchSize),
          true),
        230,
        20,
        maxBatchNumRecordsProbe,
        recordsPerPartitionBatchProbe,
        buildPartitionStatSet,
        keySizes,
        new MockHashTableSizeCalculator(hashTableSize),
        new MockHashJoinHelperSizeCalculator(hashJoinHelperSize),
        fragmentationFactor,
        safetyFactor,
        .75,
        false, false);

    calc.initialize(false);
  }

  private void addBatches(PartitionStatImpl partitionStat,
                          int recordsPerPartitionBatchBuild,
                          long batchSize,
                          int numBatches) {
    for (int counter = 0; counter < numBatches; counter++) {
      partitionStat.add(new HashJoinMemoryCalculator.BatchStat(
        recordsPerPartitionBatchBuild, batchSize));
    }
  }

  public static class MockHashTableSizeCalculator implements HashTableSizeCalculator {
    private final long size;

    public MockHashTableSizeCalculator(final long size) {
      this.size = size;
    }

    @Override
    public long calculateSize(HashJoinMemoryCalculator.PartitionStat partitionStat,
                              Map<String, Long> keySizes,
                              double loadFactor, double safetyFactor, double fragmentationFactor) {
      return size;
    }

    @Override
    public double getDoublingFactor() {
      return HashTableSizeCalculatorConservativeImpl.HASHTABLE_DOUBLING_FACTOR;
    }

    @Override
    public String getType() {
      return null;
    }
  }

  public static class MockHashJoinHelperSizeCalculator implements HashJoinHelperSizeCalculator {
    private final long size;

    public MockHashJoinHelperSizeCalculator(final long size)
    {
      this.size = size;
    }

    @Override
    public long calculateSize(HashJoinMemoryCalculator.PartitionStat partitionStat, double fragmentationFactor) {
      return size;
    }
  }

  public static class ConditionalMockBatchSizePredictor implements BatchSizePredictor {
    private final List<Integer> recordsPerBatch;
    private final List<Long> batchSize;

    private boolean hasData;
    private boolean updateable;

    public ConditionalMockBatchSizePredictor() {
      recordsPerBatch = new ArrayList<>();
      batchSize = new ArrayList<>();
      hasData = false;
      updateable = true;
    }

    public ConditionalMockBatchSizePredictor(final List<Integer> recordsPerBatch,
                                             final List<Long> batchSize,
                                             final boolean hasData) {
      this.recordsPerBatch = Preconditions.checkNotNull(recordsPerBatch);
      this.batchSize = Preconditions.checkNotNull(batchSize);

      Preconditions.checkArgument(recordsPerBatch.size() == batchSize.size());

      this.hasData = hasData;
      updateable = true;
    }

    @Override
    public long getBatchSize() {
      return batchSize.get(0);
    }

    @Override
    public int getNumRecords() {
      return 0;
    }

    @Override
    public boolean hadDataLastTime() {
      return hasData;
    }

    @Override
    public void updateStats() {
      Preconditions.checkState(updateable);
      updateable = false;
      hasData = true;
    }

    @Override
    public long predictBatchSize(int desiredNumRecords, boolean reserveHash) {
      Preconditions.checkState(hasData);

      for (int index = 0; index < recordsPerBatch.size(); index++) {
        if (desiredNumRecords == recordsPerBatch.get(index)) {
          return batchSize.get(index);
        }
      }

      throw new IllegalArgumentException();
    }
  }
}
