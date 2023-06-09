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

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.categories.OperatorTest;
import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.common.logical.data.Order;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.config.ExternalSort;
import org.apache.drill.exec.physical.impl.BaseTestOpBatchEmitOutcome;
import org.apache.drill.exec.physical.impl.MockRecordBatch;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.store.mock.MockStorePOP;
import org.apache.drill.test.OperatorFixture;
import org.apache.drill.exec.physical.rowSet.HyperRowSetImpl;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.test.rowSet.RowSetComparison;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static junit.framework.TestCase.assertTrue;
import static org.apache.drill.exec.record.RecordBatch.IterOutcome.EMIT;
import static org.apache.drill.exec.record.RecordBatch.IterOutcome.NONE;
import static org.apache.drill.exec.record.RecordBatch.IterOutcome.OK;
import static org.apache.drill.exec.record.RecordBatch.IterOutcome.OK_NEW_SCHEMA;
import static org.junit.Assert.assertEquals;

@Category(OperatorTest.class)
public class TestSortEmitOutcome extends BaseTestOpBatchEmitOutcome {

  private ExternalSortBatch sortBatch;

  private static ExternalSort sortPopConfig;

  @BeforeClass
  public static void defineOrdering() {
    String columnToSort = inputSchema.column(0).getName();
    FieldReference expr = FieldReference.getWithQuotedRef(columnToSort);
    Order.Ordering ordering = new Order.Ordering(Order.Ordering.ORDER_ASC, expr, Order.Ordering.NULLS_FIRST);
    sortPopConfig = new ExternalSort(null, Lists.newArrayList(ordering), false);
  }

  @After
  public void closeOperator() {
    if (sortBatch != null) {
      sortBatch.close();
    }
  }

  /**
   * Verifies that if SortBatch receives empty batches with OK_NEW_SCHEMA and EMIT outcome then it correctly produces
   * empty batches as output. First empty batch will be with OK_NEW_SCHEMA and second will be with EMIT outcome.
   */
  @Test
  public void testSortEmptyBatchEmitOutcome() {
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(emptyInputRowSet.container());

    inputOutcomes.add(OK_NEW_SCHEMA);
    inputOutcomes.add(EMIT);

    final MockRecordBatch mockInputBatch = new MockRecordBatch(operatorFixture.getFragmentContext(), opContext,
      inputContainer, inputOutcomes, emptyInputRowSet.container().getSchema());
    sortBatch = new ExternalSortBatch(sortPopConfig, operatorFixture.getFragmentContext(), mockInputBatch);

    // BuildSchema phase output
    assertTrue(sortBatch.next() == OK_NEW_SCHEMA);
    outputRecordCount += sortBatch.getRecordCount();

    // Output for first empty EMIT batch
    assertTrue(sortBatch.next() == OK_NEW_SCHEMA);
    assertTrue(sortBatch.next() == EMIT);
    outputRecordCount += sortBatch.getRecordCount();
    assertEquals(0, outputRecordCount);

    assertTrue(sortBatch.next() == NONE);
  }

  /**
   * Verifies ExternalSortBatch handling of first non-empty batch with EMIT outcome post buildSchema phase. Expectation
   * is that it will return 2 output batch for first EMIT incoming, first output batch with OK_NEW_SCHEMA followed by
   * second output batch with EMIT outcome.
   */
  @Test
  public void testSortNonEmptyBatchEmitOutcome() {
    final RowSet.SingleRowSet nonEmptyInputRowSet2 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(2, 20, "item2")
      .addRow(13, 130, "item13")
      .addRow(4, 40, "item4")
      .build();

    final RowSet.SingleRowSet expectedRowSet = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(2, 20, "item2")
      .addRow(4, 40, "item4")
      .addRow(13, 130, "item13")
      .build();

    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(nonEmptyInputRowSet2.container());

    inputOutcomes.add(OK_NEW_SCHEMA);
    inputOutcomes.add(EMIT);

    final MockRecordBatch mockInputBatch = new MockRecordBatch(operatorFixture.getFragmentContext(), opContext,
      inputContainer, inputOutcomes, emptyInputRowSet.container().getSchema());
    sortBatch = new ExternalSortBatch(sortPopConfig, operatorFixture.getFragmentContext(), mockInputBatch);

    // BuildSchema phase output
    assertTrue(sortBatch.next() == OK_NEW_SCHEMA);
    outputRecordCount += sortBatch.getRecordCount();
    assertEquals(0, outputRecordCount);

    // Output batch 1 for first non-empty EMIT batch
    assertTrue(sortBatch.next() == OK_NEW_SCHEMA);
    outputRecordCount += sortBatch.getRecordCount();
    assertEquals(3, outputRecordCount);

    // verify results
    RowSet actualRowSet = HyperRowSetImpl.fromContainer(sortBatch.getContainer(), sortBatch.getSelectionVector4());
    new RowSetComparison(expectedRowSet).verify(actualRowSet);

    // Output batch 2 for first non-empty EMIT batch
    assertTrue(sortBatch.next() == EMIT);
    outputRecordCount += sortBatch.getRecordCount();
    assertEquals(3, outputRecordCount);

    // Release memory for row sets
    nonEmptyInputRowSet2.clear();
    expectedRowSet.clear();
  }

