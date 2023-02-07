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
package org.apache.drill.exec.physical.impl.svremover;

import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.physical.config.SelectionVectorRemover;
import org.apache.drill.exec.physical.impl.BaseTestOpBatchEmitOutcome;
import org.apache.drill.exec.physical.impl.MockRecordBatch;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.physical.rowSet.DirectRowSet;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.test.rowSet.RowSetComparison;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestSVRemoverIterOutcome extends BaseTestOpBatchEmitOutcome {
  //private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestSVRemoverIterOutcome.class);

  // Holds reference to actual operator instance created for each tests
  private static RemovingRecordBatch removingRecordBatch;

  // Lits of expected outcomes populated by each tests. Used to verify actual IterOutcome returned with next call on
  // operator to expected outcome
  private final List<RecordBatch.IterOutcome> expectedOutcomes = new ArrayList<>();

  // List of expected row counts populated by each tests. Used to verify actual output row count to expected row count
  private final List<Integer> expectedRecordCounts = new ArrayList<>();

  // List of expected row sets populated by each tests. Used to verify actual output from operator to expected output
  private final List<RowSet> expectedRowSets = new ArrayList<>();

  /**
   * Cleanup method executed post each test
   */
  @After
  public void afterTestCleanup() {
    // close removing recordbatch
    removingRecordBatch.close();

    // Release memory from expectedRowSets
    for (RowSet expectedRowSet : expectedRowSets) {
      expectedRowSet.clear();
    }
    expectedOutcomes.clear();
    expectedRecordCounts.clear();
    expectedRowSets.clear();
  }

  private void testSVRemoverCommon() {
    final SelectionVectorRemover svRemover = new SelectionVectorRemover(null);
    final MockRecordBatch batch = new MockRecordBatch(operatorFixture.getFragmentContext(), opContext, inputContainer,
        inputOutcomes, inputContainerSv2, inputContainer.get(0).getSchema());

    removingRecordBatch = new RemovingRecordBatch(svRemover, fragContext, batch);

    int i=0;
    int expectedRowSetIndex = 0;
    while (i < expectedOutcomes.size()) {
      try {
        assertEquals(expectedOutcomes.get(i), removingRecordBatch.next());
        assertEquals(removingRecordBatch.getRecordCount(), (int)expectedRecordCounts.get(i++));

        if (removingRecordBatch.getRecordCount() > 0) {
          final RowSet actualRowSet = DirectRowSet.fromContainer(removingRecordBatch.getContainer());
          new RowSetComparison(expectedRowSets.get(expectedRowSetIndex++)).verify(actualRowSet);
        }
      } finally {
        removingRecordBatch.getContainer().zeroVectors();
      }
    }
  }

  @Test
  public void test_SimpleContainer_NoSchemaChange() {
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(nonEmptyInputRowSet.container());

    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);

    // Expected row sets
    final RowSet expectedRowSet = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 10, "item1")
      .build();

    expectedRowSets.add(expectedRowSet);

    expectedOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    expectedOutcomes.add(RecordBatch.IterOutcome.OK);

    expectedRecordCounts.add(0);
    expectedRecordCounts.add(expectedRowSet.rowCount());

    testSVRemoverCommon();
  }

  @Test
  public void test_SimpleContainer_SchemaChange() {
    inputContainer.add(emptyInputRowSet.container());

    TupleMetadata inputSchema2 = new SchemaBuilder()
      .add("id_left", TypeProtos.MinorType.INT)
      .add("cost_left", TypeProtos.MinorType.VARCHAR)
      .add("name_left", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    RowSet nonEmpty2 = operatorFixture.rowSetBuilder(inputSchema2)
      .addRow(1, "10", "item1")
      .addRow(2, "20", "item2")
      .build();

    inputContainer.add(nonEmpty2.container());

    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);

    // Expected row sets
    final RowSet expectedRowSet = operatorFixture.rowSetBuilder(inputSchema2)
      .addRow(1, "10", "item1")
      .addRow(2, "20", "item2")
      .build();

    expectedRowSets.add(expectedRowSet);

    expectedOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    expectedOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);

    expectedRecordCounts.add(0);
    expectedRecordCounts.add(expectedRowSet.rowCount());

    testSVRemoverCommon();
  }

  @Test
  public void test_SV2Container_NoSchemaChange() {
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

    inputContainer.add(emptyWithSv2.container());
    inputContainer.add(nonEmptyInputRowSet2.container());

    inputContainerSv2.add(emptyWithSv2.getSv2());
    inputContainerSv2.add(nonEmptyInputRowSet2.getSv2());

    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);

    // Expected row sets
    final RowSet expectedRowSet = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 101, "item101")
      .addRow(1, 103, "item103")
      .addRow(2, 201, "item201")
      .addRow(2, 202, "item202")
      .build();

    expectedRowSets.add(expectedRowSet);

    expectedOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    expectedOutcomes.add(RecordBatch.IterOutcome.OK);

    expectedRecordCounts.add(0);
    expectedRecordCounts.add(expectedRowSet.rowCount());

    testSVRemoverCommon();
  }

  @Test
  public void test_SV2Container_SchemaChange() {
    // Batch for schema 1
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

    // Batch for schema 2
    TupleMetadata inputSchema2 = new SchemaBuilder()
      .add("id_left", TypeProtos.MinorType.INT)
      .add("cost_left", TypeProtos.MinorType.VARCHAR)
      .add("name_left", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    final RowSet.SingleRowSet empty2WithSv2 = operatorFixture.rowSetBuilder(inputSchema2)
      .withSv2()
      .build();

    final RowSet.SingleRowSet nonEmpty2InputRowSet2 = operatorFixture.rowSetBuilder(inputSchema2)
      .addSelection(true, 1, "101", "item101")
      .addSelection(false, 1, "102", "item102")
      .addSelection(true, 1, "103", "item103")
      .addSelection(false, 2, "200", "item200")
      .addSelection(true, 2, "201", "item201")
      .withSv2()
      .build();

    inputContainer.add(emptyWithSv2.container());
    inputContainer.add(nonEmptyInputRowSet2.container());
    inputContainer.add(empty2WithSv2.container());
    inputContainer.add(nonEmpty2InputRowSet2.container());

    inputContainerSv2.add(emptyWithSv2.getSv2());
    inputContainerSv2.add(nonEmptyInputRowSet2.getSv2());
    inputContainerSv2.add(empty2WithSv2.getSv2());
    inputContainerSv2.add(nonEmpty2InputRowSet2.getSv2());

    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);

    // Expected row sets
    final RowSet expectedRowSet = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 101, "item101")
      .addRow(1, 103, "item103")
      .addRow(2, 201, "item201")
      .addRow(2, 202, "item202")
      .build();

    final RowSet expectedRowSet1 = operatorFixture.rowSetBuilder(inputSchema2)
      .addRow(1, "101", "item101")
      .addRow(1, "103", "item103")
      .addRow(2, "201", "item201")
      .build();

    expectedRowSets.add(expectedRowSet);
    expectedRowSets.add(expectedRowSet1);

    expectedOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    expectedOutcomes.add(RecordBatch.IterOutcome.OK);
    expectedOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    expectedOutcomes.add(RecordBatch.IterOutcome.OK);

    expectedRecordCounts.add(0);
    expectedRecordCounts.add(expectedRowSet.rowCount());
    expectedRecordCounts.add(0);
    expectedRecordCounts.add(expectedRowSet1.rowCount());

    testSVRemoverCommon();
  }
}
