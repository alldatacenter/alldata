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
package org.apache.drill.exec.physical.impl.limit;

import org.apache.drill.categories.OperatorTest;
import org.apache.drill.exec.physical.config.Limit;
import org.apache.drill.exec.physical.impl.MockRecordBatch;
import org.apache.drill.exec.physical.impl.BaseTestOpBatchEmitOutcome;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category(OperatorTest.class)
public class TestLimitBatchEmitOutcome extends BaseTestOpBatchEmitOutcome {

  /**
   * Test to show empty batch with both OK_NEW_SCHEMA and EMIT outcome is not
   * ignored by Limit and is pass through to the downstream operator.
   */
  @Test
  public void testLimitEmptyBatchEmitOutcome() throws Throwable {
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(emptyInputRowSet.container());

    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);


    final MockRecordBatch mockInputBatch = new MockRecordBatch(operatorFixture.getFragmentContext(), opContext,
      inputContainer, inputOutcomes, emptyInputRowSet.container().getSchema());
    // Only set for this Test class
    mockInputBatch.useUnnestKillHandlingForLimit(true);

    final Limit limitConf = new Limit(null, 0, 1);
    @SuppressWarnings("resource")
    final LimitRecordBatch limitBatch = new LimitRecordBatch(limitConf, operatorFixture.getFragmentContext(),
      mockInputBatch);

    assertTrue(limitBatch.next() == RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    outputRecordCount += limitBatch.getRecordCount();
    assertTrue(limitBatch.next() == RecordBatch.IterOutcome.EMIT);
    outputRecordCount += limitBatch.getRecordCount();
    assertEquals(0, outputRecordCount);
  }

  /**
   * Test to validate limit considers all the data until it sees EMIT outcome
   * and return output batch with data that meets the limit criteria.
   */
  @Test
  public void testLimitNonEmptyBatchEmitOutcome() throws Throwable {
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(nonEmptyInputRowSet.container());

    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);


    final MockRecordBatch mockInputBatch = new MockRecordBatch(operatorFixture.getFragmentContext(), opContext,
      inputContainer, inputOutcomes, emptyInputRowSet.container().getSchema());
    // Only set for this Test class
    mockInputBatch.useUnnestKillHandlingForLimit(true);

    final Limit limitConf = new Limit(null, 0, 1);
    @SuppressWarnings("resource")
    final LimitRecordBatch limitBatch = new LimitRecordBatch(limitConf, operatorFixture.getFragmentContext(),
      mockInputBatch);

    assertTrue(limitBatch.next() == RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    outputRecordCount += limitBatch.getRecordCount();
    assertEquals(0, outputRecordCount);
    assertTrue(limitBatch.next() == RecordBatch.IterOutcome.EMIT);
    outputRecordCount += limitBatch.getRecordCount();
    assertEquals(1, outputRecordCount);
  }

  /**
   * Test to show that once a limit number of records is produced using first
   * set of batches then on getting a batch with EMIT outcome, the limit state
   * is again refreshed and applied to next set of batches with data.
   */
  @Test
  public void testLimitResetsAfterFirstEmitOutcome() throws Throwable {
    final RowSet.SingleRowSet nonEmptyInputRowSet2 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(2, 20, "item2")
      .addRow(3, 30, "item3")
      .build();

    inputContainer.add(nonEmptyInputRowSet.container());
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(nonEmptyInputRowSet2.container());

    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);
    inputOutcomes.add(RecordBatch.IterOutcome.OK);

    final MockRecordBatch mockInputBatch = new MockRecordBatch(operatorFixture.getFragmentContext(), opContext,
      inputContainer, inputOutcomes, emptyInputRowSet.container().getSchema());
    // Only set for this Test class
    mockInputBatch.useUnnestKillHandlingForLimit(true);

    final Limit limitConf = new Limit(null, 0, 1);
    @SuppressWarnings("resource")
    final LimitRecordBatch limitBatch = new LimitRecordBatch(limitConf, operatorFixture.getFragmentContext(),
      mockInputBatch);

    assertTrue(limitBatch.next() == RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    assertEquals(1, limitBatch.getRecordCount());
    assertTrue(limitBatch.next() == RecordBatch.IterOutcome.EMIT);

    // State refresh happens and limit again works on new data batches
    assertEquals(0, limitBatch.getRecordCount());
    assertTrue(limitBatch.next() == RecordBatch.IterOutcome.OK);
    assertEquals(1, limitBatch.getRecordCount());
    assertTrue(limitBatch.next() == RecordBatch.IterOutcome.NONE);
  }

