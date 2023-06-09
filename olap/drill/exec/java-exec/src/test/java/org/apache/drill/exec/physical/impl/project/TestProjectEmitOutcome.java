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
package org.apache.drill.exec.physical.impl.project;

import org.apache.drill.categories.OperatorTest;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.physical.config.Project;
import org.apache.drill.exec.physical.impl.MockRecordBatch;
import org.apache.drill.exec.physical.impl.BaseTestOpBatchEmitOutcome;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.proto.UserBitShared.DrillPBError.ErrorType;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category(OperatorTest.class)
public class TestProjectEmitOutcome extends BaseTestOpBatchEmitOutcome {

  /**
   * Test that if empty input batch is received with OK_NEW_SCHEMA or EMIT
   * outcome, then Project doesn't ignores these empty batches and instead
   * return them downstream with correct outcomes.
   *
   * @throws Throwable
   */
  @Test
  public void testProjectEmptyBatchEmitOutcome() throws Throwable {
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(emptyInputRowSet.container());
    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final MockRecordBatch mockInputBatch = new MockRecordBatch(operatorFixture.getFragmentContext(), opContext,
      inputContainer, inputOutcomes, emptyInputRowSet.container().getSchema());

    final Project projectConf = new Project(parseExprs("id_left+5", "id_left"), null);
    try (final ProjectRecordBatch projectBatch = new ProjectRecordBatch(projectConf, mockInputBatch,
      operatorFixture.getFragmentContext());) {

      assertTrue(projectBatch.next() == RecordBatch.IterOutcome.OK_NEW_SCHEMA);
      outputRecordCount += projectBatch.getRecordCount();
      assertTrue(projectBatch.next() == RecordBatch.IterOutcome.EMIT);
      outputRecordCount += projectBatch.getRecordCount();
      assertEquals(0, outputRecordCount);
    }
  }

  /**
   * Test to show if a non-empty batch is accompanied with EMIT outcome then
   * Project operator produces output for that batch and return the output using
   * EMIT outcome.
   *
   * @throws Throwable
   */
  @Test
  public void testProjectNonEmptyBatchEmitOutcome() throws Throwable {
    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(nonEmptyInputRowSet.container());
    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final MockRecordBatch mockInputBatch = new MockRecordBatch(operatorFixture.getFragmentContext(), opContext,
      inputContainer, inputOutcomes, emptyInputRowSet.container().getSchema());

    final Project projectConf = new Project(parseExprs("id_left+5", "id_left"), null);
    try (final ProjectRecordBatch projectBatch = new ProjectRecordBatch(projectConf, mockInputBatch,
        operatorFixture.getFragmentContext());) {

      assertTrue(projectBatch.next() == RecordBatch.IterOutcome.OK_NEW_SCHEMA);
      outputRecordCount += projectBatch.getRecordCount();
      assertTrue(projectBatch.next() == RecordBatch.IterOutcome.EMIT);
      outputRecordCount += projectBatch.getRecordCount();
      assertEquals(1, outputRecordCount);
    }
  }

  /**
   * Test to show that non-empty first batch produces output for that batch with OK_NEW_SCHEMA and later empty batch
   * with EMIT outcome is also passed through rather than getting ignored.
   * @throws Throwable
   */
  @Test
  public void testProjectNonEmptyFirst_EmptyBatchEmitOutcome() throws Throwable {
    inputContainer.add(nonEmptyInputRowSet.container());
    inputContainer.add(emptyInputRowSet.container());
    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final MockRecordBatch mockInputBatch = new MockRecordBatch(operatorFixture.getFragmentContext(), opContext,
      inputContainer, inputOutcomes, emptyInputRowSet.container().getSchema());

    final Project projectConf = new Project(parseExprs("id_left+5", "id_left"), null);
    try (final ProjectRecordBatch projectBatch = new ProjectRecordBatch(projectConf, mockInputBatch,
          operatorFixture.getFragmentContext());) {

      assertTrue(projectBatch.next() == RecordBatch.IterOutcome.OK_NEW_SCHEMA);
      outputRecordCount += projectBatch.getRecordCount();
      assertTrue(projectBatch.next() == RecordBatch.IterOutcome.EMIT);
      outputRecordCount += projectBatch.getRecordCount();
      assertEquals(1, outputRecordCount);
    }
  }

