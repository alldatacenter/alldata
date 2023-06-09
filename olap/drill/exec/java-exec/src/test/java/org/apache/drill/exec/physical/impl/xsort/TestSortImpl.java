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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.drill.categories.OperatorTest;
import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.common.logical.data.Order.Ordering;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.config.Sort;
import org.apache.drill.exec.physical.impl.spill.SpillSet;
import org.apache.drill.exec.physical.impl.xsort.SortImpl.SortResults;
import org.apache.drill.exec.proto.ExecProtos.FragmentHandle;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.test.BaseDirTestWatcher;
import org.apache.drill.test.DrillTest;
import org.apache.drill.test.OperatorFixture;
import org.apache.drill.exec.physical.rowSet.DirectRowSet;
import org.apache.drill.exec.physical.rowSet.HyperRowSetImpl;
import org.apache.drill.exec.physical.rowSet.IndirectRowSet;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSet.ExtendableRowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.test.rowSet.RowSetComparison;
import org.apache.drill.exec.physical.rowSet.RowSetReader;
import org.apache.drill.exec.physical.rowSet.RowSetWriter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

import io.netty.buffer.DrillBuf;

/**
 * Tests the external sort implementation: the "guts" of the sort stripped of the
 * Volcano-protocol layer. Assumes the individual components are already tested.
 */
@Category(OperatorTest.class)
public class TestSortImpl extends DrillTest {

  @Rule
  public final BaseDirTestWatcher dirTestWatcher = new BaseDirTestWatcher();

  private static VectorContainer dest;

  /**
   * Create the sort implementation to be used by test.
   *
   * @param fixture operator fixture
   * @param sortOrder sort order as specified by {@link Ordering}
   * @param nullOrder null order as specified by {@link Ordering}
   * @return the initialized sort implementation, ready to do work
   */
  public static SortImpl makeSortImpl(OperatorFixture fixture,
                               String sortOrder, String nullOrder) {
    FieldReference expr = FieldReference.getWithQuotedRef("key");
    Ordering ordering = new Ordering(sortOrder, expr, nullOrder);
    Sort popConfig = new Sort(null, Lists.newArrayList(ordering), false);
    OperatorContext opContext = fixture.newOperatorContext(popConfig);
    QueryId queryId = QueryId.newBuilder()
        .setPart1(1234)
        .setPart2(5678)
        .build();
    FragmentHandle handle = FragmentHandle.newBuilder()
          .setMajorFragmentId(2)
          .setMinorFragmentId(3)
          .setQueryId(queryId)
          .build();
    SortConfig sortConfig = new SortConfig(opContext.getFragmentContext().getConfig(), opContext.getFragmentContext().getOptions());

    SpillSet spillSet = new SpillSet(opContext.getFragmentContext().getConfig(), handle, popConfig);
    PriorityQueueCopierWrapper copierHolder = new PriorityQueueCopierWrapper(opContext);
    SpilledRuns spilledRuns = new SpilledRuns(opContext, spillSet, copierHolder);
    dest = new VectorContainer(opContext.getAllocator());
    return new SortImpl(opContext, sortConfig, spilledRuns, dest);
  }

  /**
   * Handy fixture to hold a sort, a set of input row sets (batches) and the
   * output set of row sets (batches.) Pumps the input into the sort and
   * harvests the output. Subclasses define the specifics of the sort,
   * define the input data, and validate the output data.
   */
  public static class SortTestFixture {
    private final OperatorFixture fixture;
    private final List<RowSet> inputSets = new ArrayList<>();
    private final List<RowSet> expected = new ArrayList<>();
    String sortOrder = Ordering.ORDER_ASC;
    String nullOrder = Ordering.NULLS_UNSPECIFIED;

    public SortTestFixture(OperatorFixture fixture) {
      this.fixture = fixture;
    }

    public SortTestFixture(OperatorFixture fixture, String sortOrder, String nullOrder) {
      this.fixture = fixture;
      this.sortOrder = sortOrder;
      this.nullOrder = nullOrder;
    }

    public void addInput(RowSet input) {
      inputSets.add(input);
    }

    public void addOutput(RowSet output) {
      expected.add(output);
    }

    public void run() {
      SortImpl sort = makeSortImpl(fixture, sortOrder, nullOrder);

      // Simulates a NEW_SCHEMA event

      if (! inputSets.isEmpty()) {
        sort.setSchema(inputSets.get(0).container().getSchema());
      }

      // Simulates an OK event

      for (RowSet input : inputSets) {
        sort.addBatch(input.vectorAccessible());
      }

      // Simulate returning results

      SortResults results = sort.startMerge();
      if (results.getContainer() != dest) {
        dest.clear();
        dest = results.getContainer();
      }
      for (RowSet expectedSet : expected) {
        assertTrue(results.next());
        RowSet rowSet = toRowSet(results, dest);
        new RowSetComparison(expectedSet)
              .verify(rowSet);
        expectedSet.clear();
      }
      assertFalse(results.next());
      validateSort(sort);
      results.close();
      dest.clear();
      sort.close();

      // Note: context closed separately because this is normally done by
      // the external sort itself after closing the output container.

      sort.opContext().close();
      validateFinalStats(sort);
    }