  /**
   * Test to show that when the limit number of records is found with first
   * incoming batch, then next empty incoming batch with OK outcome is ignored,
   * but the empty EMIT outcome batch is not ignored. Empty incoming batch with
   * EMIT outcome produces empty output batch with EMIT outcome.
   */
  @Test
  public void testLimitNonEmptyFirst_EmptyOKEmitOutcome() throws Throwable {
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
    // Only set for this Test class
    mockInputBatch.useUnnestKillHandlingForLimit(true);

    final Limit limitConf = new Limit(null, 0, 1);
    @SuppressWarnings("resource")
    final LimitRecordBatch limitBatch = new LimitRecordBatch(limitConf, operatorFixture.getFragmentContext(),
      mockInputBatch);

    assertTrue(limitBatch.next() == RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    assertEquals(1, limitBatch.getRecordCount());
    // OK will not be received since it's was accompanied with empty batch
    assertTrue(limitBatch.next() == RecordBatch.IterOutcome.EMIT);
    assertEquals(0, limitBatch.getRecordCount());
    assertTrue(limitBatch.next() == RecordBatch.IterOutcome.NONE);
  }

  /**
   * Test to show that limit refreshes it's state after seeing first EMIT
   * outcome and works on data batches following it as new set's of incoming
   * batch and apply the limits rule from fresh on those. So for first set of
   * batches with OK_NEW_SCHEMA and EMIT outcome but total number of records
   * received being less than limit condition, it still produces an output with
   * that many records (in this case 1 even though limit number of records is
   * 2).
   * <p>
   * After seeing EMIT, it refreshes it's state and operate on next input
   * batches to again return limit number of records. So for 3rd batch with 2
   * records but with EMIT outcome it produces an output batch with 2 records
   * not with 1 since state is refreshed.
   */
  @Test
  public void testMultipleLimitWithEMITOutcome() throws Throwable {
    final RowSet.SingleRowSet nonEmptyInputRowSet2 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(2, 20, "item2")
      .addRow(3, 30, "item3")
      .build();

    inputContainer.add(nonEmptyInputRowSet.container());
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(nonEmptyInputRowSet2.container());
    inputContainer.add(emptyInputRowSet.container());

    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final MockRecordBatch mockInputBatch = new MockRecordBatch(operatorFixture.getFragmentContext(), opContext,
      inputContainer, inputOutcomes, emptyInputRowSet.container().getSchema());
    // Only set for this Test class
    mockInputBatch.useUnnestKillHandlingForLimit(true);

    final Limit limitConf = new Limit(null, 0, 2);
    @SuppressWarnings("resource")
    final LimitRecordBatch limitBatch = new LimitRecordBatch(limitConf, operatorFixture.getFragmentContext(),
      mockInputBatch);

    // first limit evaluation
    assertTrue(limitBatch.next() == RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    assertEquals(1, limitBatch.getRecordCount());
    assertTrue(limitBatch.next() == RecordBatch.IterOutcome.EMIT);
    assertEquals(0, limitBatch.getRecordCount());

    // After seeing EMIT limit will refresh it's state and again evaluate limit on next set of input batches
    assertTrue(limitBatch.next() == RecordBatch.IterOutcome.EMIT);
    assertEquals(2, limitBatch.getRecordCount());

    // Since limit is hit it will return NONE
    assertTrue(limitBatch.next() == RecordBatch.IterOutcome.NONE);
  }

  /**
   * Test shows that limit operates on multiple input batches until it finds
   * limit number of records or it sees an EMIT outcome to refresh it's state.
   */
  @Test
  public void testLimitNonEmptyFirst_NonEmptyOK_EmptyBatchEmitOutcome() throws Throwable {

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
    // Only set for this Test class
    mockInputBatch.useUnnestKillHandlingForLimit(true);

    final Limit limitConf = new Limit(null, 0, 2);
    @SuppressWarnings("resource")
    final LimitRecordBatch limitBatch = new LimitRecordBatch(limitConf, operatorFixture.getFragmentContext(),
      mockInputBatch);

    assertTrue(limitBatch.next() == RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    assertEquals(1, limitBatch.getRecordCount());
    assertTrue(limitBatch.next() == RecordBatch.IterOutcome.OK);
    assertEquals(1, limitBatch.getRecordCount());
    assertTrue(limitBatch.next() == RecordBatch.IterOutcome.EMIT);
    assertEquals(0, limitBatch.getRecordCount());

    nonEmptyInputRowSet2.clear();
  }
}
