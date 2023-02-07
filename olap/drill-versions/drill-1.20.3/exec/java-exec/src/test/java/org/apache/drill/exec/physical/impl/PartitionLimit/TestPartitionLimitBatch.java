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
package org.apache.drill.exec.physical.impl.PartitionLimit;

import org.apache.drill.categories.OperatorTest;
import org.apache.drill.exec.physical.config.PartitionLimit;
import org.apache.drill.exec.physical.impl.BaseTestOpBatchEmitOutcome;
import org.apache.drill.exec.physical.impl.MockRecordBatch;
import org.apache.drill.exec.physical.impl.limit.PartitionLimitRecordBatch;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.physical.rowSet.IndirectRowSet;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.test.rowSet.RowSetComparison;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

@Category(OperatorTest.class)
public class TestPartitionLimitBatch extends BaseTestOpBatchEmitOutcome {

  private static String PARTITION_COLUMN;

  // Holds reference to actual operator instance created for each tests
  private static PartitionLimitRecordBatch limitBatch;

  // Lits of expected outcomes populated by each tests. Used to verify actual IterOutcome returned with next call on
  // operator to expected outcome
  private final List<RecordBatch.IterOutcome> expectedOutcomes = new ArrayList<>();

  // List of expected row counts populated by each tests. Used to verify actual output row count to expected row count
  private final List<Integer> expectedRecordCounts = new ArrayList<>();

  // List of expected row sets populated by each tests. Used to verify actual output from operator to expected output
  private final List<RowSet> expectedRowSets = new ArrayList<>();

  @BeforeClass
  public static void partitionLimitSetup() {
    PARTITION_COLUMN = inputSchema.column(0).getName();
  }

  /**
   * Cleanup method executed post each test
   */
  @After
  public void afterTestCleanup() {
    // close limitBatch
    limitBatch.close();

    // Release memory from expectedRowSets
    for (RowSet expectedRowSet : expectedRowSets) {
      expectedRowSet.clear();
    }
    expectedOutcomes.clear();
    expectedRecordCounts.clear();
    expectedRowSets.clear();
  }

  /**
   * Common method used by all the tests for {@link PartitionLimitRecordBatch} below. It creates the MockRecordBatch
   * and {@link PartitionLimitRecordBatch} with the populated containers and outcomes list in the test. It also
   * verifies the expected outcomes list and record count populated by each test against each next() call to
   * {@link PartitionLimitRecordBatch}. For cases when the expected record count is >0 it verifies the actual output
   * returned by {@link PartitionLimitRecordBatch} with expected output rows.
   * @param start - Start offset for {@link PartitionLimit} PopConfig
   * @param end - End offset for {@link PartitionLimit} PopConfig
   */
  private void testPartitionLimitCommon(Integer start, Integer end) {
    final MockRecordBatch mockInputBatch = new MockRecordBatch(operatorFixture.getFragmentContext(), opContext,
      inputContainer, inputOutcomes, inputContainerSv2, inputContainer.get(0).getSchema());

    final PartitionLimit limitConf = new PartitionLimit(null, start, end, PARTITION_COLUMN);
    limitBatch = new PartitionLimitRecordBatch(limitConf, operatorFixture.getFragmentContext(), mockInputBatch);

    int i=0;
    int expectedRowSetIndex = 0;
    while (i < expectedOutcomes.size()) {
      try {
        assertTrue(expectedOutcomes.get(i) == limitBatch.next());
        assertTrue(expectedRecordCounts.get(i++) == limitBatch.getRecordCount());

        if (limitBatch.getRecordCount() > 0) {
          final RowSet actualRowSet = IndirectRowSet.fromSv2(limitBatch.getContainer(),
            limitBatch.getSelectionVector2());
          new RowSetComparison(expectedRowSets.get(expectedRowSetIndex++)).verify(actualRowSet);
        }
      } finally {
        limitBatch.getSelectionVector2().clear();
        limitBatch.getContainer().zeroVectors();
      }
    }
  }

  /**
   * Verifies that empty batch with both OK_NEW_SCHEMA and EMIT outcome is not ignored by
   * {@link PartitionLimitRecordBatch} and is passed to the downstream operator.
   */
  @Test
  public void testPartitionLimit_EmptyBatchEmitOutcome() {
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(emptyInputRowSet.container());

    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);

    expectedOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    expectedOutcomes.add(RecordBatch.IterOutcome.EMIT);

    expectedRecordCounts.add(0);
    expectedRecordCounts.add(0);