    protected void validateSort(SortImpl sort) { }
    protected void validateFinalStats(SortImpl sort) { }
  }

  /**
   * Sort produces a variety of output types. Convert each type to the corresponding
   * row set format. For historical reasons, the sort dumps its output into a vector
   * container (normally attached to the external sort batch, here used stand-alone.)
   *
   * @param results sort results iterator
   * @param dest container that holds the sort results
   */
  private static RowSet toRowSet(SortResults results, VectorContainer dest) {
    if (results.getSv4() != null) {
      return HyperRowSetImpl.fromContainer(dest, results.getSv4());
    } else if (results.getSv2() != null) {
      return IndirectRowSet.fromSv2(dest, results.getSv2());
    } else {
      return DirectRowSet.fromContainer(dest);
    }
  }

  /**
   * Test for null input (no input batches). Note that, in this case,
   * we never see a schema.
   */
  @Test
  public void testNullInput() throws Exception {
    try (OperatorFixture fixture = OperatorFixture.standardFixture(dirTestWatcher)) {
      SortTestFixture sortTest = new SortTestFixture(fixture);
      sortTest.run();
    }
  }

  /**
   * Test for an input with a schema, but only an empty input batch.
   */
  @Test
  public void testEmptyInput() throws Exception {
    try (OperatorFixture fixture = OperatorFixture.standardFixture(dirTestWatcher)) {
      TupleMetadata schema = SortTestUtilities.nonNullSchema();
      SortTestFixture sortTest = new SortTestFixture(fixture);
      sortTest.addInput(fixture.rowSetBuilder(schema)
          .build());
      sortTest.run();
    }
  }

  /**
   * Degenerate case: single row in single batch.
   */
  @Test
  public void testSingleRow() throws Exception {
    try (OperatorFixture fixture = OperatorFixture.standardFixture(dirTestWatcher)) {
      TupleMetadata schema = SortTestUtilities.nonNullSchema();
      SortTestFixture sortTest = new SortTestFixture(fixture);
      sortTest.addInput(fixture.rowSetBuilder(schema)
          .addRow(1, "first")
          .build());
      sortTest.addOutput(fixture.rowSetBuilder(schema)
          .addRow(1, "first")
          .build());
      sortTest.run();
    }
  }

  /**
   * Degenerate case: two (unsorted) rows in single batch
   */
  @Test
  public void testSingleBatch() throws Exception {
    try (OperatorFixture fixture = OperatorFixture.standardFixture(dirTestWatcher)) {
      TupleMetadata schema = SortTestUtilities.nonNullSchema();
      SortTestFixture sortTest = new SortTestFixture(fixture);
      sortTest.addInput(fixture.rowSetBuilder(schema)
          .addRow(2, "second")
          .addRow(1, "first")
          .build());
      sortTest.addOutput(fixture.rowSetBuilder(schema)
          .addRow(1, "first")
          .addRow(2, "second")
          .build());
      sortTest.run();
    }
  }

  /**
   * Degenerate case, one row in each of two (unsorted) batches.
   */
  @Test
  public void testTwoBatches() throws Exception {
    try (OperatorFixture fixture = OperatorFixture.standardFixture(dirTestWatcher)) {
      TupleMetadata schema = SortTestUtilities.nonNullSchema();
      SortTestFixture sortTest = new SortTestFixture(fixture);
      sortTest.addInput(fixture.rowSetBuilder(schema)
          .addRow(2, "second")
          .build());
      sortTest.addInput(fixture.rowSetBuilder(schema)
          .addRow(1, "first")
          .build());
      sortTest.addOutput(fixture.rowSetBuilder(schema)
          .addRow(1, "first")
          .addRow(2, "second")
          .build());
      sortTest.run();
    }
  }

  /**
   * Crude-but-effective data generator that produces pseudo-random data
   * that can be easily verified. The pseudo-random data is generate by the
   * simple means of incrementing a counter using a random value, and wrapping.
   * This ensures we visit each value twice, and that the sorted output will
   * be a continuous run of numbers in proper order.
   */
  public static class DataGenerator {
    private final OperatorFixture fixture;
    private final TupleMetadata schema;
    private final int targetCount;
    private final int batchSize;
    private final int step;
    private int rowCount;
    private int currentValue;

    public DataGenerator(OperatorFixture fixture, int targetCount, int batchSize) {
      this(fixture, targetCount, batchSize, 0, guessStep(targetCount));
    }