  /**
   * Test to show if an empty batch is accompanied with OK outcome then that batch is ignored by Project operator and
   * it doesn't return anything instead call's next() to get another batch. If the subsequent next() call returns empty
   * batch with EMIT outcome then Project returns the EMIT outcome correctly rather than ignoring it because of empty
   * batch.
   * @throws Throwable
   */
  @Test
  public void testProjectNonEmptyFirst_EmptyOK_EmptyBatchEmitOutcome() throws Throwable {
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

    final Project projectConf = new Project(parseExprs("id_left+5", "id_left"), null);
    try (final ProjectRecordBatch projectBatch = new ProjectRecordBatch(projectConf, mockInputBatch,
            operatorFixture.getFragmentContext());) {

      assertTrue(projectBatch.next() == RecordBatch.IterOutcome.OK_NEW_SCHEMA);
      outputRecordCount += projectBatch.getRecordCount();
      // OK will not be received since it's was accompanied with empty batch
      assertTrue(projectBatch.next() == RecordBatch.IterOutcome.EMIT);
      outputRecordCount += projectBatch.getRecordCount();
      assertTrue(projectBatch.next() == RecordBatch.IterOutcome.NONE);
      assertEquals(1, outputRecordCount);
    }
  }

  /**
   * Test to show that in cases with functions in project list whose output is complex types, if Project sees an EMIT
   * outcome then it fails. This scenario can happen when complex functions are used in subquery between LATERAL and
   * UNNEST. In which case guidance is to use those functions in project list of outermost query.
   * Below test passes first batch as non-empty with OK_NEW_SCHEMA during which complex writers are cached for
   * projected columns and later when an empty batch arrives with EMIT outcome the exception is thrown.
   * @throws Throwable
   */
  @Test
  public void testProjectWithComplexWritersAndEmitOutcome_NonEmptyFirstBatch() throws Throwable {
    final RowSet.SingleRowSet nonEmptyInputRowSet2 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(2, 20, "{ \"a\" : 1 }")
      .build();

    inputContainer.add(nonEmptyInputRowSet2.container());
    inputContainer.add(emptyInputRowSet.container());

    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final MockRecordBatch mockInputBatch = new MockRecordBatch(operatorFixture.getFragmentContext(), opContext,
      inputContainer, inputOutcomes, emptyInputRowSet.container().getSchema());

    final Project projectConf = new Project(parseExprs("convert_fromJSON(name_left)", "name_columns"), null);
    try (final ProjectRecordBatch projectBatch = new ProjectRecordBatch(projectConf, mockInputBatch,
            operatorFixture.getFragmentContext());) {

      assertEquals(RecordBatch.IterOutcome.OK_NEW_SCHEMA, projectBatch.next());
      projectBatch.next(); // Fails
      fail();
    } catch (UserException e) {
      // exception is expected because of complex writers case
      assertEquals(ErrorType.UNSUPPORTED_OPERATION, e.getErrorType());
    }
  }

  /**
   * Test to show that in cases with functions in project list whose output is complex types, if Project sees an EMIT
   * outcome then it fails. This scenario can happen when complex functions are used in subquery between LATERAL and
   * UNNEST. In which case guidance is to use those functions in project list of outermost query.
   *
   * Below test passes first batch as empty with OK_NEW_SCHEMA during which complex writers are not known so far
   * and Project calls next() on upstream to get a batch with data. But later when an empty batch arrives with EMIT
   * outcome the exception is thrown as the scenario is not supported
   * @throws Throwable
   */
  @Test
  public void testProjectWithComplexWritersAndEmitOutcome_EmptyFirstBatch() throws Throwable {
    final RowSet.SingleRowSet nonEmptyInputRowSet2 = operatorFixture.rowSetBuilder(inputSchema)
      .addRow(2, 20, "{ \"a\" : 1 }")
      .build();

    inputContainer.add(emptyInputRowSet.container());
    inputContainer.add(nonEmptyInputRowSet2.container());

    inputOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final MockRecordBatch mockInputBatch = new MockRecordBatch(operatorFixture.getFragmentContext(), opContext,
      inputContainer, inputOutcomes, emptyInputRowSet.container().getSchema());

    final Project projectConf = new Project(parseExprs("convert_fromJSON(name_left)", "name_columns"), null);
    try (final ProjectRecordBatch projectBatch = new ProjectRecordBatch(projectConf, mockInputBatch,
            operatorFixture.getFragmentContext());) {

      projectBatch.next(); // Fails
      fail();
    } catch (UserException e) {
      // exception is expected because of complex writers case
      assertEquals(ErrorType.UNSUPPORTED_OPERATION, e.getErrorType());
    }
  }
}