    testPartitionLimitCommon(0, 1);
  }

  /**
   * Verifies {@link PartitionLimitRecordBatch} considers all the batch until it sees EMIT outcome and return output
   * batch with data that meets the {@link PartitionLimitRecordBatch} criteria.
   */
  @Test
  public void testPartitionLimit_NonEmptyBatchEmitOutcome() {
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(nonEmptyInputRowSet.container());

    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);

    expectedOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    expectedOutcomes.add(RecordBatch.IterOutcome.EMIT);

    expectedRecordCounts.add(0);
    expectedRecordCounts.add(1);

    RowSet expectedBatch =  operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 10, "item1")
      .build();
    expectedRowSets.add(expectedBatch);

    testPartitionLimitCommon(0, 1);
  }

  /**
   * Verifies that {@link PartitionLimitRecordBatch} batch operates on batches across EMIT boundary with fresh
   * configuration. That is it considers partition column data separately for batches across EMIT boundary.
   */
  @Test
  public void testPartitionLimit_ResetsAfterFirstEmitOutcome() {
    final RowSet.SingleRowSet nonEmptyInputRowSet2 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 100, "item100")
      .addRow(1, 101, "item101")
      .addRow(1, 102, "item102")
      .addRow(2, 200, "item200")
      .build();

    final RowSet expectedRowSet1 =  operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 10, "item1")
      .build();
    final RowSet expectedRowSet2 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 100, "item100")
      .addRow(2, 200, "item200")
      .build();

    inputContainer.add(nonEmptyInputRowSet.container());
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(nonEmptyInputRowSet2.container());

    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);
    inputOutcomes.add(RecordBatch.IterOutcome.OK);


    expectedOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    expectedOutcomes.add(RecordBatch.IterOutcome.EMIT);
    expectedOutcomes.add(RecordBatch.IterOutcome.OK);
    expectedOutcomes.add(RecordBatch.IterOutcome.NONE);

    expectedRecordCounts.add(1);
    expectedRecordCounts.add(0);
    // Since in this input batch there is 2 different partitionId
    expectedRecordCounts.add(2);
    expectedRecordCounts.add(0);

    expectedRowSets.add(expectedRowSet1);
    expectedRowSets.add(expectedRowSet2);

    testPartitionLimitCommon(0, 1);
  }

  /**
   * Verifies that when the {@link PartitionLimitRecordBatch} number of records is found with first incoming batch,
   * then next empty incoming batch with OK outcome is ignored, but the empty EMIT outcome batch is not ignored.
   * Empty incoming batch with EMIT outcome produces empty output batch with EMIT outcome.
   */
  @Test
  public void testPartitionLimit_NonEmptyFirst_EmptyOKEmitOutcome() {
    inputContainer.add(nonEmptyInputRowSet.container());
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(emptyInputRowSet.container());

    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomes.add(RecordBatch.IterOutcome.OK);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);
    inputOutcomes.add(RecordBatch.IterOutcome.NONE);

    expectedOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    expectedOutcomes.add(RecordBatch.IterOutcome.EMIT);
    expectedOutcomes.add(RecordBatch.IterOutcome.NONE);

    expectedRecordCounts.add(1);
    expectedRecordCounts.add(0);
    expectedRecordCounts.add(0);

    final RowSet expectedRowSet1 =  operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 10, "item1")
      .build();
    expectedRowSets.add(expectedRowSet1);

    testPartitionLimitCommon(0, 1);
  }

  /**
   * Verifies that {@link PartitionLimitRecordBatch} refreshes it's state after seeing first EMIT outcome and works on
   * data batches following it as new set's of incoming batch and apply the partition limit rule from fresh on those.
   * So for first set of batches with OK_NEW_SCHEMA and EMIT outcome the total number of records received being less
   * than limit condition, it still produces an output with that many records for each partition key (in this case 1
   * even though limit number of records is 2).
   *
   * After seeing EMIT, it refreshes it's state and operate on next input batches to again return limit number of
   * records per partition id. So for 3rd batch with 6 records and 3 partition id and with EMIT outcome it produces an
   * output batch with <=2 records for each partition id.
   */
  @Test
  public void testPartitionLimit_AcrossEmitOutcome() {
    final RowSet.SingleRowSet nonEmptyInputRowSet2 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 100, "item100")
      .addRow(1, 101, "item101")
      .addRow(1, 102, "item102")
      .addRow(2, 200, "item200")
      .addRow(3, 300, "item300")
      .addRow(3, 301, "item301")
      .build();

    final RowSet expectedRows1 =  operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 10, "item1")
      .build();

    final RowSet.SingleRowSet expectedRows2 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 100, "item100")
      .addRow(1, 101, "item101")
      .addRow(2, 200, "item200")
      .addRow(3, 300, "item300")
      .addRow(3, 301, "item301")
      .build();

    inputContainer.add(nonEmptyInputRowSet.container());
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(nonEmptyInputRowSet2.container());
    inputContainer.add(emptyInputRowSet.container());

    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);

    expectedOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    expectedOutcomes.add(RecordBatch.IterOutcome.EMIT);
    expectedOutcomes.add(RecordBatch.IterOutcome.EMIT);
    expectedOutcomes.add(RecordBatch.IterOutcome.NONE);

    expectedRecordCounts.add(expectedRows1.rowCount());
    expectedRecordCounts.add(0);
    expectedRecordCounts.add(expectedRows2.rowCount());
    expectedRecordCounts.add(0);

    expectedRowSets.add(expectedRows1);
    expectedRowSets.add(expectedRows2);

    testPartitionLimitCommon(0, 2);
  }

  /**
   * Verifies that {@link PartitionLimitRecordBatch} considers same partition id across batches but within EMIT
   * boundary to impose limit condition.
   */
  @Test
  public void testPartitionLimit_PartitionIdSpanningAcrossBatches() {

    final RowSet.SingleRowSet nonEmptyInputRowSet2 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 100, "item100")
      .addRow(1, 101, "item101")
      .addRow(1, 102, "item102")
      .addRow(1, 103, "item103")
      .addRow(2, 200, "item200")
      .addRow(2, 201, "item201")
      .addRow(2, 202, "item202")
      .build();

    final RowSet expectedRowSet1 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 10, "item1")
      .build();

    final RowSet expectedRowSet2 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(2, 200, "item200")
      .build();

    inputContainer.add(nonEmptyInputRowSet.container());
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(nonEmptyInputRowSet2.container());
    inputContainer.add(emptyInputRowSet.container());

    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomes.add(RecordBatch.IterOutcome.OK);
    inputOutcomes.add(RecordBatch.IterOutcome.OK);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);

    expectedOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    // second OK batch is consumed by abstractRecordBatch since it's empty
    expectedOutcomes.add(RecordBatch.IterOutcome.OK);
    expectedOutcomes.add(RecordBatch.IterOutcome.EMIT);

    expectedRecordCounts.add(1);
    expectedRecordCounts.add(1);
    expectedRecordCounts.add(0);

    expectedRowSets.add(expectedRowSet1);
    expectedRowSets.add(expectedRowSet2);

    testPartitionLimitCommon(0, 1);
  }

  @Test
  public void testPartitionLimit_PartitionIdSpanningAcrossBatches_WithOffset() {
    final RowSet.SingleRowSet nonEmptyInputRowSet2 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 100, "item100")
      .addRow(1, 101, "item101")
      .addRow(1, 102, "item102")
      .addRow(1, 103, "item103")
      .addRow(2, 200, "item200")
      .addRow(2, 201, "item201")
      .addRow(2, 202, "item202")
      .build();

    final RowSet expectedRowSet1 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 101, "item101")
      .addRow(2, 202, "item202")
      .build();

    inputContainer.add(nonEmptyInputRowSet.container());
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(nonEmptyInputRowSet2.container());
    inputContainer.add(emptyInputRowSet.container());

    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomes.add(RecordBatch.IterOutcome.OK);
    inputOutcomes.add(RecordBatch.IterOutcome.OK);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);

    expectedOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    // second OK batch is consumed by abstractRecordBatch since it's empty
    expectedOutcomes.add(RecordBatch.IterOutcome.OK);
    expectedOutcomes.add(RecordBatch.IterOutcome.EMIT);

    expectedRecordCounts.add(0);
    expectedRecordCounts.add(2);
    expectedRecordCounts.add(0);

    expectedRowSets.add(expectedRowSet1);

    testPartitionLimitCommon(2, 3);
  }

  /**
   * Verifies {@link PartitionLimitRecordBatch} works correctly in cases a partition id spans across batches and
   * limit condition is met by picking records from multiple batch for same partition id.
   */
  @Test
  public void testPartitionLimit_PartitionIdSelectedAcrossBatches() {
    final RowSet.SingleRowSet nonEmptyInputRowSet2 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 100, "item100")
      .addRow(1, 101, "item101")
      .addRow(1, 102, "item102")
      .addRow(1, 103, "item103")
      .addRow(2, 200, "item200")
      .addRow(2, 201, "item201")
      .addRow(2, 202, "item202")
      .build();

    final RowSet expectedRowSet1 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 10, "item1")
      .build();

    final RowSet expectedRowSet2 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 100, "item100")
      .addRow(1, 101, "item101")
      .addRow(1, 102, "item102")
      .addRow(1, 103, "item103")
      .addRow(2, 200, "item200")
      .addRow(2, 201, "item201")
      .addRow(2, 202, "item202")
      .build();

    inputContainer.add(nonEmptyInputRowSet.container());
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(nonEmptyInputRowSet2.container());
    inputContainer.add(emptyInputRowSet.container());

    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomes.add(RecordBatch.IterOutcome.OK);
    inputOutcomes.add(RecordBatch.IterOutcome.OK);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);

    expectedOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    // second OK batch is consumed by abstractRecordBatch since it's empty
    expectedOutcomes.add(RecordBatch.IterOutcome.OK);
    expectedOutcomes.add(RecordBatch.IterOutcome.EMIT);

    expectedRecordCounts.add(expectedRowSet1.rowCount());
    expectedRecordCounts.add(expectedRowSet2.rowCount());
    expectedRecordCounts.add(0);

    expectedRowSets.add(expectedRowSet1);
    expectedRowSets.add(expectedRowSet2);

    testPartitionLimitCommon(0, 5);
  }

  /**
   * Verifies {@link PartitionLimitRecordBatch} works correctly in cases where start offset is such that all the
   * records of a partition id is ignored but records in other partition id is selected.
   */
  @Test
  public void testPartitionLimit_IgnoreOnePartitionIdWithOffset() {
    final RowSet.SingleRowSet nonEmptyInputRowSet2 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 100, "item100")
      .addRow(1, 101, "item101")
      .addRow(1, 102, "item102")
      .addRow(1, 103, "item103")
      .addRow(2, 200, "item200")
      .addRow(2, 201, "item201")
      .addRow(2, 202, "item202")
      .build();

    final RowSet expectedRowSet1 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 102, "item102")
      .addRow(1, 103, "item103")
      .build();

    inputContainer.add(nonEmptyInputRowSet.container());
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(nonEmptyInputRowSet2.container());
    inputContainer.add(emptyInputRowSet.container());

    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomes.add(RecordBatch.IterOutcome.OK);
    inputOutcomes.add(RecordBatch.IterOutcome.OK);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);

    expectedOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    // second OK batch is consumed by abstractRecordBatch since it's empty
    expectedOutcomes.add(RecordBatch.IterOutcome.OK);
    expectedOutcomes.add(RecordBatch.IterOutcome.EMIT);

    expectedRecordCounts.add(0);
    expectedRecordCounts.add(expectedRowSet1.rowCount());
    expectedRecordCounts.add(0);

    expectedRowSets.add(expectedRowSet1);

    testPartitionLimitCommon(3, 5);
  }

  @Test
  public void testPartitionLimit_LargeOffsetIgnoreAllRecords() {
    final RowSet.SingleRowSet nonEmptyInputRowSet2 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 100, "item100")
      .addRow(1, 101, "item101")
      .addRow(1, 102, "item102")
      .addRow(1, 103, "item103")
      .addRow(2, 200, "item200")
      .addRow(2, 201, "item201")
      .addRow(2, 202, "item202")
      .build();

    inputContainer.add(nonEmptyInputRowSet.container());
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(nonEmptyInputRowSet2.container());
    inputContainer.add(emptyInputRowSet.container());

    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomes.add(RecordBatch.IterOutcome.OK);
    inputOutcomes.add(RecordBatch.IterOutcome.OK);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);

    expectedOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    // second OK batch is consumed by abstractRecordBatch since it's empty
    expectedOutcomes.add(RecordBatch.IterOutcome.OK);
    expectedOutcomes.add(RecordBatch.IterOutcome.EMIT);

    expectedRecordCounts.add(0);
    expectedRecordCounts.add(0);
    expectedRecordCounts.add(0);

    testPartitionLimitCommon(5, 6);
  }

  /**
   * Verifies {@link PartitionLimitRecordBatch} works correctly when start and end offset is same. In this case it
   * works as Limit 0 scenario where it will not output any rows for any partition id across batches.
   */
  @Test
  public void testPartitionLimit_Limit0() {
    final RowSet.SingleRowSet nonEmptyInputRowSet2 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 100, "item100")
      .addRow(1, 101, "item101")
      .addRow(1, 102, "item102")
      .addRow(1, 103, "item103")
      .addRow(2, 200, "item200")
      .addRow(2, 201, "item201")
      .addRow(2, 202, "item202")
      .build();

    inputContainer.add(nonEmptyInputRowSet.container());
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(nonEmptyInputRowSet2.container());
    inputContainer.add(emptyInputRowSet.container());

    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomes.add(RecordBatch.IterOutcome.OK);
    inputOutcomes.add(RecordBatch.IterOutcome.OK);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);

    expectedOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    // second OK batch is consumed by abstractRecordBatch since it's empty
    expectedOutcomes.add(RecordBatch.IterOutcome.OK);
    expectedOutcomes.add(RecordBatch.IterOutcome.EMIT);

    expectedRecordCounts.add(0);
    expectedRecordCounts.add(0);
    expectedRecordCounts.add(0);

    testPartitionLimitCommon(0, 0);
  }

  /**
   * Verifies {@link PartitionLimitRecordBatch} works correctly for cases where no end offset is mentioned. This
   * necessary means selecting all the records in a partition.
   */
  @Test
  public void testPartitionLimit_NoLimit() {
    final RowSet.SingleRowSet nonEmptyInputRowSet2 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 100, "item100")
      .addRow(1, 101, "item101")
      .addRow(1, 102, "item102")
      .addRow(1, 103, "item103")
      .addRow(2, 200, "item200")
      .addRow(2, 201, "item201")
      .addRow(2, 202, "item202")
      .build();

    final RowSet expectedRowSet1 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 10, "item1")
      .build();

    final RowSet expectedRowSet2 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 100, "item100")
      .addRow(1, 101, "item101")
      .addRow(1, 102, "item102")
      .addRow(1, 103, "item103")
      .addRow(2, 200, "item200")
      .addRow(2, 201, "item201")
      .addRow(2, 202, "item202")
      .build();

    inputContainer.add(nonEmptyInputRowSet.container());
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(nonEmptyInputRowSet2.container());
    inputContainer.add(emptyInputRowSet.container());

    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomes.add(RecordBatch.IterOutcome.OK);
    inputOutcomes.add(RecordBatch.IterOutcome.OK);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);

    expectedOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    // second OK batch is consumed by abstractRecordBatch since it's empty
    expectedOutcomes.add(RecordBatch.IterOutcome.OK);
    expectedOutcomes.add(RecordBatch.IterOutcome.EMIT);

    expectedRecordCounts.add(expectedRowSet1.rowCount());
    expectedRecordCounts.add(expectedRowSet2.rowCount());
    expectedRecordCounts.add(0);

    expectedRowSets.add(expectedRowSet1);
    expectedRowSets.add(expectedRowSet2);

    testPartitionLimitCommon(0, null);
  }

  /**
   * Verifies {@link PartitionLimitRecordBatch} takes care of provided negative start offset correctly
   */
  @Test
  public void testPartitionLimit_NegativeOffset() {
    final RowSet.SingleRowSet nonEmptyInputRowSet2 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 100, "item100")
      .addRow(1, 101, "item101")
      .addRow(1, 102, "item102")
      .addRow(1, 103, "item103")
      .addRow(2, 200, "item200")
      .addRow(2, 201, "item201")
      .addRow(2, 202, "item202")
      .build();

    final RowSet expectedRowSet1 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 10, "item1")
      .build();

    final RowSet expectedRowSet2 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 100, "item100")
      .addRow(2, 200, "item200")
      .addRow(2, 201, "item201")
      .build();

    inputContainer.add(nonEmptyInputRowSet.container());
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(nonEmptyInputRowSet2.container());
    inputContainer.add(emptyInputRowSet.container());

    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomes.add(RecordBatch.IterOutcome.OK);
    inputOutcomes.add(RecordBatch.IterOutcome.OK);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);

    expectedOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    // second OK batch is consumed by abstractRecordBatch since it's empty
    expectedOutcomes.add(RecordBatch.IterOutcome.OK);
    expectedOutcomes.add(RecordBatch.IterOutcome.EMIT);

    expectedRecordCounts.add(expectedRowSet1.rowCount());
    expectedRecordCounts.add(expectedRowSet2.rowCount());
    expectedRecordCounts.add(0);

    expectedRowSets.add(expectedRowSet1);
    expectedRowSets.add(expectedRowSet2);

    testPartitionLimitCommon(-5, 2);
  }

  /**
   * Verifies {@link PartitionLimitRecordBatch} behaves correctly across EMIT boundary with single or multiple
   * batches within each EMIT boundary. It resets it states correctly across EMIT boundary and then operates on all
   * the batches within EMIT boundary at a time.
   */
  @Test
  public void testPartitionLimit_MultipleEmit_SingleMultipleBatch() {
    final RowSet.SingleRowSet nonEmptyInputRowSet2 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 100, "item100")
      .addRow(1, 101, "item101")
      .addRow(1, 102, "item102")
      .addRow(1, 103, "item103")
      .addRow(2, 200, "item200")
      .addRow(2, 201, "item201")
      .addRow(2, 202, "item202")
      .build();

    // Second EMIT boundary batches
    final RowSet.SingleRowSet nonEmptyInputRowSet3 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 1001, "item1001")
      .addRow(1, 1002, "item1002")
      .addRow(1, 1003, "item1003")
      .addRow(2, 2000, "item2000")
      .addRow(2, 2001, "item2001")
      .addRow(2, 2002, "item2002")
      .build();

    final RowSet.SingleRowSet nonEmptyInputRowSet4 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(3, 3001, "item3001")
      .addRow(3, 3002, "item3002")
      .addRow(3, 3003, "item3003")
      .addRow(4, 4000, "item4000")
      .addRow(4, 4001, "item4001")
      .build();

    // Third EMIT boundary batches
    final RowSet.SingleRowSet nonEmptyInputRowSet5 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 10001, "item10001")
      .addRow(1, 10002, "item10002")
      .addRow(1, 10003, "item10003")
      .build();

    // First EMIT boundary expected rowsets
    final RowSet expectedRowSet1 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 10, "item1")
      .build();

    final RowSet expectedRowSet2 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 100, "item100")
      .addRow(2, 200, "item200")
      .addRow(2, 201, "item201")
      .build();

    // Second EMIT boundary expected rowsets
    final RowSet expectedRowSet3 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 1001, "item1001")
      .addRow(1, 1002, "item1002")
      .addRow(2, 2000, "item2000")
      .addRow(2, 2001, "item2001")
      .build();

    final RowSet expectedRowSet4 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(3, 3001, "item3001")
      .addRow(3, 3002, "item3002")
      .addRow(4, 4000, "item4000")
      .addRow(4, 4001, "item4001")
      .build();

    // Third EMIT boundary expected rowsets
    final RowSet expectedRowSet5 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 10001, "item10001")
      .addRow(1, 10002, "item10002")
      .build();

    inputContainer.add(nonEmptyInputRowSet.container());
    inputContainer.add(nonEmptyInputRowSet2.container());
    inputContainer.add(nonEmptyInputRowSet3.container());
    inputContainer.add(nonEmptyInputRowSet4.container());
    inputContainer.add(nonEmptyInputRowSet5.container());
    inputContainer.add(emptyInputRowSet.container());

    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);
    inputOutcomes.add(RecordBatch.IterOutcome.OK);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);

    expectedOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    expectedOutcomes.add(RecordBatch.IterOutcome.EMIT);
    expectedOutcomes.add(RecordBatch.IterOutcome.OK);
    expectedOutcomes.add(RecordBatch.IterOutcome.EMIT);
    expectedOutcomes.add(RecordBatch.IterOutcome.EMIT);
    expectedOutcomes.add(RecordBatch.IterOutcome.EMIT);

    expectedRecordCounts.add(expectedRowSet1.rowCount());
    expectedRecordCounts.add(expectedRowSet2.rowCount());
    expectedRecordCounts.add(expectedRowSet3.rowCount());
    expectedRecordCounts.add(expectedRowSet4.rowCount());
    expectedRecordCounts.add(expectedRowSet5.rowCount());
    expectedRecordCounts.add(0);

    expectedRowSets.add(expectedRowSet1);
    expectedRowSets.add(expectedRowSet2);
    expectedRowSets.add(expectedRowSet3);
    expectedRowSets.add(expectedRowSet4);
    expectedRowSets.add(expectedRowSet5);

    testPartitionLimitCommon(-5, 2);
  }

  /**
   * Verifies {@link PartitionLimitRecordBatch} behaves correctly across EMIT boundary with single or multiple
   * batches (with sv2) within each EMIT boundary. It resets it states correctly across EMIT boundary and then
   * operates on all the batches within EMIT boundary at a time.
   */
  @Test
  public void testPartitionLimit_MultipleEmit_SingleMultipleBatch_WithSV2() {
    final RowSet.SingleRowSet emptyWithSv2 = operatorFixture.rowSetBuilder(inputSchema)
      .withSv2()
      .build();

    final RowSet.SingleRowSet nonEmptyInputRowSet2 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 100, "item100")
      .addRow(1, 101, "item101")
      .addRow(1, 102, "item102")
      .addRow(1, 103, "item103")
      .addRow(2, 200, "item200")
      .addRow(2, 201, "item201")
      .addRow(2, 202, "item202")
      .withSv2()
      .build();

    // Second EMIT boundary batches
    final RowSet.SingleRowSet nonEmptyInputRowSet3 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 1001, "item1001")
      .addRow(1, 1002, "item1002")
      .addRow(1, 1003, "item1003")
      .addRow(2, 2000, "item2000")
      .addRow(2, 2001, "item2001")
      .addRow(2, 2002, "item2002")
      .withSv2()
      .build();

    final RowSet.SingleRowSet nonEmptyInputRowSet4 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(3, 3001, "item3001")
      .addRow(3, 3002, "item3002")
      .addRow(3, 3003, "item3003")
      .addRow(4, 4000, "item4000")
      .addRow(4, 4001, "item4001")
      .withSv2()
      .build();

    // Third EMIT boundary batches
    final RowSet.SingleRowSet nonEmptyInputRowSet5 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 10001, "item10001")
      .addRow(1, 10002, "item10002")
      .addRow(1, 10003, "item10003")
      .withSv2()
      .build();

    // First EMIT boundary expected row sets
    final RowSet expectedRowSet2 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 100, "item100")
      .addRow(1, 101, "item101")
      .addRow(2, 200, "item200")
      .addRow(2, 201, "item201")
      .build();

    // Second EMIT boundary expected row sets
    final RowSet expectedRowSet3 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 1001, "item1001")
      .addRow(1, 1002, "item1002")
      .addRow(2, 2000, "item2000")
      .addRow(2, 2001, "item2001")
      .build();

    final RowSet expectedRowSet4 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(3, 3001, "item3001")
      .addRow(3, 3002, "item3002")
      .addRow(4, 4000, "item4000")
      .addRow(4, 4001, "item4001")
      .build();

    // Third EMIT boundary expected row sets
    final RowSet expectedRowSet5 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 10001, "item10001")
      .addRow(1, 10002, "item10002")
      .build();

    inputContainer.add(emptyWithSv2.container());
    inputContainer.add(nonEmptyInputRowSet2.container());
    inputContainer.add(nonEmptyInputRowSet3.container());
    inputContainer.add(nonEmptyInputRowSet4.container());
    inputContainer.add(nonEmptyInputRowSet5.container());
    inputContainer.add(emptyWithSv2.container());

    inputContainerSv2.add(emptyWithSv2.getSv2());
    inputContainerSv2.add(nonEmptyInputRowSet2.getSv2());
    inputContainerSv2.add(nonEmptyInputRowSet3.getSv2());
    inputContainerSv2.add(nonEmptyInputRowSet4.getSv2());
    inputContainerSv2.add(nonEmptyInputRowSet5.getSv2());
    inputContainerSv2.add(emptyWithSv2.getSv2());

    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);
    inputOutcomes.add(RecordBatch.IterOutcome.OK);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);

    expectedOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    expectedOutcomes.add(RecordBatch.IterOutcome.EMIT);
    expectedOutcomes.add(RecordBatch.IterOutcome.OK);
    expectedOutcomes.add(RecordBatch.IterOutcome.EMIT);
    expectedOutcomes.add(RecordBatch.IterOutcome.EMIT);
    expectedOutcomes.add(RecordBatch.IterOutcome.EMIT);

    expectedRecordCounts.add(0);
    expectedRecordCounts.add(expectedRowSet2.rowCount());
    expectedRecordCounts.add(expectedRowSet3.rowCount());
    expectedRecordCounts.add(expectedRowSet4.rowCount());
    expectedRecordCounts.add(expectedRowSet5.rowCount());
    expectedRecordCounts.add(0);

    expectedRowSets.add(expectedRowSet2);
    expectedRowSets.add(expectedRowSet3);
    expectedRowSets.add(expectedRowSet4);
    expectedRowSets.add(expectedRowSet5);

    testPartitionLimitCommon(-5, 2);
  }

  /**
   * Verifies {@link PartitionLimitRecordBatch} behaves correctly across EMIT boundary with single or multiple
   * batches (with sv2) within each EMIT boundary. It resets it states correctly across EMIT boundary and then
   * operates on all the batches within EMIT boundary at a time.
   */
  @Test
  public void testPartitionLimit_MultipleEmit_SingleMultipleBatch_WithSV2_FilteredRows() {
    final RowSet.SingleRowSet emptyWithSv2 = operatorFixture.rowSetBuilder(inputSchema)
      .withSv2()
      .build();

    final RowSet.SingleRowSet nonEmptyInputRowSet2 = operatorFixture.rowSetBuilder(inputSchema)
      .addSelection(false, 1, 100, "item100")
      .addSelection(true, 1, 101, "item101")
      .addSelection(false, 1, 102, "item102")
      .addSelection(true, 1, 103, "item103")
      .addSelection(false, 2, 200, "item200")
      .addSelection(true, 2, 201, "item201")
      .addSelection(true, 2, 202, "item202")
      .withSv2()
      .build();

    // Second EMIT boundary batches
    final RowSet.SingleRowSet nonEmptyInputRowSet3 = operatorFixture.rowSetBuilder(inputSchema)
      .addSelection(false, 1, 1001, "item1001")
      .addSelection(true, 1, 1002, "item1002")
      .addSelection(true, 1, 1003, "item1003")
      .addSelection(true, 2, 2000, "item2000")
      .addSelection(false, 2, 2001, "item2001")
      .addSelection(true, 2, 2002, "item2002")
      .withSv2()
      .build();

    final RowSet.SingleRowSet nonEmptyInputRowSet4 = operatorFixture.rowSetBuilder(inputSchema)
      .addSelection(true, 3, 3001, "item3001")
      .addSelection(false, 3, 3002, "item3002")
      .addSelection(true, 3, 3003, "item3003")
      .addSelection(true, 4, 4000, "item4000")
      .addSelection(true, 4, 4001, "item4001")
      .withSv2()
      .build();

    // Third EMIT boundary batches
    final RowSet.SingleRowSet nonEmptyInputRowSet5 = operatorFixture.rowSetBuilder(inputSchema)
      .addSelection(true, 1, 10001, "item10001")
      .addSelection(true, 1, 10002, "item10002")
      .addSelection(false, 1, 10003, "item10003")
      .withSv2()
      .build();

    // First EMIT boundary expected row sets
    final RowSet expectedRowSet2 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 101, "item101")
      .addRow(1, 103, "item103")
      .addRow(2, 201, "item201")
      .addRow(2, 202, "item202")
      .build();

    // Second EMIT boundary expected row sets
    final RowSet expectedRowSet3 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 1002, "item1002")
      .addRow(1, 1003, "item1003")
      .addRow(2, 2000, "item2000")
      .addRow(2, 2002, "item2002")
      .build();

    final RowSet expectedRowSet4 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(3, 3001, "item3001")
      .addRow(3, 3003, "item3003")
      .addRow(4, 4000, "item4000")
      .addRow(4, 4001, "item4001")
      .build();

    // Third EMIT boundary expected row sets
    final RowSet expectedRowSet5 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 10001, "item10001")
      .addRow(1, 10002, "item10002")
      .build();

    inputContainer.add(emptyWithSv2.container());
    inputContainer.add(nonEmptyInputRowSet2.container());
    inputContainer.add(nonEmptyInputRowSet3.container());
    inputContainer.add(nonEmptyInputRowSet4.container());
    inputContainer.add(nonEmptyInputRowSet5.container());
    inputContainer.add(emptyWithSv2.container());

    inputContainerSv2.add(emptyWithSv2.getSv2());
    inputContainerSv2.add(nonEmptyInputRowSet2.getSv2());
    inputContainerSv2.add(nonEmptyInputRowSet3.getSv2());
    inputContainerSv2.add(nonEmptyInputRowSet4.getSv2());
    inputContainerSv2.add(nonEmptyInputRowSet5.getSv2());
    inputContainerSv2.add(emptyWithSv2.getSv2());

    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);
    inputOutcomes.add(RecordBatch.IterOutcome.OK);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);

    expectedOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    expectedOutcomes.add(RecordBatch.IterOutcome.EMIT);
    expectedOutcomes.add(RecordBatch.IterOutcome.OK);
    expectedOutcomes.add(RecordBatch.IterOutcome.EMIT);
    expectedOutcomes.add(RecordBatch.IterOutcome.EMIT);
    expectedOutcomes.add(RecordBatch.IterOutcome.EMIT);

    expectedRecordCounts.add(0);
    expectedRecordCounts.add(expectedRowSet2.rowCount());
    expectedRecordCounts.add(expectedRowSet3.rowCount());
    expectedRecordCounts.add(expectedRowSet4.rowCount());
    expectedRecordCounts.add(expectedRowSet5.rowCount());
    expectedRecordCounts.add(0);

    expectedRowSets.add(expectedRowSet2);
    expectedRowSets.add(expectedRowSet3);
    expectedRowSets.add(expectedRowSet4);
    expectedRowSets.add(expectedRowSet5);

    testPartitionLimitCommon(-5, 2);
  }
}