    public DataGenerator(OperatorFixture fixture, int targetCount, int batchSize, int seed, int step) {
      this.fixture = fixture;
      this.targetCount = targetCount;
      Preconditions.checkArgument(batchSize > 0 && batchSize <= ValueVector.MAX_ROW_COUNT);
      this.batchSize = batchSize;
      this.step = step;
      schema = SortTestUtilities.nonNullSchema();
      currentValue = seed;
    }

    /**
     * Pick a reasonable prime step based on data size.
     *
     * @param target number of rows to generate
     * @return the prime step size
     */
    private static int guessStep(int target) {
      if (target < 10) {
        return 7;
      } else if (target < 200) {
        return 71;
      } else if (target < 2000) {
        return 701;
      } else if (target < 20000) {
        return 7001;
      } else {
        return 17011;
      }
    }

    public RowSet nextRowSet() {
      if (rowCount == targetCount) {
        return null;
      }
      RowSetBuilder builder = fixture.rowSetBuilder(schema);
      int end = Math.min(batchSize, targetCount - rowCount);
      for (int i = 0; i < end; i++) {
        builder.addRow(currentValue, i + ", " + currentValue);
        currentValue = (currentValue + step) % targetCount;
        rowCount++;
      }
      return builder.build();
    }
  }

  /**
   * Validate a sort output batch based on the expectation that the key
   * is an ordered sequence of integers, split across multiple batches.
   */
  public static class DataValidator {
    private final int targetCount;
    private final int batchSize;
    private int batchCount;
    private int rowCount;

    public DataValidator(int targetCount, int batchSize) {
      this.targetCount = targetCount;
      Preconditions.checkArgument(batchSize > 0 && batchSize <= ValueVector.MAX_ROW_COUNT);
      this.batchSize = batchSize;
    }

    public void validate(RowSet output) {
      batchCount++;
      int expectedSize = Math.min(batchSize, targetCount - rowCount);
      assertEquals("Size of batch " + batchCount, expectedSize, output.rowCount());
      RowSetReader reader = output.reader();
      while (reader.next()) {
        assertEquals("Value of " + batchCount + ":" + rowCount,
            rowCount, reader.scalar(0).getInt());
        rowCount++;
      }
    }

    public void validateDone() {
      assertEquals("Wrong row count", targetCount, rowCount);
    }
  }

  /**
   * Run a full-blown sort test with multiple input batches. Because we want to
   * generate multiple inputs, we don't create them statically. Instead, we generate
   * them on the fly using a data generator. A matching data validator verifies the
   * output. Here, we are focusing on overall test flow. Separate, detailed, unit
   * tests have already probed the details of each sort component and data type,
   * so we don't need to repeat that whole exercise here; using integer keys is
   * sufficient.
   *
   * @param fixture the operator test fixture
   * @param dataGen input batch generator
   * @param validator validates output batches
   */
  public void runLargeSortTest(OperatorFixture fixture, DataGenerator dataGen,
                               DataValidator validator) {
    SortImpl sort = makeSortImpl(fixture, Ordering.ORDER_ASC, Ordering.NULLS_UNSPECIFIED);

    int batchCount = 0;
    RowSet input;
    while ((input = dataGen.nextRowSet()) != null) {
      batchCount++;
      if (batchCount == 1) {
        // Simulates a NEW_SCHEMA event

        sort.setSchema(input.container().getSchema());
      }

      // Simulates an OK event

      sort.addBatch(input.vectorAccessible());
    }

    // Simulate returning results

    SortResults results = sort.startMerge();
    if (results.getContainer() != dest) {
      dest.clear();
      dest = results.getContainer();
    }
    while (results.next()) {
      RowSet output = toRowSet(results, dest);
      validator.validate(output);
    }
    validator.validateDone();
    results.close();
    dest.clear();
    sort.close();
    sort.opContext().close();
  }

  /**
   * Set up and run a test for "jumbo" batches, and time the run.
   * @param fixture operator test fixture
   * @param rowCount number of rows to test
   */
  public void runJumboBatchTest(OperatorFixture fixture, int rowCount) {
    DataGenerator dataGen = new DataGenerator(fixture, rowCount, ValueVector.MAX_ROW_COUNT);
    DataValidator validator = new DataValidator(rowCount, ValueVector.MAX_ROW_COUNT);
    runLargeSortTest(fixture, dataGen, validator);
  }

  /**
   * Most tests have used small row counts because we want to probe specific bits
   * of interest. Try 1000 rows just to ensure things work
   */
  @Test
  public void testModerateBatch() throws Exception {
    try (OperatorFixture fixture = OperatorFixture.standardFixture(dirTestWatcher)) {
      runJumboBatchTest(fixture, 1000);
    }
  }