  /**
   * Verifies ExternalSortBatch behavior when it receives first incoming batch post buildSchema phase as empty batch
   * with EMIT outcome followed by non-empty batch with EMIT outcome. Expectation is sort will handle the EMIT
   * boundary correctly and produce 2 empty output batch for first EMIT outcome and 1 non-empty output batch for second
   * EMIT outcome.
   */
  @Test
  public void testSortEmptyBatchFollowedByNonEmptyBatchEmitOutcome() {
    final RowSet.SingleRowSet nonEmptyInputRowSet2 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(2, 20, "item2")
      .addRow(13, 130, "item13")
      .addRow(4, 40, "item4")
      .build();

    final RowSet.SingleRowSet expectedRowSet = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(2, 20, "item2")
      .addRow(4, 40, "item4")
      .addRow(13, 130, "item13")
      .build();

    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(nonEmptyInputRowSet2.container());

    inputOutcomes.add(OK_NEW_SCHEMA);
    inputOutcomes.add(EMIT);
    inputOutcomes.add(EMIT);

    final MockRecordBatch mockInputBatch = new MockRecordBatch(operatorFixture.getFragmentContext(), opContext,
      inputContainer, inputOutcomes, emptyInputRowSet.container().getSchema());
    sortBatch = new ExternalSortBatch(sortPopConfig, operatorFixture.getFragmentContext(), mockInputBatch);

    // BuildSchema phase
    assertTrue(sortBatch.next() == OK_NEW_SCHEMA);
    outputRecordCount += sortBatch.getRecordCount();
    assertEquals(0, outputRecordCount);

    // Output for first empty EMIT batch
    assertTrue(sortBatch.next() == OK_NEW_SCHEMA);
    assertTrue(sortBatch.next() == EMIT);
    outputRecordCount += sortBatch.getRecordCount();
    assertEquals(0, outputRecordCount);

    // Output for second non-empty EMIT batch
    assertTrue(sortBatch.next() == EMIT);
    outputRecordCount += sortBatch.getRecordCount();
    assertEquals(3, outputRecordCount);

    // verify results
    RowSet actualRowSet = HyperRowSetImpl.fromContainer(sortBatch.getContainer(), sortBatch.getSelectionVector4());
    new RowSetComparison(expectedRowSet).verify(actualRowSet);

    // Release memory for row sets
    nonEmptyInputRowSet2.clear();
    expectedRowSet.clear();
  }

