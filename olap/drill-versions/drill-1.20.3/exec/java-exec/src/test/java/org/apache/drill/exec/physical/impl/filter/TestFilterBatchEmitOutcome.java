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
package org.apache.drill.exec.physical.impl.filter;

import org.apache.drill.categories.OperatorTest;
import org.apache.drill.exec.physical.config.Filter;
import org.apache.drill.exec.physical.impl.MockRecordBatch;
import org.apache.drill.exec.physical.impl.BaseTestOpBatchEmitOutcome;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category(OperatorTest.class)
public class TestFilterBatchEmitOutcome extends BaseTestOpBatchEmitOutcome {

  /**
   * Test to show if an empty batch is accompanied with EMIT outcome then Filter operator is not ignoring it and
   * asking for next batch with data. Instead it is just returning the empty batch along with EMIT outcome right away.
   *
   * This test also shows that if first batch accompanied with OK_NEW_SCHEMA is empty then it is also pass through by
   * Filter operator rather than ignoring it and waiting for a batch with some data in it.
   * @throws Throwable
   */
  @Test
  public void testFilterEmptyBatchEmitOutcome() throws Throwable {
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(emptyInputRowSet.container());
    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final MockRecordBatch mockInputBatch = new MockRecordBatch(operatorFixture.getFragmentContext(), opContext,
      inputContainer, inputOutcomes, emptyInputRowSet.container().getSchema());

    final Filter filterConf = new Filter(null, parseExpr("id_left=5"), 1.0f);
    final FilterRecordBatch filterRecordBatch = new FilterRecordBatch(filterConf, mockInputBatch,
      operatorFixture.getFragmentContext());

    assertTrue(filterRecordBatch.next() == RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    outputRecordCount += filterRecordBatch.getRecordCount();
    assertTrue(filterRecordBatch.next() == RecordBatch.IterOutcome.EMIT);
    outputRecordCount += filterRecordBatch.getRecordCount();
    assertEquals(0, outputRecordCount);
  }

  /**
   * Test to show if a non-empty batch is accompanied with EMIT outcome then Filter operator produces output for
   * that batch with data matching filter condition and return the output using EMIT outcome.
   * @throws Throwable
   */
  @Test
  public void testFilterNonEmptyBatchEmitOutcome() throws Throwable {
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(nonEmptyInputRowSet.container());
    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final MockRecordBatch mockInputBatch = new MockRecordBatch(operatorFixture.getFragmentContext(), opContext,
      inputContainer, inputOutcomes, emptyInputRowSet.container().getSchema());

    final Filter filterConf = new Filter(null, parseExpr("id_left=1"), 1.0f);
    final FilterRecordBatch filterRecordBatch = new FilterRecordBatch(filterConf, mockInputBatch,
      operatorFixture.getFragmentContext());

    assertTrue(filterRecordBatch.next() == RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    outputRecordCount += filterRecordBatch.getRecordCount();
    assertTrue(filterRecordBatch.next() == RecordBatch.IterOutcome.EMIT);
    outputRecordCount += filterRecordBatch.getRecordCount();
    assertEquals(1, outputRecordCount);
  }

  /**
   * Test to show if a non-empty batch is accompanied with EMIT outcome then Filter operator produces empty output
   * batch since filter condition is not satisfied by any data in incoming batch. This empty output batch is
   * accompanied with EMIT outcome.
   * @throws Throwable
   */
  @Test
  public void testFilterNonEmptyBatchEmitOutcome_WithNonMatchingCondition() throws Throwable {
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(nonEmptyInputRowSet.container());
    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final MockRecordBatch mockInputBatch = new MockRecordBatch(operatorFixture.getFragmentContext(), opContext,
      inputContainer, inputOutcomes, emptyInputRowSet.container().getSchema());

    final Filter filterConf = new Filter(null, parseExpr("id_left=2"), 1.0f);
    final FilterRecordBatch filterRecordBatch = new FilterRecordBatch(filterConf, mockInputBatch,
      operatorFixture.getFragmentContext());

    assertTrue(filterRecordBatch.next() == RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    outputRecordCount += filterRecordBatch.getRecordCount();
    assertTrue(filterRecordBatch.next() == RecordBatch.IterOutcome.EMIT);
    outputRecordCount += filterRecordBatch.getRecordCount();
    assertEquals(0, outputRecordCount);
  }

  /**
   * Test to show that non-empty first batch produces output for that batch with OK_NEW_SCHEMA and later empty batch
   * with EMIT outcome is also passed through rather than getting ignored.
   * @throws Throwable
   */
  @Test
  public void testFilterNonEmptyFirst_EmptyBatchEmitOutcome() throws Throwable {
    inputContainer.add(nonEmptyInputRowSet.container());
    inputContainer.add(emptyInputRowSet.container());
    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final MockRecordBatch mockInputBatch = new MockRecordBatch(operatorFixture.getFragmentContext(), opContext,
      inputContainer, inputOutcomes, emptyInputRowSet.container().getSchema());

    final Filter filterConf = new Filter(null, parseExpr("id_left=1"), 1.0f);
    final FilterRecordBatch filterRecordBatch = new FilterRecordBatch(filterConf, mockInputBatch,
      operatorFixture.getFragmentContext());

    assertTrue(filterRecordBatch.next() == RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    outputRecordCount += filterRecordBatch.getRecordCount();
    assertTrue(filterRecordBatch.next() == RecordBatch.IterOutcome.EMIT);
    outputRecordCount += filterRecordBatch.getRecordCount();
    assertEquals(1, outputRecordCount);
  }

  /**
   * Test to show if an empty batch is accompanied with OK outcome then that batch is ignored by Filter operator and
   * it doesn't return anything instead call's next() to get another batch. If the subsequent next() call returns empty
   * batch with EMIT outcome then Filter returns the EMIT outcome correctly rather than ignoring it because of empty
   * batch.
   * @throws Throwable
   */
  @Test
  public void testFilterNonEmptyFirst_EmptyOK_EmptyBatchEmitOutcome() throws Throwable {
    inputContainer.add(nonEmptyInputRowSet.container());
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(emptyInputRowSet.container());

    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomes.add(RecordBatch.IterOutcome.OK);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);
    inputOutcomes.add(RecordBatch.IterOutcome.NONE);

    final MockRecordBatch mockInputBatch = new MockRecordBatch(operatorFixture.getFragmentContext(), opContext,
      inputContainer, inputOutcomes, emptyInputRowSet.container().getSchema());

    final Filter filterConf = new Filter(null, parseExpr("id_left=1"), 1.0f);
    final FilterRecordBatch filterRecordBatch = new FilterRecordBatch(filterConf, mockInputBatch,
      operatorFixture.getFragmentContext());

    assertTrue(filterRecordBatch.next() == RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    outputRecordCount += filterRecordBatch.getRecordCount();
    // OK will not be received since it's was accompanied with empty batch
    assertTrue(filterRecordBatch.next() == RecordBatch.IterOutcome.EMIT);
    outputRecordCount += filterRecordBatch.getRecordCount();
    assertTrue(filterRecordBatch.next() == RecordBatch.IterOutcome.NONE);
    assertEquals(1, outputRecordCount);
  }

  /**
   * Test to show empty batch with OK outcome is ignore and later non-empty batch with OK outcome produces an output
   * batch. Whereas a empty batch with EMIT outcome is not ignored and a empty output batch is returned with EMIT
   * outcome.
   * @throws Throwable
   */
  @Test
  public void testFilterNonEmptyFirst_NonEmptyOK_EmptyBatchEmitOutcome() throws Throwable {
    final RowSet.SingleRowSet nonEmptyInputRowSet2 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(2, 20, "item2")
      .build();

    inputContainer.add(nonEmptyInputRowSet.container());
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(nonEmptyInputRowSet2.container());
    inputContainer.add(emptyInputRowSet.container());

    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomes.add(RecordBatch.IterOutcome.OK);
    inputOutcomes.add(RecordBatch.IterOutcome.OK);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final MockRecordBatch mockInputBatch = new MockRecordBatch(operatorFixture.getFragmentContext(), opContext,
      inputContainer, inputOutcomes, emptyInputRowSet.container().getSchema());

    final Filter filterConf = new Filter(null, parseExpr("id_left>=1"), 1.0f);
    final FilterRecordBatch filterRecordBatch = new FilterRecordBatch(filterConf, mockInputBatch,
      operatorFixture.getFragmentContext());

    assertTrue(filterRecordBatch.next() == RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    outputRecordCount += filterRecordBatch.getRecordCount();
    assertTrue(filterRecordBatch.next() == RecordBatch.IterOutcome.OK);
    outputRecordCount += filterRecordBatch.getRecordCount();
    assertTrue(filterRecordBatch.next() == RecordBatch.IterOutcome.EMIT);
    outputRecordCount += filterRecordBatch.getRecordCount();
    assertEquals(2, outputRecordCount);

    // free up resources
    nonEmptyInputRowSet2.clear();
  }
}