  /**
   * Hit the sort with the largest possible batch size to ensure nothing is lost
   * at the edges.
   */
  @Test
  public void testLargeBatch() throws Exception {
    try (OperatorFixture fixture = OperatorFixture.standardFixture(dirTestWatcher)) {
      runJumboBatchTest(fixture, ValueVector.MAX_ROW_COUNT);
    }
  }

  /**
   * Use this function to pre-load Netty's free list with a large
   * number of "dirty" blocks. This will often catch error due to
   * failure to initialize value vector memory.
   *
   * @param allocator - used for allocating Drillbuf
   */
  @SuppressWarnings("unused")
  private void partyOnMemory(BufferAllocator allocator) {
    DrillBuf bufs[] = new DrillBuf[10];
    for (int i = 0; i < bufs.length; i++) {
      bufs[i] = allocator.buffer(ValueVector.MAX_BUFFER_SIZE);
      for (int j = 0; j < ValueVector.MAX_BUFFER_SIZE; j += 4) {
        bufs[i].setInt(j, 0xDEADBEEF);
      }
    }
    for (int i = 0; i < bufs.length; i++) {
      bufs[i].release();
    }
  }

  /**
   * Run a test using wide rows. This stresses the "copier" portion of the sort
   * and allows us to test the original generated copier and the revised "generic"
   * copier.
   *
   * @param fixture operator test fixture
   * @param colCount number of data (non-key) columns
   * @param rowCount number of rows to generate
   */
  public void runWideRowsTest(OperatorFixture fixture, int colCount, int rowCount) {
    SchemaBuilder builder = new SchemaBuilder()
        .add("key", MinorType.INT);
    for (int i = 0; i < colCount; i++) {
      builder.add("col" + (i+1), MinorType.INT);
    }
    TupleMetadata schema = builder.buildSchema();
    ExtendableRowSet rowSet = fixture.rowSet(schema);
    RowSetWriter writer = rowSet.writer(rowCount);
    for (int i = 0; i < rowCount; i++) {
      writer.set(0, i);
      for (int j = 0; j < colCount; j++) {
        writer.set(j + 1, i * 100_000 + j);
      }
      writer.save();
    }
    writer.done();

    SortImpl sort = makeSortImpl(fixture, Ordering.ORDER_ASC, Ordering.NULLS_UNSPECIFIED);
    sort.setSchema(rowSet.container().getSchema());
    sort.addBatch(rowSet.vectorAccessible());
    SortResults results = sort.startMerge();
    if (results.getContainer() != dest) {
      dest.clear();
      dest = results.getContainer();
    }
    assertTrue(results.next());
    assertFalse(results.next());
    results.close();
    dest.clear();
    sort.close();
    sort.opContext().close();
  }

  /**
   * Test wide rows with the stock copier.
   */
  @Test
  public void testWideRows() throws Exception {
    try (OperatorFixture fixture = OperatorFixture.standardFixture(dirTestWatcher)) {
      runWideRowsTest(fixture, 1000, ValueVector.MAX_ROW_COUNT);
    }
  }

  /**
   * Force the sorter to spill, and verify that the resulting data
   * is correct. Uses a specific property of the sort to set the
   * in-memory batch limit so that we don't have to fiddle with filling
   * up memory. The point here is not to test the code that decides when
   * to spill (that was already tested.) Nor to test the spilling
   * mechanism itself (that has also already been tested.) Rather it is
   * to ensure that, when those components are integrated into the
   * sort implementation, that the whole assembly does the right thing.
   */
  @Test
  public void testSpill() throws Exception {
    OperatorFixture.Builder builder = OperatorFixture.builder(dirTestWatcher);
    builder.configBuilder()
      .put(ExecConstants.EXTERNAL_SORT_BATCH_LIMIT, 2);
    try (OperatorFixture fixture = builder.build()) {
      TupleMetadata schema = SortTestUtilities.nonNullSchema();
      SortTestFixture sortTest = new SortTestFixture(fixture) {
        @Override
        protected void validateSort(SortImpl sort) {
          assertEquals(1, sort.getMetrics().getSpillCount());
          assertEquals(0, sort.getMetrics().getMergeCount());
          assertEquals(2, sort.getMetrics().getPeakBatchCount());
        }
        @Override
        protected void validateFinalStats(SortImpl sort) {
          assertTrue(sort.getMetrics().getWriteBytes() > 0);
        }
      };
      sortTest.addInput(fixture.rowSetBuilder(schema)
          .addRow(2, "second")
          .build());
      sortTest.addInput(fixture.rowSetBuilder(schema)
          .addRow(3, "third")
          .build());
      sortTest.addInput(fixture.rowSetBuilder(schema)
          .addRow(1, "first")
          .build());
      sortTest.addOutput(fixture.rowSetBuilder(schema)
          .addRow(1, "first")
          .addRow(2, "second")
          .addRow(3, "third")
          .build());
      sortTest.run();
    }
  }
}