  /**
   * Verifies ExternalSortBatch behavior with runs of empty batch with EMIT outcome followed by an non-empty batch
   * with EMIT outcome.
   */
  @Test
  public void testSortMultipleEmptyBatchWithANonEmptyBatchEmitOutcome() {
    final RowSet.SingleRowSet nonEmptyInputRowSet2 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(2, 20, "item2")
      .addRow(13, 130, "item13")
      .addRow(4, 40, "item4")
      .build();

    final RowSet.SingleRowSet expectedRowSet = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(2, 20, "item2")
      .addRow(4, 40, "item4")
      .addRow(13, 130, "item13")
      .build();

    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(nonEmptyInputRowSet2.container());

    inputOutcomes.add(OK_NEW_SCHEMA);
    inputOutcomes.add(EMIT);
    inputOutcomes.add(EMIT);
    inputOutcomes.add(EMIT);
    inputOutcomes.add(EMIT);

    final MockRecordBatch mockInputBatch = new MockRecordBatch(operatorFixture.getFragmentContext(), opContext,
      inputContainer, inputOutcomes, emptyInputRowSet.container().getSchema());
    sortBatch = new ExternalSortBatch(sortPopConfig, operatorFixture.getFragmentContext(), mockInputBatch);

    // BuildSchema phase output
    assertTrue(sortBatch.next() == OK_NEW_SCHEMA);
    outputRecordCount += sortBatch.getRecordCount();
    assertEquals(0, outputRecordCount);

    // Output for first empty EMIT batch
    assertTrue(sortBatch.next() == OK_NEW_SCHEMA);
    assertTrue(sortBatch.next() == EMIT);

    // Output for 2nd empty EMIT batch
    assertTrue(sortBatch.next() == EMIT);
    // Output for 3rd empty EMIT batch
    assertTrue(sortBatch.next() == EMIT);

    outputRecordCount += sortBatch.getRecordCount();
    assertEquals(0, outputRecordCount);

    // Output for 4th non-empty EMIT batch
    assertTrue(sortBatch.next() == EMIT);
    outputRecordCount += sortBatch.getRecordCount();
    assertEquals(3, outputRecordCount);

    // verify results
    RowSet actualRowSet = HyperRowSetImpl.fromContainer(sortBatch.getContainer(), sortBatch.getSelectionVector4());
    new RowSetComparison(expectedRowSet).verify(actualRowSet);

    // Release memory for row sets
    nonEmptyInputRowSet2.clear();
    expectedRowSet.clear();
  }

  /**
   * Verifies ExternalSortBatch behavior when it receives non-empty batch in BuildSchema phase followed by empty EMIT
   * batch. Second record boundary has non-empty batch with OK outcome followed by empty EMIT outcome batch. In this
   * case for first non-empty batch in buildSchema phase, sort should consider that data as part of first record
   * boundary and produce it in output for that record boundary with EMIT outcome. Same is true for second pair of
   * batches with OK and EMIT outcome
   */
  @Test
  public void testTopNResetsAfterFirstEmitOutcome() {
    final RowSet.SingleRowSet nonEmptyInputRowSet2 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(2, 20, "item2")
      .addRow(3, 30, "item3")
      .build();

    final RowSet.SingleRowSet expectedRowSet1 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 10, "item1")
      .build();

    final RowSet.SingleRowSet expectedRowSet2 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(2, 20, "item2")
      .addRow(3, 30, "item3")
      .build();

    inputContainer.add(nonEmptyInputRowSet.container());
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(nonEmptyInputRowSet2.container());
    inputContainer.add(emptyInputRowSet.container());

    inputOutcomes.add(OK_NEW_SCHEMA);
    inputOutcomes.add(EMIT);
    inputOutcomes.add(OK);
    inputOutcomes.add(EMIT);

    final MockRecordBatch mockInputBatch = new MockRecordBatch(operatorFixture.getFragmentContext(), opContext,
      inputContainer, inputOutcomes, emptyInputRowSet.container().getSchema());
    sortBatch = new ExternalSortBatch(sortPopConfig, operatorFixture.getFragmentContext(), mockInputBatch);

    // BuildSchema phase output
    assertTrue(sortBatch.next() == OK_NEW_SCHEMA);

    // Output batch 1 for non-empty batch in BuildSchema phase and empty EMIT batch following it
    assertTrue(sortBatch.next() == OK_NEW_SCHEMA);
    assertEquals(1, sortBatch.getRecordCount());

    // verify results
    RowSet actualRowSet = HyperRowSetImpl.fromContainer(sortBatch.getContainer(), sortBatch.getSelectionVector4());
    new RowSetComparison(expectedRowSet1).verify(actualRowSet);

    assertTrue(sortBatch.next() == EMIT);
    assertEquals(0, sortBatch.getRecordCount());

    // Output batch 2 for non-empty input batch with OK followed by empty EMIT batch
    assertTrue(sortBatch.next() == EMIT);
    assertEquals(2, sortBatch.getRecordCount());

    // verify results
    RowSet actualRowSet2 = HyperRowSetImpl.fromContainer(sortBatch.getContainer(), sortBatch.getSelectionVector4());
    new RowSetComparison(expectedRowSet2).verify(actualRowSet2);

    // Release memory for row sets
    nonEmptyInputRowSet2.clear();
    expectedRowSet2.clear();
    expectedRowSet1.clear();
  }

  /**
   * Verifies ExternalSortBatch behavior when it receives incoming batches with different IterOutcomes like
   * OK_NEW_SCHEMA / OK / EMIT / NONE
   */
  @Test
  public void testSort_NonEmptyFirst_EmptyOKEmitOutcome() {
    final RowSet.SingleRowSet expectedRowSet = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 10, "item1")
      .build();

    inputContainer.add(nonEmptyInputRowSet.container());
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(emptyInputRowSet.container());

    inputOutcomes.add(OK_NEW_SCHEMA);
    inputOutcomes.add(OK);
    inputOutcomes.add(EMIT);
    inputOutcomes.add(NONE);

    final MockRecordBatch mockInputBatch = new MockRecordBatch(operatorFixture.getFragmentContext(), opContext,
      inputContainer, inputOutcomes, emptyInputRowSet.container().getSchema());
    sortBatch = new ExternalSortBatch(sortPopConfig, operatorFixture.getFragmentContext(), mockInputBatch);

    // BuildSchema phase output
    assertTrue(sortBatch.next() == OK_NEW_SCHEMA);
    assertEquals(0, sortBatch.getRecordCount());

    // Output batch 1 for first 3 input batches with OK_NEW_SCHEMA/OK/EMIT outcome
    assertTrue(sortBatch.next() == OK_NEW_SCHEMA);
    assertEquals(1, sortBatch.getRecordCount());

    // verify results
    RowSet actualRowSet = HyperRowSetImpl.fromContainer(sortBatch.getContainer(), sortBatch.getSelectionVector4());
    new RowSetComparison(expectedRowSet).verify(actualRowSet);

    // Output batch 2 for first 3 input batches with OK_NEW_SCHEMA/OK/EMIT outcome
    assertTrue(sortBatch.next() == EMIT);
    assertEquals(0, sortBatch.getRecordCount());

    // Output batch for NONE outcome
    assertTrue(sortBatch.next() == NONE);

    // Release memory for row set
    expectedRowSet.clear();
  }

  @Test
  public void testTopNMultipleOutputBatch() {
    final RowSet.SingleRowSet nonEmptyInputRowSet2 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(4, 40, "item4")
      .addRow(2, 20, "item2")
      .addRow(5, 50, "item5")
      .addRow(3, 30, "item3")
      .build();

    final RowSet.SingleRowSet expectedRowSet1 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 10, "item1")
      .build();

    final RowSet.SingleRowSet expectedRowSet2 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(2, 20, "item2")
      .addRow(3, 30, "item3")
      .addRow(4, 40, "item4")
      .addRow(5, 50, "item5")
      .build();

    inputContainer.add(nonEmptyInputRowSet.container());
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(nonEmptyInputRowSet2.container());

    inputOutcomes.add(OK_NEW_SCHEMA);
    inputOutcomes.add(EMIT);
    inputOutcomes.add(OK);

    final MockRecordBatch mockInputBatch = new MockRecordBatch(operatorFixture.getFragmentContext(), opContext,
      inputContainer, inputOutcomes, emptyInputRowSet.container().getSchema());
    sortBatch = new ExternalSortBatch(sortPopConfig, operatorFixture.getFragmentContext(), mockInputBatch);

    // BuildSchema phase output
    assertTrue(sortBatch.next() == OK_NEW_SCHEMA);

    // Output batch 1 for first EMIT outcome
    assertTrue(sortBatch.next() == OK_NEW_SCHEMA);
    assertEquals(1, sortBatch.getRecordCount());

    // verify results
    RowSet actualRowSet1 = HyperRowSetImpl.fromContainer(sortBatch.getContainer(), sortBatch.getSelectionVector4());
    new RowSetComparison(expectedRowSet1).verify(actualRowSet1);

    // Output batch 2 for first EMIT outcome
    assertTrue(sortBatch.next() == EMIT);
    assertEquals(0, sortBatch.getRecordCount());

    // Output batch for OK outcome
    assertTrue(sortBatch.next() == OK);
    assertEquals(4, sortBatch.getRecordCount());

    // verify results
    RowSet actualRowSet2 = HyperRowSetImpl.fromContainer(sortBatch.getContainer(), sortBatch.getSelectionVector4());
    new RowSetComparison(expectedRowSet2).verify(actualRowSet2);

    // Output batch for NONE outcome
    assertTrue(sortBatch.next() == NONE);

    // Release memory for row sets
    nonEmptyInputRowSet2.clear();
    expectedRowSet2.clear();
    expectedRowSet1.clear();
  }

  @Test
  public void testSortMultipleEMITOutcome() {
    final RowSet.SingleRowSet nonEmptyInputRowSet2 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(2, 20, "item2")
      .addRow(3, 30, "item3")
      .build();

    inputContainer.add(nonEmptyInputRowSet.container());
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(nonEmptyInputRowSet2.container());
    inputContainer.add(emptyInputRowSet.container());

    inputOutcomes.add(OK_NEW_SCHEMA);
    inputOutcomes.add(EMIT);
    inputOutcomes.add(EMIT);
    inputOutcomes.add(EMIT);

    final MockRecordBatch mockInputBatch = new MockRecordBatch(operatorFixture.getFragmentContext(), opContext,
      inputContainer, inputOutcomes, emptyInputRowSet.container().getSchema());
    sortBatch = new ExternalSortBatch(sortPopConfig, operatorFixture.getFragmentContext(), mockInputBatch);

    // BuildSchema phase output
    assertTrue(sortBatch.next() == OK_NEW_SCHEMA);

    // Output batch 1 for first EMIT outcome
    assertTrue(sortBatch.next() == OK_NEW_SCHEMA);
    assertEquals(1, sortBatch.getRecordCount());

    // Output batch 2 for first EMIT outcome
    assertTrue(sortBatch.next() == EMIT);
    assertEquals(0, sortBatch.getRecordCount());

    // Output batch for second EMIT outcome
    assertTrue(sortBatch.next() == EMIT);
    assertEquals(2, sortBatch.getRecordCount());

    // Output batch for third EMIT outcome
    assertTrue(sortBatch.next() == EMIT);
    assertEquals(0, sortBatch.getRecordCount());

    nonEmptyInputRowSet2.clear();
  }

  /**
   * Verifies ExternalSortBatch behavior when it receives multiple non-empty batch across same EMIT boundary such
   * that all the output records can fit within single output batch. Then Sort correctly waits for the EMIT outcome
   * before producing the output batches for all the buffered incoming batches with data.
   */
  @Test
  public void testSortMultipleInputToSingleOutputBatch() {

    final RowSet.SingleRowSet nonEmptyInputRowSet2 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(2, 20, "item2")
      .build();

    final RowSet.SingleRowSet expectedRowSet = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 10, "item1")
      .addRow(2, 20, "item2")
      .build();

    inputContainer.add(nonEmptyInputRowSet.container());
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(nonEmptyInputRowSet2.container());
    inputContainer.add(emptyInputRowSet.container());

    inputOutcomes.add(OK_NEW_SCHEMA);
    inputOutcomes.add(OK);
    inputOutcomes.add(OK);
    inputOutcomes.add(EMIT);

    final MockRecordBatch mockInputBatch = new MockRecordBatch(operatorFixture.getFragmentContext(), opContext,
      inputContainer, inputOutcomes, emptyInputRowSet.container().getSchema());
    sortBatch = new ExternalSortBatch(sortPopConfig, operatorFixture.getFragmentContext(), mockInputBatch);

    // BuildSchema phase output
    assertTrue(sortBatch.next() == OK_NEW_SCHEMA);

    // Output batch 1 for the EMIT boundary
    assertTrue(sortBatch.next() == OK_NEW_SCHEMA);
    assertEquals(2, sortBatch.getRecordCount());

    // Verify Results
    RowSet actualRowSet = HyperRowSetImpl.fromContainer(sortBatch.getContainer(), sortBatch.getSelectionVector4());
    new RowSetComparison(expectedRowSet).verify(actualRowSet);

    // Output batch 2 for the EMIT boundary
    assertTrue(sortBatch.next() == EMIT);
    assertEquals(0, sortBatch.getRecordCount());

    nonEmptyInputRowSet2.clear();
  }

  /**
   * Verifies ExternalSortBatch behavior when it sees batches with EMIT outcome but has to spill to disk because of
   * memory pressure. Expectation is currenlty spilling is not supported with EMIT outcome so while preparing the
   * output batch that will be detected and Sort will throw UnsupportedOperationException
   * @throws Exception
   */
  @Test(expected = UnsupportedOperationException.class)
  public void testSpillNotSupportedWithEmitOutcome() throws Exception {
    final OperatorFixture.Builder builder = OperatorFixture.builder(dirTestWatcher);
    // Configuration that forces Sort to spill after buffering 2 incoming batches with data
    builder.configBuilder().put(ExecConstants.EXTERNAL_SORT_BATCH_LIMIT, 2);

    final OperatorFixture fixture_local = builder.build();

    final RowSet.SingleRowSet local_EmptyInputRowSet = fixture_local.rowSetBuilder(inputSchema).build();
    final RowSet.SingleRowSet local_nonEmptyInputRowSet1 = fixture_local.rowSetBuilder(inputSchema)
      .addRow(3, 30, "item3")
      .addRow(2, 20, "item2")
      .build();
    final RowSet.SingleRowSet local_nonEmptyInputRowSet2 = fixture_local.rowSetBuilder(inputSchema)
      .addRow(1, 10, "item1")
      .build();
    final RowSet.SingleRowSet local_nonEmptyInputRowSet3 = fixture_local.rowSetBuilder(inputSchema)
      .addRow(4, 40, "item4")
      .build();

    inputContainer.add(local_EmptyInputRowSet.container());
    inputContainer.add(local_nonEmptyInputRowSet1.container());
    inputContainer.add(local_nonEmptyInputRowSet2.container());
    inputContainer.add(local_nonEmptyInputRowSet3.container());
    inputContainer.add(local_EmptyInputRowSet.container());

    inputOutcomes.add(OK_NEW_SCHEMA);
    inputOutcomes.add(OK);
    inputOutcomes.add(OK);
    inputOutcomes.add(OK);
    inputOutcomes.add(EMIT);

    final PhysicalOperator mockPopConfig_local = new MockStorePOP(null);
    final OperatorContext opContext_local = fixture_local.getFragmentContext().newOperatorContext(mockPopConfig_local);

    final MockRecordBatch mockInputBatch = new MockRecordBatch(fixture_local.getFragmentContext(), opContext_local,
      inputContainer, inputOutcomes, local_EmptyInputRowSet.container().getSchema());
    final ExternalSortBatch sortBatch_local = new ExternalSortBatch(sortPopConfig, fixture_local.getFragmentContext(),
      mockInputBatch);

    assertTrue(sortBatch_local.next() == OK_NEW_SCHEMA);
    // Should throw the exception
    sortBatch_local.next();

    // Release memory for row sets
    local_EmptyInputRowSet.clear();
    local_nonEmptyInputRowSet1.clear();
    local_nonEmptyInputRowSet2.clear();
    local_nonEmptyInputRowSet3.clear();
    sortBatch_local.close();
    fixture_local.close();
  }

  /***************************************************************************************************************
   * Test for validating ExternalSortBatch behavior without EMIT outcome
   ***************************************************************************************************************/
  @Test
  public void testTopN_WithEmptyNonEmptyBatchesAndOKOutcome() {
    final RowSet.SingleRowSet nonEmptyInputRowSet2 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(7, 70, "item7")
      .addRow(3, 30, "item3")
      .addRow(13, 130, "item13")
      .build();

    final RowSet.SingleRowSet nonEmptyInputRowSet3 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(17, 170, "item17")
      .addRow(23, 230, "item23")
      .addRow(130, 1300, "item130")
      .build();

    final RowSet.SingleRowSet expectedRowSet = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(1, 10, "item1")
      .addRow(3, 30, "item3")
      .addRow(7, 70, "item7")
      .addRow(13, 130, "item13")
      .addRow(17, 170, "item17")
      .addRow(23, 230, "item23")
      .addRow(130, 1300, "item130")
      .build();

    inputContainer.add(nonEmptyInputRowSet.container());
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(nonEmptyInputRowSet2.container());
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(nonEmptyInputRowSet3.container());
    inputContainer.add(emptyInputRowSet.container());

    inputOutcomes.add(OK_NEW_SCHEMA);
    inputOutcomes.add(OK);
    inputOutcomes.add(OK);
    inputOutcomes.add(OK);
    inputOutcomes.add(OK);
    inputOutcomes.add(OK);

    final MockRecordBatch mockInputBatch = new MockRecordBatch(operatorFixture.getFragmentContext(), opContext,
      inputContainer, inputOutcomes, emptyInputRowSet.container().getSchema());
    sortBatch = new ExternalSortBatch(sortPopConfig, operatorFixture.getFragmentContext(), mockInputBatch);

    assertTrue(sortBatch.next() == OK_NEW_SCHEMA);
    assertTrue(sortBatch.next() == OK_NEW_SCHEMA);
    assertEquals(7, sortBatch.getRecordCount());
    assertTrue(sortBatch.getSchema().getSelectionVectorMode() == BatchSchema.SelectionVectorMode.FOUR_BYTE);

    RowSet actualRowSet = HyperRowSetImpl.fromContainer(sortBatch.getContainer(), sortBatch.getSelectionVector4());
    new RowSetComparison(expectedRowSet).verify(actualRowSet);

    assertTrue(sortBatch.next() == NONE);

    nonEmptyInputRowSet2.clear();
    nonEmptyInputRowSet3.clear();
    expectedRowSet.clear();
  }

  @Test
  public void testRegularTopNWithEmptyDataSet() {
    inputContainer.add(emptyInputRowSet.container());

    inputOutcomes.add(OK_NEW_SCHEMA);

    final MockRecordBatch mockInputBatch = new MockRecordBatch(operatorFixture.getFragmentContext(), opContext,
      inputContainer, inputOutcomes, emptyInputRowSet.container().getSchema());
    sortBatch = new ExternalSortBatch(sortPopConfig, operatorFixture.getFragmentContext(), mockInputBatch);

    assertTrue(sortBatch.next() == OK_NEW_SCHEMA);
    assertTrue(sortBatch.next() == NONE);
  }

  /**
   * Verifies successful spilling in absence of EMIT outcome
   * @throws Exception
   */
  @Test
  public void testSpillWithNoEmitOutcome() throws Exception {
    final OperatorFixture.Builder builder = OperatorFixture.builder(dirTestWatcher);
    // Configuration that forces Sort to spill after buffering 2 incoming batches with data
    builder.configBuilder().put(ExecConstants.EXTERNAL_SORT_BATCH_LIMIT, 2);

    final OperatorFixture fixture_local = builder.build();

    final RowSet.SingleRowSet local_nonEmptyInputRowSet1 = fixture_local.rowSetBuilder(inputSchema)
      .addRow(3, 30, "item3")
      .addRow(2, 20, "item2")
      .build();
    final RowSet.SingleRowSet local_nonEmptyInputRowSet2 = fixture_local.rowSetBuilder(inputSchema)
      .addRow(1, 10, "item1")
      .build();
    final RowSet.SingleRowSet local_nonEmptyInputRowSet3 = fixture_local.rowSetBuilder(inputSchema)
      .addRow(4, 40, "item4")
      .build();

    inputContainer.add(local_nonEmptyInputRowSet1.container());
    inputContainer.add(local_nonEmptyInputRowSet2.container());
    inputContainer.add(local_nonEmptyInputRowSet3.container());

    inputOutcomes.add(OK_NEW_SCHEMA);
    inputOutcomes.add(OK);
    inputOutcomes.add(OK);

    final PhysicalOperator mockPopConfig_local = new MockStorePOP(null);
    final OperatorContext opContext_local = fixture_local.getFragmentContext().newOperatorContext(mockPopConfig_local);

    final MockRecordBatch mockInputBatch = new MockRecordBatch(fixture_local.getFragmentContext(), opContext_local,
      inputContainer, inputOutcomes, local_nonEmptyInputRowSet1.container().getSchema());
    final ExternalSortBatch sortBatch_local = new ExternalSortBatch(sortPopConfig, fixture_local.getFragmentContext(),
      mockInputBatch);

    assertTrue(sortBatch_local.next() == OK_NEW_SCHEMA);
    assertTrue(sortBatch_local.next() == OK_NEW_SCHEMA);
    assertTrue(sortBatch_local.getRecordCount() == 4);
    assertTrue(sortBatch_local.getSchema().getSelectionVectorMode() == BatchSchema.SelectionVectorMode.NONE);
    assertTrue(sortBatch_local.next() == NONE);

    // Release memory for row sets
    local_nonEmptyInputRowSet1.clear();
    local_nonEmptyInputRowSet2.clear();
    local_nonEmptyInputRowSet3.clear();
    sortBatch_local.close();
    fixture_local.close();
  }
}
