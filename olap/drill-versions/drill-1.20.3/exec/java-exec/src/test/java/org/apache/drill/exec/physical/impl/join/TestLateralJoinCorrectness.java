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

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.drill.categories.OperatorTest;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.config.LateralJoinPOP;
import org.apache.drill.exec.physical.impl.MockRecordBatch;
import org.apache.drill.exec.planner.common.DrillLateralJoinRelBase;
import org.apache.drill.exec.proto.UserBitShared.DrillPBError.ErrorType;
import org.apache.drill.exec.record.CloseableRecordBatch;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.mock.MockStorePOP;
import org.apache.drill.test.SubOperatorTest;
import org.apache.drill.exec.physical.rowSet.DirectRowSet;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.test.rowSet.RowSetComparison;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category(OperatorTest.class)
public class TestLateralJoinCorrectness extends SubOperatorTest {

  // Operator Context for mock batch
  private static OperatorContext operatorContext;

  // Left Batch Schema
  private static TupleMetadata leftSchema;

  // Right Batch Schema
  private static TupleMetadata rightSchema;

  // Empty left RowSet
  private static RowSet.SingleRowSet emptyLeftRowSet;

  // Non-Empty left RowSet
  private static RowSet.SingleRowSet nonEmptyLeftRowSet;

  // List of left incoming containers
  private static final List<VectorContainer> leftContainer = new ArrayList<>(5);

  // List of left IterOutcomes
  private static final List<RecordBatch.IterOutcome> leftOutcomes = new ArrayList<>(5);

  // Empty right RowSet
  private static RowSet.SingleRowSet emptyRightRowSet;

  // Non-Empty right RowSet
  private static RowSet.SingleRowSet nonEmptyRightRowSet;

  // List of right incoming containers
  private static final List<VectorContainer> rightContainer = new ArrayList<>(5);

  // List of right IterOutcomes
  private static final List<RecordBatch.IterOutcome> rightOutcomes = new ArrayList<>(5);

  // Lateral Join POP Config
  private static LateralJoinPOP ljPopConfig;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    PhysicalOperator mockPopConfig = new MockStorePOP(null);
    operatorContext = fixture.newOperatorContext(mockPopConfig);

    ljPopConfig = new LateralJoinPOP(null, null, JoinRelType.INNER,
      DrillLateralJoinRelBase.IMPLICIT_COLUMN, Lists.newArrayList());

    leftSchema = new SchemaBuilder()
      .add("id_left", TypeProtos.MinorType.INT)
      .add("cost_left", TypeProtos.MinorType.INT)
      .add("name_left", TypeProtos.MinorType.VARCHAR)
      .buildSchema();
    emptyLeftRowSet = fixture.rowSetBuilder(leftSchema).build();

    rightSchema = new SchemaBuilder()
      .add(ljPopConfig.getImplicitRIDColumn(), TypeProtos.MinorType.INT)
      .add("id_right", TypeProtos.MinorType.INT)
      .add("cost_right", TypeProtos.MinorType.INT)
      .add("name_right", TypeProtos.MinorType.VARCHAR)
      .buildSchema();
    emptyRightRowSet = fixture.rowSetBuilder(rightSchema).build();
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    operatorContext.close();
    emptyLeftRowSet.clear();
    emptyRightRowSet.clear();
  }


  @Before
  public void beforeTest() throws Exception {
    nonEmptyLeftRowSet = fixture.rowSetBuilder(leftSchema)
      .addRow(1, 10, "item1")
      .build();

    nonEmptyRightRowSet = fixture.rowSetBuilder(rightSchema)
      .addRow(1, 1, 11, "item11")
      .addRow(1, 2, 21, "item21")
      .addRow(1, 3, 31, "item31")
      .build();
  }

  @After
  public void afterTest() throws Exception {
    nonEmptyLeftRowSet.clear();
    nonEmptyRightRowSet.clear();
    leftContainer.clear();
    leftOutcomes.clear();
    rightContainer.clear();
    rightOutcomes.clear();
  }

  /**
   * Helper method to check if input outcome is one of terminal state or not
   *
   * @param outcome - input IterOutcome state to check for
   * @return
   */
  private boolean isTerminal(RecordBatch.IterOutcome outcome) {
    return (outcome == RecordBatch.IterOutcome.NONE);
  }

  /**
   * With both left and right batch being empty, the {@link LateralJoinBatch#buildSchema()} phase
   * should still build the output container schema and return empty batch
   *
   * @throws Exception
   */
  @Test
  public void testBuildSchemaEmptyLRBatch() throws Exception {

    // Get the left container with dummy data for Lateral Join
    leftContainer.add(emptyLeftRowSet.container());

    // Get the left IterOutcomes for Lateral Join
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);

    // Create Left MockRecordBatch
    final CloseableRecordBatch leftMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer, leftOutcomes, emptyLeftRowSet.container().getSchema());

    // Get the right container with dummy data
    rightContainer.add(emptyRightRowSet.container());

    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);

    final CloseableRecordBatch rightMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      rightContainer, rightOutcomes, emptyRightRowSet.container().getSchema());

    final LateralJoinBatch ljBatch = new LateralJoinBatch(ljPopConfig, fixture.getFragmentContext(),
      leftMockBatch, rightMockBatch);

    try {
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == ljBatch.next());
      assertTrue(ljBatch.getRecordCount() == 0);

      while (!isTerminal(ljBatch.next())) {
        // do nothing
      }
    } catch (AssertionError | Exception error) {
      fail();
    } finally {
      // Close all the resources for this test case
      ljBatch.close();
      leftMockBatch.close();
      rightMockBatch.close();
    }
  }

  /**
   * With both left and right batch being non-empty, the {@link LateralJoinBatch#buildSchema()} phase
   * will fail with {@link IllegalStateException} since it expects first batch from right branch of LATERAL
   * to be always empty
   *
   * @throws Exception
   */
  @Test
  public void testBuildSchemaNonEmptyLRBatch() throws Exception {

    // Get the left container with dummy data
    leftContainer.add(nonEmptyLeftRowSet.container());

    // Get the left IterOutcomes for Lateral Join
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);

    // Create Left MockRecordBatch
    final CloseableRecordBatch leftMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer, leftOutcomes, leftContainer.get(0).getSchema());

    // Get the right container with dummy data
    rightContainer.add(nonEmptyRightRowSet.container());
    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);

    final CloseableRecordBatch rightMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      rightContainer, rightOutcomes, rightContainer.get(0).getSchema());

    final LateralJoinBatch ljBatch = new LateralJoinBatch(ljPopConfig, fixture.getFragmentContext(),
      leftMockBatch, rightMockBatch);

    try {
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == ljBatch.next());
      fail();
    } catch (AssertionError | Exception error) {
      // Expected since first right batch is supposed to be empty
      assertTrue(error instanceof IllegalStateException);
    } finally {
      // Close all the resources for this test case
      ljBatch.close();
      leftMockBatch.close();
      rightMockBatch.close();
    }
  }

  /**
   * With non-empty left and empty right batch, the {@link LateralJoinBatch#buildSchema()} phase
   * should still build the output container schema and return empty batch
   *
   * @throws Exception
   */
  @Test
  public void testBuildSchemaNonEmptyLEmptyRBatch() throws Exception {

    // Get the left container with dummy data for Lateral Join
    leftContainer.add(nonEmptyLeftRowSet.container());

    // Get the left IterOutcomes for Lateral Join
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);

    // Create Left MockRecordBatch
    final CloseableRecordBatch leftMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer, leftOutcomes, leftContainer.get(0).getSchema());

    // Get the right container with dummy data
    rightContainer.add(emptyRightRowSet.container());
    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);

    final CloseableRecordBatch rightMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      rightContainer, rightOutcomes, rightContainer.get(0).getSchema());

    final LateralJoinBatch ljBatch = new LateralJoinBatch(ljPopConfig, fixture.getFragmentContext(),
      leftMockBatch, rightMockBatch);

    try {
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == ljBatch.next());
      assertTrue(ljBatch.getRecordCount() == 0);

      // Since Right batch is empty it should drain left and return NONE
      assertTrue(RecordBatch.IterOutcome.NONE == ljBatch.next());

    } catch (AssertionError | Exception error) {
      fail();
    } finally {
      // Close all the resources for this test case
      ljBatch.close();
      leftMockBatch.close();
      rightMockBatch.close();
    }
  }

  /**
   * This case should never happen since With empty left there cannot be non-empty right batch, the
   * {@link LateralJoinBatch#buildSchema()} phase should fail() with {@link IllegalStateException}
   *
   * @throws Exception
   */
  @Test
  public void testBuildSchemaEmptyLNonEmptyRBatch() throws Exception {

    // Get the left container with dummy data for Lateral Join
    leftContainer.add(emptyLeftRowSet.container());

    // Get the left IterOutcomes for Lateral Join
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);

    // Create Left MockRecordBatch
    final CloseableRecordBatch leftMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer, leftOutcomes, leftContainer.get(0).getSchema());

    // Get the right container with dummy data
    rightContainer.add(nonEmptyRightRowSet.container());
    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);

    final CloseableRecordBatch rightMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      rightContainer, rightOutcomes, rightContainer.get(0).getSchema());

    final LateralJoinBatch ljBatch = new LateralJoinBatch(ljPopConfig, fixture.getFragmentContext(),
      leftMockBatch, rightMockBatch);

    try {
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == ljBatch.next());
      fail();

    } catch (AssertionError | Exception error) {
      // Expected since first right batch is supposed to be empty
      assertTrue(error instanceof IllegalStateException);
    } finally {
      // Close all the resources for this test case
      ljBatch.close();
      leftMockBatch.close();
      rightMockBatch.close();
    }
  }

  /**
   * Test for receiving unexpected EMIT outcome during build phase on left side.
   * @throws Exception
   */
  @Test
  public void testBuildSchemaWithEMITOutcome() throws Exception {
    // Get the left container with dummy data for Lateral Join
    leftContainer.add(emptyLeftRowSet.container());

    // Get the left IterOutcomes for Lateral Join
    leftOutcomes.add(RecordBatch.IterOutcome.EMIT);

    // Create Left MockRecordBatch
    final CloseableRecordBatch leftMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer, leftOutcomes, leftContainer.get(0).getSchema());

    // Get the right container with dummy data
    rightContainer.add(nonEmptyRightRowSet.container());
    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);

    final CloseableRecordBatch rightMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      rightContainer, rightOutcomes, rightContainer.get(0).getSchema());

    final LateralJoinBatch ljBatch = new LateralJoinBatch(ljPopConfig, fixture.getFragmentContext(),
      leftMockBatch, rightMockBatch);

    try {
      ljBatch.next();
      fail();
    } catch (AssertionError | Exception error) {
      // Expected since first right batch is supposed to be empty
      assertTrue(error instanceof IllegalStateException);
    } finally {
      // Close all the resources for this test case
      ljBatch.close();
      leftMockBatch.close();
      rightMockBatch.close();
    }
  }

  /**
   * Test to show correct IterOutcome produced by LATERAL when one record in left batch produces only 1 right batch
   * with EMIT outcome. Then output of LATERAL should be produced by OK outcome after the join. It verifies the number
   * of records in the output batch based on left and right input batches.
   *
   * @throws Exception
   */
  @Test
  public void test1RecordLeftBatchTo1RightRecordBatch() throws Exception {
    // Get the left container with dummy data for Lateral Join
    leftContainer.add(nonEmptyLeftRowSet.container());

    // Get the left IterOutcomes for Lateral Join
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);

    // Create Left MockRecordBatch
    final CloseableRecordBatch leftMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer, leftOutcomes, leftContainer.get(0).getSchema());

    // Get the right container with dummy data
    rightContainer.add(emptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet.container());

    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final CloseableRecordBatch rightMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      rightContainer, rightOutcomes, rightContainer.get(0).getSchema());

    final LateralJoinBatch ljBatch = new LateralJoinBatch(ljPopConfig, fixture.getFragmentContext(),
      leftMockBatch, rightMockBatch);

    try {
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == ljBatch.next());
      assertTrue(RecordBatch.IterOutcome.OK == ljBatch.next());
      assertTrue(ljBatch.getRecordCount() == (nonEmptyLeftRowSet.rowCount() * nonEmptyRightRowSet.rowCount()));
      assertTrue(RecordBatch.IterOutcome.NONE == ljBatch.next());
    } catch (AssertionError | Exception error) {
      fail();
    } finally {
      // Close all the resources for this test case
      ljBatch.close();
      leftMockBatch.close();
      rightMockBatch.close();
    }
  }

  /**
   * Test to show correct IterOutcome & output produced by LATERAL when one record in left batch produces 2 right
   * batches with OK and EMIT outcome respectively. Then output of LATERAL should be produced with OK outcome after the
   * join is done using both right batch with the left batch. It verifies the number of records in the output batch
   * based on left and right input batches.
   *
   * @throws Exception
   */
  @Test
  public void test1RecordLeftBatchTo2RightRecordBatch() throws Exception {
    // Get the left container with dummy data for Lateral Join
    leftContainer.add(nonEmptyLeftRowSet.container());

    // Get the left IterOutcomes for Lateral Join
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);

    // Create Left MockRecordBatch
    final CloseableRecordBatch leftMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer, leftOutcomes, leftContainer.get(0).getSchema());

    // Get the right container with dummy data
    final RowSet.SingleRowSet nonEmptyRightRowSet2 = fixture.rowSetBuilder(rightSchema)
      .addRow(1, 4, 41, "item41")
      .addRow(1, 5, 51, "item51")
      .build();

    rightContainer.add(emptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet2.container());

    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    rightOutcomes.add(RecordBatch.IterOutcome.OK);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final CloseableRecordBatch rightMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      rightContainer, rightOutcomes, rightContainer.get(0).getSchema());

    final LateralJoinBatch ljBatch = new LateralJoinBatch(ljPopConfig, fixture.getFragmentContext(),
      leftMockBatch, rightMockBatch);

    try {
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == ljBatch.next());
      assertTrue(RecordBatch.IterOutcome.OK == ljBatch.next());
      assertTrue(ljBatch.getRecordCount() ==
        (nonEmptyLeftRowSet.rowCount() * (nonEmptyRightRowSet.rowCount() + nonEmptyRightRowSet2.rowCount())));
      assertTrue(RecordBatch.IterOutcome.NONE == ljBatch.next());
    } catch (AssertionError | Exception error) {
      fail();
    } finally {
      // Close all the resources for this test case
      ljBatch.close();
      leftMockBatch.close();
      rightMockBatch.close();
      nonEmptyRightRowSet2.clear();
    }
  }

  /**
   * Test to show that if the result of cross join between left batch and empty right batch is an empty
   * batch and next batch on left side result in NONE outcome, then there is no output produced and result
   * returned to downstream is NONE.
   *
   * @throws Exception
   */
  @Test
  public void test1RecordLeftBatchToEmptyRightBatch() throws Exception {

    // Get the left container with dummy data for Lateral Join
    leftContainer.add(nonEmptyLeftRowSet.container());

    // Get the left IterOutcomes for Lateral Join
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);

    // Create Left MockRecordBatch
    final CloseableRecordBatch leftMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer, leftOutcomes, leftContainer.get(0).getSchema());

    // Get the right container with dummy data
    rightContainer.add(emptyRightRowSet.container());
    rightContainer.add(emptyRightRowSet.container());

    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final CloseableRecordBatch rightMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      rightContainer, rightOutcomes, rightContainer.get(0).getSchema());

    final LateralJoinBatch ljBatch = new LateralJoinBatch(ljPopConfig, fixture.getFragmentContext(),
      leftMockBatch, rightMockBatch);

    try {
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == ljBatch.next());
      assertTrue(RecordBatch.IterOutcome.NONE == ljBatch.next());
    } catch (AssertionError | Exception error) {
      fail();
    } finally {
      // Close all the resources for this test case
      ljBatch.close();
      leftMockBatch.close();
      rightMockBatch.close();
    }
  }

  /**
   * Test to show LATERAL tries to pack the output batch until it's full or all the data is consumed from left and
   * right side. We have multiple left and right batches which fits after join inside the same output batch, hence
   * LATERAL only generates one output batch.
   *
   * @throws Exception
   */
  @Test
  public void testFillingUpOutputBatch() throws Exception {
    // Create data for left input
    final RowSet.SingleRowSet leftRowSet2 = fixture.rowSetBuilder(leftSchema)
      .addRow(2, 20, "item20")
      .build();

    // Create data for right input
    final RowSet.SingleRowSet nonEmptyRightRowSet2 = fixture.rowSetBuilder(rightSchema)
      .addRow(1, 4, 41, "item41")
      .addRow(1, 5, 51, "item51")
      .build();

    // Get the left container with dummy data for Lateral Join
    leftContainer.add(nonEmptyLeftRowSet.container());
    leftContainer.add(leftRowSet2.container());

    // Get the left IterOutcomes for Lateral Join
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    leftOutcomes.add(RecordBatch.IterOutcome.OK);

    // Create Left MockRecordBatch
    final CloseableRecordBatch leftMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer, leftOutcomes, leftContainer.get(0).getSchema());

    // Get the right container with dummy data
    rightContainer.add(emptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet2.container());

    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final CloseableRecordBatch rightMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      rightContainer, rightOutcomes, rightContainer.get(0).getSchema());

    final LateralJoinBatch ljBatch = new LateralJoinBatch(ljPopConfig, fixture.getFragmentContext(),
      leftMockBatch, rightMockBatch);

    try {
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == ljBatch.next());
      assertTrue(RecordBatch.IterOutcome.OK == ljBatch.next());
      assertTrue(ljBatch.getRecordCount() ==
        (nonEmptyLeftRowSet.rowCount() * nonEmptyRightRowSet.rowCount() +
          leftRowSet2.rowCount() * nonEmptyRightRowSet2.rowCount()));
      assertTrue(RecordBatch.IterOutcome.NONE == ljBatch.next());
    } catch (AssertionError | Exception error) {
      fail();
    } finally {
      // Close all the resources for this test case
      ljBatch.close();
      leftMockBatch.close();
      rightMockBatch.close();
      leftRowSet2.clear();
      nonEmptyRightRowSet2.clear();
    }
  }

  /**
   * When multiple left batches are received with different schema, then LATERAL produces output for each schema type
   * separately (even though output batch is not filled completely) and handles the schema change in left batch.
   * Moreover in this case the schema change was only for columns which are not produced by the UNNEST or right branch.
   *
   * @throws Exception
   */
  @Test
  public void testHandlingSchemaChangeForNonUnnestField() throws Exception {

    // Create left input schema 2
    TupleMetadata leftSchema2 = new SchemaBuilder()
      .add("id_left", TypeProtos.MinorType.INT)
      .add("cost_left", TypeProtos.MinorType.VARCHAR)
      .add("name_left", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    final RowSet.SingleRowSet leftRowSet2 = fixture.rowSetBuilder(leftSchema2)
      .addRow(2, "20", "item20")
      .build();

    // Create data for right input
    final RowSet.SingleRowSet nonEmptyRightRowSet2 = fixture.rowSetBuilder(rightSchema)
      .addRow(1, 4, 41, "item41")
      .addRow(1, 5, 51, "item51")
      .build();

    // Get the left container with dummy data for Lateral Join
    leftContainer.add(nonEmptyLeftRowSet.container());
    leftContainer.add(leftRowSet2.container());

    // Get the left IterOutcomes for Lateral Join
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);

    // Create Left MockRecordBatch
    final CloseableRecordBatch leftMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer, leftOutcomes, leftContainer.get(0).getSchema());

    // Get the right container with dummy data
    rightContainer.add(emptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet2.container());

    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final CloseableRecordBatch rightMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      rightContainer, rightOutcomes, rightContainer.get(0).getSchema());

    final LateralJoinBatch ljBatch = new LateralJoinBatch(ljPopConfig, fixture.getFragmentContext(),
      leftMockBatch, rightMockBatch);

    try {
      int totalRecordCount = 0;
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == ljBatch.next());
      assertTrue(RecordBatch.IterOutcome.OK == ljBatch.next());
      totalRecordCount += ljBatch.getRecordCount();
      // This means 2 output record batches were received because of Schema change
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == ljBatch.next());
      totalRecordCount += ljBatch.getRecordCount();
      assertTrue(totalRecordCount ==
        (nonEmptyLeftRowSet.rowCount() * nonEmptyRightRowSet.rowCount() +
          leftRowSet2.rowCount() * nonEmptyRightRowSet2.rowCount()));
      assertTrue(RecordBatch.IterOutcome.NONE == ljBatch.next());
    } catch (AssertionError | Exception error) {
      fail();
    } finally {
      // Close all the resources for this test case
      ljBatch.close();
      leftMockBatch.close();
      rightMockBatch.close();
      leftRowSet2.clear();
      nonEmptyRightRowSet2.clear();
    }
  }

  /**
   * When multiple left and right batches are received with different schema, then LATERAL produces output for each
   * schema type separately (even though output batch is not filled completely) and handles the schema change correctly.
   * Moreover in this case the schema change was for both left and right branch (produced by UNNEST with empty batch).
   *
   * @throws Exception
   */
  @Test
  public void testHandlingSchemaChangeForUnnestField() throws Exception {
    // Create left input schema 2
    TupleMetadata leftSchema2 = new SchemaBuilder()
      .add("id_left", TypeProtos.MinorType.INT)
      .add("cost_left", TypeProtos.MinorType.VARCHAR)
      .add("name_left", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    // Create right input schema
    TupleMetadata rightSchema2 = new SchemaBuilder()
      .add(ljPopConfig.getImplicitRIDColumn(), TypeProtos.MinorType.INT)
      .add("id_right", TypeProtos.MinorType.INT)
      .add("cost_right", TypeProtos.MinorType.VARCHAR)
      .add("name_right", TypeProtos.MinorType.VARCHAR)
      .buildSchema();


    // Create data for left input
    final RowSet.SingleRowSet leftRowSet2 = fixture.rowSetBuilder(leftSchema2)
      .addRow(2, "20", "item20")
      .build();

    // Create data for right input
    final RowSet.SingleRowSet emptyRightRowSet2 = fixture.rowSetBuilder(rightSchema2)
      .build();

    final RowSet.SingleRowSet nonEmptyRightRowSet2 = fixture.rowSetBuilder(rightSchema2)
      .addRow(1, 4, "41", "item41")
      .addRow(1, 5, "51", "item51")
      .build();

    // Get the left container with dummy data for Lateral Join
    leftContainer.add(nonEmptyLeftRowSet.container());
    leftContainer.add(leftRowSet2.container());

    // Get the left IterOutcomes for Lateral Join
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);

    // Create Left MockRecordBatch
    final CloseableRecordBatch leftMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer, leftOutcomes, leftContainer.get(0).getSchema());

    // Get the right container with dummy data
    // first OK_NEW_SCHEMA batch
    rightContainer.add(emptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet.container());
    // second OK_NEW_SCHEMA batch. Right side batch for OK_New_Schema is always empty
    rightContainer.add(emptyRightRowSet2.container());
    rightContainer.add(nonEmptyRightRowSet2.container());

    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);
    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final CloseableRecordBatch rightMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      rightContainer, rightOutcomes, rightContainer.get(0).getSchema());

    final LateralJoinBatch ljBatch = new LateralJoinBatch(ljPopConfig, fixture.getFragmentContext(),
      leftMockBatch, rightMockBatch);

    try {
      int totalRecordCount = 0;
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == ljBatch.next());
      assertTrue(RecordBatch.IterOutcome.OK == ljBatch.next());
      totalRecordCount += ljBatch.getRecordCount();
      // This means 2 output record batches were received because of Schema change
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == ljBatch.next());
      totalRecordCount += ljBatch.getRecordCount();
      assertTrue(RecordBatch.IterOutcome.OK == ljBatch.next());
      totalRecordCount += ljBatch.getRecordCount();
      assertTrue(totalRecordCount ==
        (nonEmptyLeftRowSet.rowCount() * nonEmptyRightRowSet.rowCount() +
          leftRowSet2.rowCount() * nonEmptyRightRowSet2.rowCount()));
      assertTrue(RecordBatch.IterOutcome.NONE == ljBatch.next());
    } catch (AssertionError | Exception error) {
      //fail();
      throw error;
    } finally {
      // Close all the resources for this test case
      ljBatch.close();
      leftMockBatch.close();
      rightMockBatch.close();
      leftRowSet2.clear();
      emptyRightRowSet2.clear();
      nonEmptyRightRowSet2.clear();
    }
  }

  /**
   * Verify if there is no schema change on left side and LATERAL still sees an unexpected schema change on right side
   * then it handles it correctly.
   * handle it corr
   * @throws Exception
   */
  @Test
  public void testHandlingUnexpectedSchemaChangeForUnnestField() throws Exception {
    // Create left input schema 2
    TupleMetadata leftSchema2 = new SchemaBuilder()
      .add("id_left", TypeProtos.MinorType.INT)
      .add("cost_left", TypeProtos.MinorType.VARCHAR)
      .add("name_left", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    // Create right input schema
    TupleMetadata rightSchema2 = new SchemaBuilder()
      .add(ljPopConfig.getImplicitRIDColumn(), TypeProtos.MinorType.INT)
      .add("id_right", TypeProtos.MinorType.INT)
      .add("cost_right", TypeProtos.MinorType.VARCHAR)
      .add("name_right", TypeProtos.MinorType.VARCHAR)
      .buildSchema();


    // Create data for left input
    final RowSet.SingleRowSet leftRowSet2 = fixture.rowSetBuilder(leftSchema2)
      .addRow(2, "20", "item20")
      .build();

    // Create data for right input
    final RowSet.SingleRowSet emptyRightRowSet2 = fixture.rowSetBuilder(rightSchema2)
      .build();

    final RowSet.SingleRowSet nonEmptyRightRowSet2 = fixture.rowSetBuilder(rightSchema2)
      .addRow(1, 4, "41", "item41")
      .addRow(1, 5, "51", "item51")
      .build();

    // Get the left container with dummy data for Lateral Join
    leftContainer.add(nonEmptyLeftRowSet.container());
    leftContainer.add(leftRowSet2.container());

    // Get the left IterOutcomes for Lateral Join
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    leftOutcomes.add(RecordBatch.IterOutcome.OK);

    // Create Left MockRecordBatch
    final CloseableRecordBatch leftMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer, leftOutcomes, leftContainer.get(0).getSchema());

    // Get the right container with dummy data
    // first OK_NEW_SCHEMA batch
    rightContainer.add(emptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet.container());
    // second OK_NEW_SCHEMA batch. Right side batch for OK_New_Schema is always empty
    rightContainer.add(emptyRightRowSet2.container());
    rightContainer.add(nonEmptyRightRowSet2.container());

    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    rightOutcomes.add(RecordBatch.IterOutcome.OK);
    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final CloseableRecordBatch rightMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      rightContainer, rightOutcomes, rightContainer.get(0).getSchema());

    final LateralJoinBatch ljBatch = new LateralJoinBatch(ljPopConfig, fixture.getFragmentContext(),
      leftMockBatch, rightMockBatch);

    try {
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == ljBatch.next());
      ljBatch.next();
      fail();
    } catch (AssertionError | Exception error) {
      // Expected since first right batch is supposed to be empty
      assertTrue(error instanceof IllegalStateException);
    } finally {
      // Close all the resources for this test case
      ljBatch.close();
      leftMockBatch.close();
      rightMockBatch.close();
      leftRowSet2.clear();
      emptyRightRowSet2.clear();
      nonEmptyRightRowSet2.clear();
    }
  }

  /**
   * When multiple left batch is received with same schema but with OK_NEW_SCHEMA, then LATERAL rebuilds the
   * schema each time and sends output in multiple output batches
   * The schema change was only for columns which are not produced by the UNNEST or right branch.
   *
   * @throws Exception
   */
  @Test
  public void testOK_NEW_SCHEMA_WithNoActualSchemaChange_ForNonUnnestField() throws Exception {

    final RowSet.SingleRowSet leftRowSet2 = fixture.rowSetBuilder(leftSchema)
      .addRow(2, 20, "item20")
      .build();

    // Create data for right input
    final RowSet.SingleRowSet nonEmptyRightRowSet2 = fixture.rowSetBuilder(rightSchema)
      .addRow(1, 4, 41, "item41")
      .addRow(1, 5, 51, "item51")
      .build();

    // Get the left container with dummy data for Lateral Join
    leftContainer.add(nonEmptyLeftRowSet.container());
    leftContainer.add(leftRowSet2.container());

    // Get the left IterOutcomes for Lateral Join
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);

    // Create Left MockRecordBatch
    final CloseableRecordBatch leftMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer, leftOutcomes, leftContainer.get(0).getSchema());

    // Get the right container with dummy data
    rightContainer.add(emptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet2.container());

    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final CloseableRecordBatch rightMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      rightContainer, rightOutcomes, rightContainer.get(0).getSchema());

    final LateralJoinBatch ljBatch = new LateralJoinBatch(ljPopConfig, fixture.getFragmentContext(),
      leftMockBatch, rightMockBatch);

    try {
      int totalRecordCount = 0;
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == ljBatch.next());
      assertTrue(RecordBatch.IterOutcome.OK == ljBatch.next());
      totalRecordCount += ljBatch.getRecordCount();
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == ljBatch.next());
      totalRecordCount += ljBatch.getRecordCount();
      assertTrue(totalRecordCount ==
        (nonEmptyLeftRowSet.rowCount() * nonEmptyRightRowSet.rowCount() +
          leftRowSet2.rowCount() * nonEmptyRightRowSet2.rowCount()));
      // This means only 1 output record batch was received.
      assertTrue(RecordBatch.IterOutcome.NONE == ljBatch.next());
    } catch (AssertionError | Exception error) {
      fail();
    } finally {
      // Close all the resources for this test case
      ljBatch.close();
      leftMockBatch.close();
      rightMockBatch.close();
      leftRowSet2.clear();
      nonEmptyRightRowSet2.clear();
    }
  }

  /**
   * When multiple left batch is received with same schema but with OK_NEW_SCHEMA, then LATERAL correctly
   * handles it by re-creating the schema and producing multiple batches of final output
   * The schema change is for columns common on both left and right side.
   *
   * @throws Exception
   */
  @Test
  public void testOK_NEW_SCHEMA_WithNoActualSchemaChange_ForUnnestField() throws Exception {
    // Create data for left input
    final RowSet.SingleRowSet leftRowSet2 = fixture.rowSetBuilder(leftSchema)
      .addRow(2, 20, "item20")
      .build();

    // Create data for right input
    final RowSet.SingleRowSet nonEmptyRightRowSet2 = fixture.rowSetBuilder(rightSchema)
      .addRow(1, 4, 41, "item41")
      .addRow(1, 5, 51, "item51")
      .build();

    // Get the left container with dummy data for Lateral Join
    leftContainer.add(nonEmptyLeftRowSet.container());
    leftContainer.add(leftRowSet2.container());

    // Get the left IterOutcomes for Lateral Join
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);

    // Create Left MockRecordBatch
    final CloseableRecordBatch leftMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer, leftOutcomes, leftContainer.get(0).getSchema());

    // Get the right container with dummy data
    rightContainer.add(emptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet.container());
    rightContainer.add(emptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet2.container());

    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);
    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final CloseableRecordBatch rightMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      rightContainer, rightOutcomes, rightContainer.get(0).getSchema());

    final LateralJoinBatch ljBatch = new LateralJoinBatch(ljPopConfig, fixture.getFragmentContext(),
      leftMockBatch, rightMockBatch);

    try {
      int totalRecordCount = 0;
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == ljBatch.next());
      assertTrue(RecordBatch.IterOutcome.OK == ljBatch.next());
      totalRecordCount += ljBatch.getRecordCount();
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == ljBatch.next());
      assertTrue(RecordBatch.IterOutcome.OK == ljBatch.next());
      totalRecordCount += ljBatch.getRecordCount();
      assertTrue(totalRecordCount ==
        (nonEmptyLeftRowSet.rowCount() * nonEmptyRightRowSet.rowCount() +
          leftRowSet2.rowCount() * nonEmptyRightRowSet2.rowCount()));
      // This means only 1 output record batch was received.
      assertTrue(RecordBatch.IterOutcome.NONE == ljBatch.next());
    } catch (AssertionError | Exception error) {
      fail();
    } finally {
      // Close all the resources for this test case
      ljBatch.close();
      leftMockBatch.close();
      rightMockBatch.close();
      leftRowSet2.clear();
      nonEmptyRightRowSet2.clear();
    }
  }


  /**
   * When multiple left batch is received with same schema but with OK_NEW_SCHEMA, then LATERAL detects that
   * correctly and suppresses schema change operation by producing output in same batch created with initial schema.
   * The schema change was only for columns which are not produced by the UNNEST or right branch.
   *
   * @throws Exception
   */
  @Test
  public void testHandlingEMITFromLeft() throws Exception {

    final RowSet.SingleRowSet leftRowSet2 = fixture.rowSetBuilder(leftSchema)
      .addRow(3, 30, "item30")
      .build();

    // Create data for right input
    final RowSet.SingleRowSet nonEmptyRightRowSet2 = fixture.rowSetBuilder(rightSchema)
      .addRow(1, 4, 41, "item41")
      .addRow(1, 5, 51, "item51")
      .build();

    // Get the left container with dummy data for Lateral Join
    leftContainer.add(emptyLeftRowSet.container());
    leftContainer.add(nonEmptyLeftRowSet.container());
    leftContainer.add(leftRowSet2.container());

    // Get the left IterOutcomes for Lateral Join
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    leftOutcomes.add(RecordBatch.IterOutcome.EMIT);
    leftOutcomes.add(RecordBatch.IterOutcome.EMIT);

    // Create Left MockRecordBatch
    final CloseableRecordBatch leftMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer, leftOutcomes, leftContainer.get(0).getSchema());

    // Get the right container with dummy data
    rightContainer.add(emptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet2.container());

    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final CloseableRecordBatch rightMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      rightContainer, rightOutcomes, rightContainer.get(0).getSchema());

    final LateralJoinBatch ljBatch = new LateralJoinBatch(ljPopConfig, fixture.getFragmentContext(),
      leftMockBatch, rightMockBatch);

    try {
      int totalRecordCount = 0;
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == ljBatch.next());

      // 1st output batch is received for first EMIT from LEFT side
      assertTrue(RecordBatch.IterOutcome.EMIT == ljBatch.next());
      totalRecordCount += ljBatch.getRecordCount();

      // 2nd output batch is received for second EMIT from LEFT side
      assertTrue(RecordBatch.IterOutcome.EMIT == ljBatch.next());
      totalRecordCount += ljBatch.getRecordCount();

      // Compare the total records generated in 2 output batches with expected count.
      assertTrue(totalRecordCount ==
        (nonEmptyLeftRowSet.rowCount() * nonEmptyRightRowSet.rowCount() +
          leftRowSet2.rowCount() * nonEmptyRightRowSet2.rowCount()));
      assertTrue(RecordBatch.IterOutcome.NONE == ljBatch.next());
    } catch (AssertionError | Exception error) {
      fail();
    } finally {
      // Close all the resources for this test case
      ljBatch.close();
      leftMockBatch.close();
      rightMockBatch.close();
      leftRowSet2.clear();
      nonEmptyRightRowSet2.clear();
    }
  }

  /**
   * Test for the case where LATERAL received a left batch with OK outcome and then populate the Join output in the
   * outgoing batch. There is still some space left in output batch so LATERAL call's next() on left side and receive
   * NONE outcome from left side. Then in this case LATERAL should produce the previous output batch with OK outcome
   * and then handle NONE outcome in future next() call.
   *
   * @throws Exception
   */
  @Test
  public void testHandlingNoneAfterOK() throws Exception {

    // Get the left container with dummy data for Lateral Join
    leftContainer.add(nonEmptyLeftRowSet.container());

    // Get the left IterOutcomes for Lateral Join
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);

    // Create Left MockRecordBatch
    final CloseableRecordBatch leftMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer, leftOutcomes, leftContainer.get(0).getSchema());

    // Get the right container with dummy data
    rightContainer.add(emptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet.container());

    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final CloseableRecordBatch rightMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      rightContainer, rightOutcomes, rightContainer.get(0).getSchema());

    final LateralJoinBatch ljBatch = new LateralJoinBatch(ljPopConfig, fixture.getFragmentContext(),
      leftMockBatch, rightMockBatch);

    try {
      int totalRecordCount = 0;
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == ljBatch.next());

      // 1st output batch is received for first EMIT from LEFT side
      assertTrue(RecordBatch.IterOutcome.OK == ljBatch.next());
      totalRecordCount += ljBatch.getRecordCount();

      // Compare the total records generated in 2 output batches with expected count.
      assertTrue(totalRecordCount ==
        (nonEmptyLeftRowSet.rowCount() * nonEmptyRightRowSet.rowCount()));
      assertTrue(RecordBatch.IterOutcome.NONE == ljBatch.next());
    } catch (AssertionError | Exception error) {
      fail();
    } finally {
      // Close all the resources for this test case
      ljBatch.close();
      leftMockBatch.close();
      rightMockBatch.close();
    }
  }

  /**
   * Test for the case when LATERAL received a left batch with OK outcome and then populate the Join output in the
   * outgoing batch. There is still some space left in output batch so LATERAL call's next() on left side and receive
   * EMIT outcome from left side with empty batch. Then in this case LATERAL should produce the previous output batch
   * with EMIT outcome.
   *
   * @throws Exception
   */
  @Test
  public void testHandlingEmptyEMITAfterOK() throws Exception {

    // Get the left container with dummy data for Lateral Join
    leftContainer.add(nonEmptyLeftRowSet.container());
    leftContainer.add(emptyLeftRowSet.container());

    // Get the left IterOutcomes for Lateral Join
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    leftOutcomes.add(RecordBatch.IterOutcome.EMIT);

    // Create Left MockRecordBatch
    final CloseableRecordBatch leftMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer, leftOutcomes, leftContainer.get(0).getSchema());

    // Get the right container with dummy data
    rightContainer.add(emptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet.container());

    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final CloseableRecordBatch rightMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      rightContainer, rightOutcomes, rightContainer.get(0).getSchema());

    final LateralJoinBatch ljBatch = new LateralJoinBatch(ljPopConfig, fixture.getFragmentContext(),
      leftMockBatch, rightMockBatch);

    try {
      int totalRecordCount = 0;
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == ljBatch.next());

      // 1st output batch is received for first EMIT from LEFT side
      assertTrue(RecordBatch.IterOutcome.EMIT == ljBatch.next());
      totalRecordCount += ljBatch.getRecordCount();

      // Compare the total records generated in 2 output batches with expected count.
      assertTrue(totalRecordCount ==
        (nonEmptyLeftRowSet.rowCount() * nonEmptyRightRowSet.rowCount()));
      assertTrue(RecordBatch.IterOutcome.NONE == ljBatch.next());
    } catch (AssertionError | Exception error) {
      fail();
    } finally {
      // Close all the resources for this test case
      ljBatch.close();
      leftMockBatch.close();
      rightMockBatch.close();
    }
  }

  /**
   * Test for the case when LATERAL received a left batch with OK outcome and then populate the Join output in the
   * outgoing batch. There is still some space left in output batch so LATERAL call's next() on left side and receive
   * EMIT outcome from left side with non-empty batch. Then in this case LATERAL should produce the output batch with
   * EMIT outcome if second left and right batches are also consumed entirely in same output batch.
   *
   * @throws Exception
   */
  @Test
  public void testHandlingNonEmptyEMITAfterOK() throws Exception {

    // Create data for left input
    final RowSet.SingleRowSet leftRowSet2 = fixture.rowSetBuilder(leftSchema)
      .addRow(2, 20, "item20")
      .build();

    // Create data for right input
    final RowSet.SingleRowSet nonEmptyRightRowSet2 = fixture.rowSetBuilder(rightSchema)
      .addRow(1, 4, 41, "item41")
      .addRow(1, 5, 51, "item51")
      .build();

    // Get the left container with dummy data for Lateral Join
    leftContainer.add(nonEmptyLeftRowSet.container());
    leftContainer.add(leftRowSet2.container());

    // Get the left IterOutcomes for Lateral Join
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    leftOutcomes.add(RecordBatch.IterOutcome.EMIT);

    // Create Left MockRecordBatch
    final CloseableRecordBatch leftMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer, leftOutcomes, leftContainer.get(0).getSchema());

    // Get the right container with dummy data
    rightContainer.add(emptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet2.container());

    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final CloseableRecordBatch rightMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      rightContainer, rightOutcomes, rightContainer.get(0).getSchema());

    final LateralJoinBatch ljBatch = new LateralJoinBatch(ljPopConfig, fixture.getFragmentContext(),
      leftMockBatch, rightMockBatch);

    try {
      int totalRecordCount = 0;
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == ljBatch.next());

      // 1st output batch is received for first EMIT from LEFT side
      assertTrue(RecordBatch.IterOutcome.EMIT == ljBatch.next());
      totalRecordCount += ljBatch.getRecordCount();

      // Compare the total records generated in 2 output batches with expected count.
      assertTrue(totalRecordCount ==
        (nonEmptyLeftRowSet.rowCount() * nonEmptyRightRowSet.rowCount()) +
          (leftRowSet2.rowCount() * nonEmptyRightRowSet2.rowCount()));
      assertTrue(RecordBatch.IterOutcome.NONE == ljBatch.next());
    } catch (AssertionError | Exception error){
      fail();
    } finally {
      // Close all the resources for this test case
      ljBatch.close();
      leftMockBatch.close();
      rightMockBatch.close();
      leftRowSet2.clear();
      nonEmptyRightRowSet2.clear();
    }
  }

  /**
   * Temporary test to validate LATERAL handling output batch getting filled without consuming full output from left
   * and right batch join.
   * <p>
   * For this test we are updating {@link LateralJoinBatch#MAX_BATCH_ROW_COUNT} by making it public, which might not expected
   * after including the BatchSizing logic
   * TODO: Update the test after incorporating the BatchSizing change.
   *
   * @throws Exception
   */
  @Test
  public void testHandlingNonEmpty_EMITAfterOK_WithMultipleOutput() throws Exception {

    // Create data for left input
    final RowSet.SingleRowSet leftRowSet2 = fixture.rowSetBuilder(leftSchema)
      .addRow(2, 20, "item20")
      .build();

    // Create data for right input
    final RowSet.SingleRowSet nonEmptyRightRowSet2 = fixture.rowSetBuilder(rightSchema)
      .addRow(1, 4, 41, "item41")
      .addRow(1, 5, 51, "item51")
      .build();

    // Get the left container with dummy data for Lateral Join
    leftContainer.add(nonEmptyLeftRowSet.container());
    leftContainer.add(leftRowSet2.container());

    // Get the left IterOutcomes for Lateral Join
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    leftOutcomes.add(RecordBatch.IterOutcome.EMIT);

    // Create Left MockRecordBatch
    final CloseableRecordBatch leftMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer, leftOutcomes, leftContainer.get(0).getSchema());

    // Get the right container with dummy data
    rightContainer.add(emptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet2.container());

    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final CloseableRecordBatch rightMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      rightContainer, rightOutcomes, rightContainer.get(0).getSchema());

    final LateralJoinBatch ljBatch = new LateralJoinBatch(ljPopConfig, fixture.getFragmentContext(),
      leftMockBatch, rightMockBatch);

    final int maxBatchSize = 2;
    ljBatch.setUseMemoryManager(false);
    ljBatch.setMaxOutputRowCount(maxBatchSize);

    try {
      int totalRecordCount = 0;
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == ljBatch.next());

      // 1st output batch
      assertTrue(RecordBatch.IterOutcome.OK == ljBatch.next());
      totalRecordCount += ljBatch.getRecordCount();
      assertTrue(ljBatch.getRecordCount() == maxBatchSize);

      // 2nd output batch
      assertTrue(RecordBatch.IterOutcome.OK == ljBatch.next());
      assertTrue(ljBatch.getRecordCount() == maxBatchSize);
      totalRecordCount += ljBatch.getRecordCount();

      // 3rd output batch
      assertTrue(RecordBatch.IterOutcome.EMIT == ljBatch.next());
      totalRecordCount += ljBatch.getRecordCount();

      // Compare the total records generated in 2 output batches with expected count.
      assertTrue(totalRecordCount ==
        (nonEmptyLeftRowSet.rowCount() * nonEmptyRightRowSet.rowCount()) +
          (leftRowSet2.rowCount() * nonEmptyRightRowSet2.rowCount()));
      assertTrue(RecordBatch.IterOutcome.NONE == ljBatch.next());
    } catch (AssertionError | Exception error) {
      fail();
    } finally {
      // Close all the resources for this test case
      ljBatch.close();
      leftMockBatch.close();
      rightMockBatch.close();
      leftRowSet2.clear();
      nonEmptyRightRowSet2.clear();
    }
  }

  /**
   * Test to check basic left lateral join is working correctly or not. We create a left batch with one and
   * corresponding right batch with zero rows and check if output still get's populated with left side of data or not.
   * Expectation is since it's a left join and even though right batch is empty the left row will be pushed to output
   * batch.
   *
   * @throws Exception
   */
  @Test
  public void testBasicLeftLateralJoin() throws Exception {
    // Get the left container with dummy data for Lateral Join
    leftContainer.add(nonEmptyLeftRowSet.container());

    // Get the left IterOutcomes for Lateral Join
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);

    // Create Left MockRecordBatch
    final CloseableRecordBatch leftMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer, leftOutcomes, leftContainer.get(0).getSchema());

    // Get the right container with dummy data
    rightContainer.add(emptyRightRowSet.container());
    rightContainer.add(emptyRightRowSet.container());

    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final CloseableRecordBatch rightMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      rightContainer, rightOutcomes, rightContainer.get(0).getSchema());

    final LateralJoinPOP popConfig = new LateralJoinPOP(null, null, JoinRelType.LEFT, DrillLateralJoinRelBase.IMPLICIT_COLUMN, Lists.newArrayList());

    final LateralJoinBatch ljBatch = new LateralJoinBatch(popConfig, fixture.getFragmentContext(),
      leftMockBatch, rightMockBatch);

    try {
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == ljBatch.next());
      assertTrue(RecordBatch.IterOutcome.OK == ljBatch.next());
      assertTrue(ljBatch.getRecordCount() == nonEmptyLeftRowSet.container().getRecordCount());
      assertTrue(RecordBatch.IterOutcome.NONE == ljBatch.next());
    } catch (AssertionError | Exception error) {
      fail();
    } finally {
      // Close all the resources for this test case
      ljBatch.close();
      leftMockBatch.close();
      rightMockBatch.close();
    }
  }

  /**
   * Test to see if there are multiple rows in left batch and for some rows right side produces multiple batches such
   * that some are with records and some are empty then we are not duplicating those left side records based on left
   * join type. In this case all the output will be produces only in 1 record batch.
   *
   * @throws Exception
   */
  @Test
  public void testLeftLateralJoin_WithMatchingAndEmptyBatch() throws Exception {
    // Get the left container with dummy data for Lateral Join
    final RowSet.SingleRowSet leftRowSet2 = fixture.rowSetBuilder(leftSchema)
      .addRow(1, 10, "item10")
      .addRow(2, 20, "item20")
      .build();

    final RowSet.SingleRowSet nonEmptyRightRowSet2 = fixture.rowSetBuilder(rightSchema)
      .addRow(2, 6, 60, "item61")
      .addRow(2, 7, 70, "item71")
      .addRow(2, 8, 80, "item81")
      .build();

    leftContainer.add(leftRowSet2.container());

    // Get the left IterOutcomes for Lateral Join
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);

    // Create Left MockRecordBatch
    final CloseableRecordBatch leftMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer, leftOutcomes, leftContainer.get(0).getSchema());

    // Get the right container with dummy data
    rightContainer.add(emptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet2.container());
    rightContainer.add(emptyRightRowSet.container());
    rightContainer.add(emptyRightRowSet.container());

    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    rightOutcomes.add(RecordBatch.IterOutcome.OK);
    rightOutcomes.add(RecordBatch.IterOutcome.OK);
    rightOutcomes.add(RecordBatch.IterOutcome.OK);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final CloseableRecordBatch rightMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      rightContainer, rightOutcomes, rightContainer.get(0).getSchema());

    final LateralJoinPOP popConfig = new LateralJoinPOP(null, null, JoinRelType.LEFT, DrillLateralJoinRelBase.IMPLICIT_COLUMN, Lists.newArrayList());

    final LateralJoinBatch ljBatch = new LateralJoinBatch(popConfig, fixture.getFragmentContext(),
      leftMockBatch, rightMockBatch);

    try {
      final int expectedOutputRecordCount = 6; // 3 for first left row and 1 for second left row
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == ljBatch.next());
      assertTrue(RecordBatch.IterOutcome.OK == ljBatch.next());
      assertTrue(ljBatch.getRecordCount() == expectedOutputRecordCount);
      assertTrue(RecordBatch.IterOutcome.NONE == ljBatch.next());
    } catch (AssertionError | Exception error) {
      fail();
    } finally {
      // Close all the resources for this test case
      ljBatch.close();
      leftMockBatch.close();
      rightMockBatch.close();
      leftRowSet2.clear();
      nonEmptyRightRowSet2.clear();
    }
  }

  /**
   * Test to see if there are multiple rows in left batch and for some rows right side produces batch with records
   * and for other rows right side produces empty batches then based on left join type we are populating the output
   * batch correctly. Expectation is that for left rows if we find corresponding right rows then we will output both
   * using cross-join but for left rows for which there is empty right side we will produce only left row in output
   * batch. In this case all the output will be produces only in 1 record batch.
   *
   * @throws Exception
   */
  @Test
  public void testLeftLateralJoin_WithAndWithoutMatching() throws Exception {
    // Get the left container with dummy data for Lateral Join
    final RowSet.SingleRowSet leftRowSet2 = fixture.rowSetBuilder(leftSchema)
      .addRow(1, 10, "item10")
      .addRow(2, 20, "item20")
      .addRow(3, 30, "item30")
      .build();

    final RowSet.SingleRowSet nonEmptyRightRowSet2 = fixture.rowSetBuilder(rightSchema)
      .addRow(3, 6, 60, "item61")
      .addRow(3, 7, 70, "item71")
      .addRow(3, 8, 80, "item81")
      .build();

    leftContainer.add(leftRowSet2.container());

    // Get the left IterOutcomes for Lateral Join
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);

    // Create Left MockRecordBatch
    final CloseableRecordBatch leftMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer, leftOutcomes, leftContainer.get(0).getSchema());

    // Get the right container with dummy data
    rightContainer.add(emptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet.container());
    rightContainer.add(emptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet2.container());

    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    rightOutcomes.add(RecordBatch.IterOutcome.OK);
    rightOutcomes.add(RecordBatch.IterOutcome.OK);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final CloseableRecordBatch rightMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      rightContainer, rightOutcomes, rightContainer.get(0).getSchema());

    final LateralJoinPOP popConfig = new LateralJoinPOP(null, null, JoinRelType.LEFT, DrillLateralJoinRelBase.IMPLICIT_COLUMN, Lists.newArrayList());

    final LateralJoinBatch ljBatch = new LateralJoinBatch(popConfig, fixture.getFragmentContext(),
      leftMockBatch, rightMockBatch);

    try {
      final int expectedOutputRecordCount = 7; // 3 for first left row and 1 for second left row
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == ljBatch.next());
      assertTrue(RecordBatch.IterOutcome.OK == ljBatch.next());
      assertTrue(ljBatch.getRecordCount() == expectedOutputRecordCount);
      assertTrue(RecordBatch.IterOutcome.NONE == ljBatch.next());
    } catch (AssertionError | Exception error) {
      //fail();
      throw error;
    } finally {
      // Close all the resources for this test case
      ljBatch.close();
      leftMockBatch.close();
      rightMockBatch.close();
      leftRowSet2.clear();
      nonEmptyRightRowSet2.clear();
    }
  }

  /**
   * Test to see if there are multiple rows in left batch and for some rows right side produces batch with records
   * and for other rows right side produces empty batches then based on left join type we are populating the output
   * batch correctly. Expectation is that for left rows if we find corresponding right rows then we will output both
   * using cross-join but for left rows for which there is empty right side we will produce only left row in output
   * batch. But in this test we have made the Batch size very small so that output will be returned in multiple
   * output batches. This test verifies even in this case indexes are manipulated correctly and outputs are produced
   * correctly.
   * TODO: Update the test case based on Batch Sizing project since then the variable might not be available.
   *
   * @throws Exception
   */
  @Test
  public void testLeftLateralJoin_WithAndWithoutMatching_MultipleBatch() throws Exception {
    // Get the left container with dummy data for Lateral Join
    final RowSet.SingleRowSet leftRowSet2 = fixture.rowSetBuilder(leftSchema)
      .addRow(1, 10, "item10")
      .addRow(2, 20, "item20")
      .addRow(3, 30, "item30")
      .build();

    final RowSet.SingleRowSet nonEmptyRightRowSet2 = fixture.rowSetBuilder(rightSchema)
      .addRow(3, 6, 60, "item61")
      .addRow(3, 7, 70, "item71")
      .addRow(3, 8, 80, "item81")
      .build();

    leftContainer.add(leftRowSet2.container());

    // Get the left IterOutcomes for Lateral Join
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);

    // Create Left MockRecordBatch
    final CloseableRecordBatch leftMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer, leftOutcomes, leftContainer.get(0).getSchema());

    // Get the right container with dummy data
    rightContainer.add(emptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet.container());
    rightContainer.add(emptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet2.container());

    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    rightOutcomes.add(RecordBatch.IterOutcome.OK);
    rightOutcomes.add(RecordBatch.IterOutcome.OK);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final CloseableRecordBatch rightMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      rightContainer, rightOutcomes, rightContainer.get(0).getSchema());

    final LateralJoinPOP popConfig = new LateralJoinPOP(null, null, JoinRelType.LEFT, DrillLateralJoinRelBase.IMPLICIT_COLUMN, Lists.newArrayList());

    final LateralJoinBatch ljBatch = new LateralJoinBatch(popConfig, fixture.getFragmentContext(),
      leftMockBatch, rightMockBatch);

    int originalMaxBatchSize = 2;
    ljBatch.setUseMemoryManager(false);
    ljBatch.setMaxOutputRowCount(originalMaxBatchSize);

    try {
      final int expectedOutputRecordCount = 7; // 3 for first left row and 1 for second left row
      int actualOutputRecordCount = 0;
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == ljBatch.next());
      assertTrue(RecordBatch.IterOutcome.OK == ljBatch.next());
      actualOutputRecordCount += ljBatch.getRecordCount();
      assertTrue(RecordBatch.IterOutcome.OK == ljBatch.next());
      actualOutputRecordCount += ljBatch.getRecordCount();
      assertTrue(RecordBatch.IterOutcome.OK == ljBatch.next());
      actualOutputRecordCount += ljBatch.getRecordCount();
      assertTrue(RecordBatch.IterOutcome.OK == ljBatch.next());
      actualOutputRecordCount += ljBatch.getRecordCount();
      assertTrue(actualOutputRecordCount == expectedOutputRecordCount);
      assertTrue(RecordBatch.IterOutcome.NONE == ljBatch.next());
    } catch (AssertionError | Exception error) {
      //fail();
      throw error;
    } finally {
      // Close all the resources for this test case
      ljBatch.close();
      leftMockBatch.close();
      rightMockBatch.close();
      //leftRowSet2.clear();
      //nonEmptyRightRowSet2.clear();
    }
  }

  /**
   * This test generates an operator tree for multiple UNNEST at same level by stacking 2 LATERAL and UNNEST pair on
   * top of each other. Then we call next() on top level LATERAL to simulate the operator tree and compare the
   * outcome and record count generated with expected values.
   * @throws Exception
   */
  @Test
  public void testMultipleUnnestAtSameLevel() throws Exception {

    // ** Prepare first pair of left batch and right batch for Lateral_1 **
    leftContainer.add(nonEmptyLeftRowSet.container());

    // Get the left IterOutcomes for Lateral Join
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);

    final CloseableRecordBatch leftMockBatch_1 = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer, leftOutcomes, leftContainer.get(0).getSchema());

    // Get the right container with dummy data
    rightContainer.add(emptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet.container());

    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final CloseableRecordBatch rightMockBatch_1 = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      rightContainer, rightOutcomes, rightContainer.get(0).getSchema());

    final LateralJoinPOP popConfig_1 = new LateralJoinPOP(null, null, JoinRelType.INNER, DrillLateralJoinRelBase.IMPLICIT_COLUMN, Lists.newArrayList());

    final LateralJoinBatch ljBatch_1 = new LateralJoinBatch(popConfig_1, fixture.getFragmentContext(),
      leftMockBatch_1, rightMockBatch_1);

    // ** Prepare second pair of left and right batch for Lateral_2 **

    // Get the right container with dummy data for Lateral Join_2

    // Create right input schema
    TupleMetadata rightSchema2 = new SchemaBuilder()
      .add(popConfig_1.getImplicitRIDColumn(), TypeProtos.MinorType.INT)
      .add("id_right_1", TypeProtos.MinorType.INT)
      .add("cost_right_1", TypeProtos.MinorType.INT)
      .add("name_right_1", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    final RowSet.SingleRowSet emptyRightRowSet2 = fixture.rowSetBuilder(rightSchema2).build();

    final RowSet.SingleRowSet nonEmptyRightRowSet2 = fixture.rowSetBuilder(rightSchema2)
      .addRow(1, 6, 60, "item61")
      .addRow(1, 7, 70, "item71")
      .addRow(1, 8, 80, "item81")
      .build();

    final List<VectorContainer> rightContainer2 = new ArrayList<>(5);
    // Get the right container with dummy data
    rightContainer2.add(emptyRightRowSet2.container());
    rightContainer2.add(nonEmptyRightRowSet2.container());
    rightContainer2.add(emptyRightRowSet2.container());
    rightContainer2.add(emptyRightRowSet2.container());

    final List<RecordBatch.IterOutcome> rightOutcomes2 = new ArrayList<>(5);
    rightOutcomes2.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    rightOutcomes2.add(RecordBatch.IterOutcome.OK);
    rightOutcomes2.add(RecordBatch.IterOutcome.OK);
    rightOutcomes2.add(RecordBatch.IterOutcome.EMIT);

    final CloseableRecordBatch rightMockBatch_2 = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      rightContainer2, rightOutcomes2, rightContainer2.get(0).getSchema());

    final LateralJoinBatch ljBatch_2 = new LateralJoinBatch(popConfig_1, fixture.getFragmentContext(),
      ljBatch_1, rightMockBatch_2);

    try {
      final int expectedOutputRecordCount = 3; // 3 from the lower level lateral and then finally 3 for 1st row in
      // second lateral and 0 for other 2 rows.

      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == ljBatch_2.next());
      assertTrue(RecordBatch.IterOutcome.OK == ljBatch_2.next());
      int actualOutputRecordCount = ljBatch_2.getRecordCount();
      assertTrue(RecordBatch.IterOutcome.NONE == ljBatch_2.next());
      assertTrue(actualOutputRecordCount == expectedOutputRecordCount);
    } catch (AssertionError | Exception error) {
      fail();
    } finally {
      // Close all the resources for this test case
      ljBatch_2.close();
      rightMockBatch_2.close();
      ljBatch_1.close();
      leftMockBatch_1.close();
      rightMockBatch_1.close();
      rightContainer2.clear();
      rightOutcomes2.clear();
    }
  }

  /**
   * This test generates an operator tree for multi level LATERAL by stacking 2 LATERAL and finally an UNNEST pair
   * (using MockRecord Batch) as left and right child of lower level LATERAL. Then we call next() on top level
   * LATERAL to simulate the operator tree and compare the outcome and record count generated with expected values.
   * @throws Exception
   */
  @Test
  public void testMultiLevelLateral() throws Exception {

    // ** Prepare first pair of left batch and right batch for Lateral_1 **
    final LateralJoinPOP popConfig_1 = new LateralJoinPOP(null, null, JoinRelType.INNER,
      DrillLateralJoinRelBase.IMPLICIT_COLUMN, Lists.newArrayList());

    // Create a left batch with implicit column for lower lateral left unnest
    TupleMetadata leftSchemaWithImplicit = new SchemaBuilder()
      .add(popConfig_1.getImplicitRIDColumn(), TypeProtos.MinorType.INT)
      .add("id_left", TypeProtos.MinorType.INT)
      .add("cost_left", TypeProtos.MinorType.INT)
      .add("name_left", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    final RowSet.SingleRowSet emptyLeftRowSet_1 = fixture.rowSetBuilder(leftSchemaWithImplicit)
      .build();
    final RowSet.SingleRowSet nonEmptyLeftRowSet_1 = fixture.rowSetBuilder(leftSchemaWithImplicit)
      .addRow(1, 1, 10, "item1")
      .build();

    final RowSet.SingleRowSet nonEmptyLeftRowSet_2 = fixture.rowSetBuilder(leftSchemaWithImplicit)
      .addRow(1, 2, 20, "item2")
      .build();

    leftContainer.add(emptyLeftRowSet_1.container());
    leftContainer.add(nonEmptyLeftRowSet_1.container());
    leftContainer.add(nonEmptyLeftRowSet_2.container());

    // Get the left IterOutcomes for Lateral Join
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    leftOutcomes.add(RecordBatch.IterOutcome.OK);
    leftOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final CloseableRecordBatch leftMockBatch_1 = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer, leftOutcomes, leftContainer.get(0).getSchema());

    // Get the right container with dummy data
    final RowSet.SingleRowSet nonEmptyRightRowSet_1 = fixture.rowSetBuilder(rightSchema)
      .addRow(1, 5, 51, "item51")
      .addRow(1, 6, 61, "item61")
      .addRow(1, 7, 71, "item71")
      .build();
    rightContainer.add(emptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet_1.container());

    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final CloseableRecordBatch rightMockBatch_1 = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      rightContainer, rightOutcomes, rightContainer.get(0).getSchema());

    final LateralJoinBatch lowerLateral = new LateralJoinBatch(popConfig_1, fixture.getFragmentContext(),
      leftMockBatch_1, rightMockBatch_1);

    // ** Prepare second pair of left and right batch for Lateral_2 **

    // Create left input schema
    TupleMetadata leftSchema2 = new SchemaBuilder()
      .add("id_left_1", TypeProtos.MinorType.INT)
      .add("cost_left_1", TypeProtos.MinorType.INT)
      .add("name_left_1", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    final RowSet.SingleRowSet emptyLeftRowSet2 = fixture.rowSetBuilder(leftSchema2).build();
    final RowSet.SingleRowSet nonEmptyLeftRowSet2 = fixture.rowSetBuilder(leftSchema2)
      .addRow(6, 60, "item6")
      .build();

    // Get the left container with dummy data
    final List<VectorContainer> leftContainer2 = new ArrayList<>(5);
    leftContainer2.add(emptyLeftRowSet2.container());
    leftContainer2.add(nonEmptyLeftRowSet2.container());

    // Get the left outcomes with dummy data
    final List<RecordBatch.IterOutcome> leftOutcomes2 = new ArrayList<>(5);
    leftOutcomes2.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    leftOutcomes2.add(RecordBatch.IterOutcome.OK);

    final CloseableRecordBatch leftMockBatch_2 = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer2, leftOutcomes2, leftContainer2.get(0).getSchema());

    final LateralJoinBatch upperLateral = new LateralJoinBatch(popConfig_1, fixture.getFragmentContext(),
      leftMockBatch_2, lowerLateral);

    try {
      final int expectedOutputRecordCount = 6;

      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == upperLateral.next());
      assertTrue(RecordBatch.IterOutcome.OK == upperLateral.next());
      int actualOutputRecordCount = upperLateral.getRecordCount();
      assertTrue(RecordBatch.IterOutcome.NONE == upperLateral.next());
      assertTrue(actualOutputRecordCount == expectedOutputRecordCount);
    } catch (AssertionError | Exception error) {
      fail();
    } finally {
      // Close all the resources for this test case
      upperLateral.close();
      leftMockBatch_2.close();
      lowerLateral.close();
      leftMockBatch_1.close();
      rightMockBatch_1.close();
      leftContainer2.clear();
      leftOutcomes2.clear();
    }
  }

  /**
   * This test generates an operator tree for multi level LATERAL by stacking 2 LATERAL and finally an UNNEST pair
   * (using MockRecord Batch) as left and right child of lower level LATERAL. Then we call next() on top level
   * LATERAL to simulate the operator tree and compare the outcome and record count generated with expected values.
   * This test also changes the MAX_BATCH_ROW_COUNT to simulate the output being produced in multiple batches.
   * @throws Exception
   */
  @Test
  public void testMultiLevelLateral_MultipleOutput() throws Exception {

    // ** Prepare first pair of left batch and right batch for lower level LATERAL Lateral_1 **
    final LateralJoinPOP popConfig_1 = new LateralJoinPOP(null, null, JoinRelType.INNER, DrillLateralJoinRelBase.IMPLICIT_COLUMN, Lists.newArrayList());

    TupleMetadata leftSchemaWithImplicit = new SchemaBuilder()
      .add(popConfig_1.getImplicitRIDColumn(), TypeProtos.MinorType.INT)
      .add("id_left", TypeProtos.MinorType.INT)
      .add("cost_left", TypeProtos.MinorType.INT)
      .add("name_left", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    final RowSet.SingleRowSet emptyLeftRowSet_1 = fixture.rowSetBuilder(leftSchemaWithImplicit)
      .build();
    final RowSet.SingleRowSet nonEmptyLeftRowSet_1 = fixture.rowSetBuilder(leftSchemaWithImplicit)
      .addRow(1, 1, 10, "item1")
      .build();

    final RowSet.SingleRowSet nonEmptyLeftRowSet_2 = fixture.rowSetBuilder(leftSchemaWithImplicit)
      .addRow(1, 2, 20, "item2")
      .build();

    leftContainer.add(emptyLeftRowSet_1.container());
    leftContainer.add(nonEmptyLeftRowSet_1.container());
    leftContainer.add(nonEmptyLeftRowSet_2.container());

    // Get the left IterOutcomes for Lateral Join
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    leftOutcomes.add(RecordBatch.IterOutcome.OK);
    leftOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final CloseableRecordBatch leftMockBatch_1 = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer, leftOutcomes, leftContainer.get(0).getSchema());

    // Get the right container with dummy data
    final RowSet.SingleRowSet nonEmptyRightRowSet_1 = fixture.rowSetBuilder(rightSchema)
      .addRow(1, 5, 51, "item51")
      .addRow(1, 6, 61, "item61")
      .addRow(1, 7, 71, "item71")
      .build();
    rightContainer.add(emptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet_1.container());

    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final CloseableRecordBatch rightMockBatch_1 = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      rightContainer, rightOutcomes, rightContainer.get(0).getSchema());

    final LateralJoinBatch lowerLateral = new LateralJoinBatch(popConfig_1, fixture.getFragmentContext(),
      leftMockBatch_1, rightMockBatch_1);

    // Use below api to enforce static output batch limit
    lowerLateral.setUseMemoryManager(false);
    lowerLateral.setMaxOutputRowCount(2);

    // ** Prepare second pair of left and right batch for upper LATERAL Lateral_2 **

    // Create left input schema
    TupleMetadata leftSchema2 = new SchemaBuilder()
      .add("id_left_1", TypeProtos.MinorType.INT)
      .add("cost_left_1", TypeProtos.MinorType.INT)
      .add("name_left_1", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    final RowSet.SingleRowSet emptyLeftRowSet2 = fixture.rowSetBuilder(leftSchema2).build();
    final RowSet.SingleRowSet nonEmptyLeftRowSet2 = fixture.rowSetBuilder(leftSchema2)
      .addRow(6, 60, "item6")
      .build();

    // Get the left container with dummy data
    final List<VectorContainer> leftContainer2 = new ArrayList<>(5);
    leftContainer2.add(emptyLeftRowSet2.container());
    leftContainer2.add(nonEmptyLeftRowSet2.container());

    // Get the left incoming batch outcomes
    final List<RecordBatch.IterOutcome> leftOutcomes2 = new ArrayList<>(5);
    leftOutcomes2.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    leftOutcomes2.add(RecordBatch.IterOutcome.OK);

    final CloseableRecordBatch leftMockBatch_2 = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer2, leftOutcomes2, leftContainer2.get(0).getSchema());

    final LateralJoinBatch upperLateral = new LateralJoinBatch(popConfig_1, fixture.getFragmentContext(),
      leftMockBatch_2, lowerLateral);

    // Use below api to enforce static output batch limit
    upperLateral.setUseMemoryManager(false);
    upperLateral.setMaxOutputRowCount(2);

    try {
      final int expectedOutputRecordCount = 6;

      int actualOutputRecordCount = 0;
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == upperLateral.next());
      assertTrue(RecordBatch.IterOutcome.OK == upperLateral.next());
      actualOutputRecordCount += upperLateral.getRecordCount();
      assertTrue(RecordBatch.IterOutcome.OK == upperLateral.next());
      actualOutputRecordCount += upperLateral.getRecordCount();
      assertTrue(RecordBatch.IterOutcome.OK == upperLateral.next());
      actualOutputRecordCount += upperLateral.getRecordCount();
      assertTrue(RecordBatch.IterOutcome.NONE == upperLateral.next());
      assertTrue(actualOutputRecordCount == expectedOutputRecordCount);
    } catch (AssertionError | Exception error) {
      fail();
    } finally {
      // Close all the resources for this test case
      upperLateral.close();
      leftMockBatch_2.close();
      lowerLateral.close();
      leftMockBatch_1.close();
      rightMockBatch_1.close();
      leftContainer2.clear();
      leftOutcomes2.clear();
    }
  }

  /**
   * This test generates an operator tree for multi level LATERAL by stacking 2 LATERAL and finally an UNNEST pair
   * (using MockRecord Batch) as left and right child of lower level LATERAL. In this setup the test try to simulate
   * the SchemaChange happening at upper level LATERAL left incoming second batch, which also results into the
   * SchemaChange of left UNNEST of lower level LATERAL. This test validates that the schema change is handled
   * correctly by both upper and lower level LATERAL.
   *
   * @throws Exception
   */
  @Test
  public void testMultiLevelLateral_SchemaChange_LeftUnnest() throws Exception {

    // ** Prepare first pair of left batch and right batch for lower level LATERAL Lateral_1 **
    final LateralJoinPOP popConfig_1 = new LateralJoinPOP(null, null, JoinRelType.INNER, DrillLateralJoinRelBase.IMPLICIT_COLUMN, Lists.newArrayList());

    TupleMetadata leftSchemaWithImplicit = new SchemaBuilder()
      .add(popConfig_1.getImplicitRIDColumn(), TypeProtos.MinorType.INT)
      .add("id_left", TypeProtos.MinorType.INT)
      .add("cost_left", TypeProtos.MinorType.INT)
      .add("name_left", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    final RowSet.SingleRowSet emptyLeftRowSet_1 = fixture.rowSetBuilder(leftSchemaWithImplicit)
      .build();
    final RowSet.SingleRowSet nonEmptyLeftRowSet_1 = fixture.rowSetBuilder(leftSchemaWithImplicit)
      .addRow(1, 1, 10, "item1")
      .build();

    leftContainer.add(emptyLeftRowSet_1.container());
    leftContainer.add(nonEmptyLeftRowSet_1.container());

    // Create left input schema2 for schema change batch
    TupleMetadata leftSchema2 = new SchemaBuilder()
      .add(popConfig_1.getImplicitRIDColumn(), TypeProtos.MinorType.INT)
      .add("new_id_left", TypeProtos.MinorType.INT)
      .add("new_cost_left", TypeProtos.MinorType.INT)
      .add("new_name_left", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    final RowSet.SingleRowSet emptyLeftRowSet_Schema2 = fixture.rowSetBuilder(leftSchema2).build();
    final RowSet.SingleRowSet nonEmptyLeftRowSet_Schema2 = fixture.rowSetBuilder(leftSchema2)
      .addRow(1, 1111, 10001, "NewRecord")
      .build();

    leftContainer.add(emptyLeftRowSet_Schema2.container());
    leftContainer.add(nonEmptyLeftRowSet_Schema2.container());

    // Get the left IterOutcomes for Lateral Join
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    leftOutcomes.add(RecordBatch.IterOutcome.EMIT);
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    leftOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final CloseableRecordBatch leftMockBatch_1 = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer, leftOutcomes, leftContainer.get(0).getSchema());

    // Get the right container with dummy data
    final RowSet.SingleRowSet nonEmptyRightRowSet_1 = fixture.rowSetBuilder(rightSchema)
      .addRow(1, 5, 51, "item51")
      .addRow(1, 6, 61, "item61")
      .addRow(1, 7, 71, "item71")
      .build();
    rightContainer.add(emptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet_1.container());

    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final CloseableRecordBatch rightMockBatch_1 = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      rightContainer, rightOutcomes, rightContainer.get(0).getSchema());

    final LateralJoinBatch lowerLevelLateral = new LateralJoinBatch(popConfig_1, fixture.getFragmentContext(),
      leftMockBatch_1, rightMockBatch_1);

    // ** Prepare second pair of left and right batch for upper level Lateral_2 **

    // Create left input schema for first batch
    TupleMetadata leftSchema3 = new SchemaBuilder()
      .add("id_left_new", TypeProtos.MinorType.INT)
      .add("cost_left_new", TypeProtos.MinorType.INT)
      .add("name_left_new", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    final RowSet.SingleRowSet emptyLeftRowSet_leftSchema3 = fixture.rowSetBuilder(leftSchema3).build();
    final RowSet.SingleRowSet nonEmptyLeftRowSet_leftSchema3 = fixture.rowSetBuilder(leftSchema3)
      .addRow(6, 60, "item6")
      .build();

    // Get left input schema for second left batch
    TupleMetadata leftSchema4 = new SchemaBuilder()
      .add("id_left_new_new", TypeProtos.MinorType.INT)
      .add("cost_left_new_new", TypeProtos.MinorType.VARCHAR)
      .add("name_left_new_new", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    final RowSet.SingleRowSet nonEmptyLeftRowSet_leftSchema4 = fixture.rowSetBuilder(leftSchema4)
      .addRow(100, "100", "item100")
      .build();

    // Build Left container for upper level LATERAL operator
    final List<VectorContainer> leftContainer2 = new ArrayList<>(5);

    // Get the left container with dummy data
    leftContainer2.add(emptyLeftRowSet_leftSchema3.container());
    leftContainer2.add(nonEmptyLeftRowSet_leftSchema3.container());
    leftContainer2.add(nonEmptyLeftRowSet_leftSchema4.container());

    // Get the left container outcomes for upper level LATERAL operator
    final List<RecordBatch.IterOutcome> leftOutcomes2 = new ArrayList<>(5);
    leftOutcomes2.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    leftOutcomes2.add(RecordBatch.IterOutcome.OK);
    leftOutcomes2.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);

    final CloseableRecordBatch leftMockBatch_2 = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer2, leftOutcomes2, leftContainer2.get(0).getSchema());

    final LateralJoinBatch upperLevelLateral = new LateralJoinBatch(popConfig_1, fixture.getFragmentContext(),
      leftMockBatch_2, lowerLevelLateral);

    try {
      // 3 for first batch on left side and another 3 for next left batch
      final int expectedOutputRecordCount = 6;
      int actualOutputRecordCount = 0;

      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == upperLevelLateral.next());
      assertTrue(RecordBatch.IterOutcome.OK == upperLevelLateral.next());
      actualOutputRecordCount += upperLevelLateral.getRecordCount();
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == upperLevelLateral.next());
      actualOutputRecordCount += upperLevelLateral.getRecordCount();
      assertTrue(RecordBatch.IterOutcome.OK == upperLevelLateral.next());
      actualOutputRecordCount += upperLevelLateral.getRecordCount();
      assertTrue(RecordBatch.IterOutcome.NONE == upperLevelLateral.next());
      assertTrue(actualOutputRecordCount == expectedOutputRecordCount);
    } catch (AssertionError | Exception error) {
      fail();
    } finally {
      // Close all the resources for this test case
      upperLevelLateral.close();
      leftMockBatch_2.close();
      lowerLevelLateral.close();
      leftMockBatch_1.close();
      rightMockBatch_1.close();
      leftContainer2.clear();
      leftOutcomes2.clear();
    }
  }

  /**
   * This test generates an operator tree for multi level LATERAL by stacking 2 LATERAL and finally an UNNEST pair
   * (using MockRecord Batch) as left and right child of lower level LATERAL. In this setup the test try to simulate
   * the SchemaChange happening at upper level LATERAL left incoming second batch, which also results into the
   * SchemaChange of right UNNEST of lower level LATERAL. This test validates that the schema change is handled
   * correctly by both upper and lower level LATERAL.
   *
   * @throws Exception
   */
  @Test
  public void testMultiLevelLateral_SchemaChange_RightUnnest() throws Exception {
    // ** Prepare first pair of left batch and right batch for lower level LATERAL Lateral_1 **
    final LateralJoinPOP popConfig_1 = new LateralJoinPOP(null, null, JoinRelType.INNER, DrillLateralJoinRelBase.IMPLICIT_COLUMN, Lists.newArrayList());

    TupleMetadata leftSchemaWithImplicit = new SchemaBuilder()
      .add(popConfig_1.getImplicitRIDColumn(), TypeProtos.MinorType.INT)
      .add("id_left", TypeProtos.MinorType.INT)
      .add("cost_left", TypeProtos.MinorType.INT)
      .add("name_left", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    final RowSet.SingleRowSet emptyLeftRowSet_1 = fixture.rowSetBuilder(leftSchemaWithImplicit)
      .build();
    final RowSet.SingleRowSet nonEmptyLeftRowSet_1 = fixture.rowSetBuilder(leftSchemaWithImplicit)
      .addRow(1, 1, 10, "item1")
      .build();
    final RowSet.SingleRowSet nonEmptyLeftRowSet2 = fixture.rowSetBuilder(leftSchemaWithImplicit)
      .addRow(1, 1111, 10001, "NewRecord")
      .build();

    leftContainer.add(emptyLeftRowSet_1.container());
    leftContainer.add(nonEmptyLeftRowSet_1.container());
    leftContainer.add(nonEmptyLeftRowSet2.container());

    // Get the left IterOutcomes for Lateral Join
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    leftOutcomes.add(RecordBatch.IterOutcome.EMIT);
    leftOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final CloseableRecordBatch leftMockBatch_1 = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer, leftOutcomes, leftContainer.get(0).getSchema());

    // Get the right container with dummy data
    TupleMetadata rightSchema2 = new SchemaBuilder()
      .add(popConfig_1.getImplicitRIDColumn(), TypeProtos.MinorType.INT)
      .add("id_right_new", TypeProtos.MinorType.INT)
      .add("cost_right_new", TypeProtos.MinorType.VARCHAR)
      .add("name_right_new", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    final RowSet.SingleRowSet emptyRightRowSet_rightSchema2 = fixture.rowSetBuilder(rightSchema2).build();
    final RowSet.SingleRowSet nonEmptyRightRowSet_rightSchema2 = fixture.rowSetBuilder(rightSchema2)
      .addRow(1, 5, "51", "item51")
      .addRow(1, 6, "61", "item61")
      .addRow(1, 7, "71", "item71")
      .build();

    rightContainer.add(emptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet.container());
    rightContainer.add(emptyRightRowSet_rightSchema2.container());
    rightContainer.add(nonEmptyRightRowSet_rightSchema2.container());

    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);
    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final CloseableRecordBatch rightMockBatch_1 = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      rightContainer, rightOutcomes, rightContainer.get(0).getSchema());

    final LateralJoinBatch lowerLevelLateral = new LateralJoinBatch(popConfig_1, fixture.getFragmentContext(),
      leftMockBatch_1, rightMockBatch_1);

    // ** Prepare second pair of left and right batch for upper level Lateral_2 **

    // Create left input schema for first batch
    TupleMetadata leftSchema3 = new SchemaBuilder()
      .add("id_left_new", TypeProtos.MinorType.INT)
      .add("cost_left_new", TypeProtos.MinorType.INT)
      .add("name_left_new", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    final RowSet.SingleRowSet emptyLeftRowSet_leftSchema3 = fixture.rowSetBuilder(leftSchema3).build();
    final RowSet.SingleRowSet nonEmptyLeftRowSet_leftSchema3 = fixture.rowSetBuilder(leftSchema3)
      .addRow(6, 60, "item6")
      .build();

    // Get left input schema for second left batch
    TupleMetadata leftSchema4 = new SchemaBuilder()
      .add("id_left_new_new", TypeProtos.MinorType.INT)
      .add("cost_left_new_new", TypeProtos.MinorType.VARCHAR)
      .add("name_left_new_new", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    final RowSet.SingleRowSet nonEmptyLeftRowSet_leftSchema4 = fixture.rowSetBuilder(leftSchema4)
      .addRow(100, "100", "item100")
      .build();

    // Build Left container for upper level LATERAL operator
    final List<VectorContainer> leftContainer2 = new ArrayList<>(5);

    // Get the left container with dummy data
    leftContainer2.add(emptyLeftRowSet_leftSchema3.container());
    leftContainer2.add(nonEmptyLeftRowSet_leftSchema3.container());
    leftContainer2.add(nonEmptyLeftRowSet_leftSchema4.container());

    // Get the left container outcomes for upper level LATERAL operator
    final List<RecordBatch.IterOutcome> leftOutcomes2 = new ArrayList<>(5);
    leftOutcomes2.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    leftOutcomes2.add(RecordBatch.IterOutcome.OK);
    leftOutcomes2.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);

    final CloseableRecordBatch leftMockBatch_2 = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer2, leftOutcomes2, leftContainer2.get(0).getSchema());

    final LateralJoinBatch upperLevelLateral = new LateralJoinBatch(popConfig_1, fixture.getFragmentContext(),
      leftMockBatch_2, lowerLevelLateral);

    try {
      // 3 for first batch on left side and another 3 for next left batch
      final int expectedOutputRecordCount = 6;
      int actualOutputRecordCount = 0;

      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == upperLevelLateral.next());
      assertTrue(RecordBatch.IterOutcome.OK == upperLevelLateral.next());
      actualOutputRecordCount += upperLevelLateral.getRecordCount();
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == upperLevelLateral.next());
      actualOutputRecordCount += upperLevelLateral.getRecordCount();
      assertTrue(RecordBatch.IterOutcome.OK == upperLevelLateral.next());
      actualOutputRecordCount += upperLevelLateral.getRecordCount();
      assertTrue(RecordBatch.IterOutcome.NONE == upperLevelLateral.next());
      assertTrue(actualOutputRecordCount == expectedOutputRecordCount);
    } catch (AssertionError | Exception error) {
      fail();
    } finally {
      // Close all the resources for this test case
      upperLevelLateral.close();
      leftMockBatch_2.close();
      lowerLevelLateral.close();
      leftMockBatch_1.close();
      rightMockBatch_1.close();
      leftContainer2.clear();
      leftOutcomes2.clear();
    }
  }

  /**
   * This test generates an operator tree for multi level LATERAL by stacking 2 LATERAL and finally an UNNEST pair
   * (using MockRecord Batch) as left and right child of lower level LATERAL. In this setup the test try to simulate
   * the SchemaChange happening at upper level LATERAL left incoming second batch, which also results into the
   * SchemaChange of both left&right UNNEST of lower level LATERAL. This test validates that the schema change is
   * handled correctly by both upper and lower level LATERAL.
   *
   * @throws Exception
   */
  @Test
  public void testMultiLevelLateral_SchemaChange_LeftRightUnnest() throws Exception {
    // ** Prepare first pair of left batch and right batch for lower level LATERAL Lateral_1 **
    final LateralJoinPOP popConfig_1 = new LateralJoinPOP(null, null, JoinRelType.INNER, DrillLateralJoinRelBase.IMPLICIT_COLUMN, Lists.newArrayList());

    TupleMetadata leftSchemaWithImplicit = new SchemaBuilder()
      .add(popConfig_1.getImplicitRIDColumn(), TypeProtos.MinorType.INT)
      .add("id_left", TypeProtos.MinorType.INT)
      .add("cost_left", TypeProtos.MinorType.INT)
      .add("name_left", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    final RowSet.SingleRowSet emptyLeftRowSet_1 = fixture.rowSetBuilder(leftSchemaWithImplicit)
      .build();
    final RowSet.SingleRowSet nonEmptyLeftRowSet_1 = fixture.rowSetBuilder(leftSchemaWithImplicit)
      .addRow(1, 1, 10, "item1")
      .build();

    // Create left input schema for first batch
    TupleMetadata leftSchema2 = new SchemaBuilder()
      .add(popConfig_1.getImplicitRIDColumn(), TypeProtos.MinorType.INT)
      .add("id_left_new", TypeProtos.MinorType.INT)
      .add("cost_left_new", TypeProtos.MinorType.INT)
      .add("name_left_new", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    final RowSet.SingleRowSet emptyLeftRowSet_leftSchema2 = fixture.rowSetBuilder(leftSchema2).build();
    final RowSet.SingleRowSet nonEmptyLeftRowSet_leftSchema2 = fixture.rowSetBuilder(leftSchema2)
      .addRow(1, 6, 60, "item6")
      .build();

    leftContainer.add(emptyLeftRowSet_1.container());
    leftContainer.add(nonEmptyLeftRowSet_1.container());
    leftContainer.add(emptyLeftRowSet_leftSchema2.container());
    leftContainer.add(nonEmptyLeftRowSet_leftSchema2.container());

    // Get the left IterOutcomes for Lateral Join
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    leftOutcomes.add(RecordBatch.IterOutcome.EMIT);
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    leftOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final CloseableRecordBatch leftMockBatch_1 = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer, leftOutcomes, leftContainer.get(0).getSchema());

    // Get the right container with dummy data
    TupleMetadata rightSchema2 = new SchemaBuilder()
      .add(popConfig_1.getImplicitRIDColumn(), TypeProtos.MinorType.INT)
      .add("id_right_new", TypeProtos.MinorType.INT)
      .add("cost_right_new", TypeProtos.MinorType.VARCHAR)
      .add("name_right_new", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    final RowSet.SingleRowSet emptyRightRowSet_rightSchema2 = fixture.rowSetBuilder(rightSchema2).build();
    final RowSet.SingleRowSet nonEmptyRightRowSet_rightSchema2 = fixture.rowSetBuilder(rightSchema2)
      .addRow(1, 5, "51", "item51")
      .addRow(1, 6, "61", "item61")
      .addRow(1, 7, "71", "item71")
      .build();

    rightContainer.add(emptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet.container());
    rightContainer.add(emptyRightRowSet_rightSchema2.container());
    rightContainer.add(nonEmptyRightRowSet_rightSchema2.container());

    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);
    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final CloseableRecordBatch rightMockBatch_1 = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      rightContainer, rightOutcomes, rightContainer.get(0).getSchema());

    final LateralJoinBatch lowerLevelLateral = new LateralJoinBatch(popConfig_1, fixture.getFragmentContext(),
      leftMockBatch_1, rightMockBatch_1);

    // ** Prepare second pair of left and right batch for upper level Lateral_2 **

    // Create left input schema for first batch
    TupleMetadata leftSchema3 = new SchemaBuilder()
      .add("id_left_left", TypeProtos.MinorType.INT)
      .add("cost_left_left", TypeProtos.MinorType.INT)
      .add("name_left_left", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    final RowSet.SingleRowSet emptyLeftRowSet_leftSchema3 = fixture.rowSetBuilder(leftSchema3).build();
    final RowSet.SingleRowSet nonEmptyLeftRowSet_leftSchema3 = fixture.rowSetBuilder(leftSchema3)
      .addRow(6, 60, "item6")
      .build();

    // Get left input schema for second left batch
    TupleMetadata leftSchema4 = new SchemaBuilder()
      .add("id_left_left_new", TypeProtos.MinorType.INT)
      .add("cost_left_left_new", TypeProtos.MinorType.VARCHAR)
      .add("name_left_left_new", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    final RowSet.SingleRowSet nonEmptyLeftRowSet_leftSchema4 = fixture.rowSetBuilder(leftSchema4)
      .addRow(100, "100", "item100")
      .build();

    // Build Left container for upper level LATERAL operator
    final List<VectorContainer> leftContainer2 = new ArrayList<>(5);

    // Get the left container with dummy data
    leftContainer2.add(emptyLeftRowSet_leftSchema3.container());
    leftContainer2.add(nonEmptyLeftRowSet_leftSchema3.container());
    leftContainer2.add(nonEmptyLeftRowSet_leftSchema4.container());

    // Get the left container outcomes for upper level LATERAL operator
    final List<RecordBatch.IterOutcome> leftOutcomes2 = new ArrayList<>(5);
    leftOutcomes2.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    leftOutcomes2.add(RecordBatch.IterOutcome.OK);
    leftOutcomes2.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);

    final CloseableRecordBatch leftMockBatch_2 = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer2, leftOutcomes2, leftContainer2.get(0).getSchema());

    final LateralJoinBatch upperLevelLateral = new LateralJoinBatch(popConfig_1, fixture.getFragmentContext(),
      leftMockBatch_2, lowerLevelLateral);

    try {
      // 3 for first batch on left side and another 3 for next left batch
      final int expectedOutputRecordCount = 6;
      int actualOutputRecordCount = 0;

      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == upperLevelLateral.next());
      assertTrue(RecordBatch.IterOutcome.OK == upperLevelLateral.next());
      actualOutputRecordCount += upperLevelLateral.getRecordCount();
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == upperLevelLateral.next());
      actualOutputRecordCount += upperLevelLateral.getRecordCount();
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == upperLevelLateral.next());
      actualOutputRecordCount += upperLevelLateral.getRecordCount();
      assertTrue(RecordBatch.IterOutcome.OK == upperLevelLateral.next());
      actualOutputRecordCount += upperLevelLateral.getRecordCount();
      assertTrue(RecordBatch.IterOutcome.NONE == upperLevelLateral.next());
      assertTrue(actualOutputRecordCount == expectedOutputRecordCount);
    } catch (AssertionError | Exception error) {
      fail();
    } finally {
      // Close all the resources for this test case
      upperLevelLateral.close();
      leftMockBatch_2.close();
      lowerLevelLateral.close();
      leftMockBatch_1.close();
      rightMockBatch_1.close();
      leftContainer2.clear();
      leftOutcomes2.clear();
    }
  }

  /**
   * Test unsupported incoming batch to LATERAL with SelectionVector
   * @throws Exception
   */
  @Test
  public void testUnsupportedSelectionVector() throws Exception {
    final RowSet.SingleRowSet leftRowSet2 = fixture.rowSetBuilder(leftSchema)
      .addRow(2, 20, "item20")
      .withSv2()
      .build();

    // Get the left container with dummy data for Lateral Join
    leftContainer.add(leftRowSet2.container());

    // Get the left IterOutcomes for Lateral Join
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);

    // Create Left MockRecordBatch
    final CloseableRecordBatch leftMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer, leftOutcomes, leftContainer.get(0).getSchema());

    // Get the right container with dummy data
    rightContainer.add(emptyRightRowSet.container());

    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);

    final CloseableRecordBatch rightMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      rightContainer, rightOutcomes, rightContainer.get(0).getSchema());

    final LateralJoinBatch ljBatch = new LateralJoinBatch(ljPopConfig, fixture.getFragmentContext(),
      leftMockBatch, rightMockBatch);

    try {
      ljBatch.next();
      fail();
    } catch (UserException e) {
      assertEquals(ErrorType.UNSUPPORTED_OPERATION, e.getErrorType());
    } catch (AssertionError | Exception error) {
      fail();
    } finally {
      // Close all the resources for this test case
      ljBatch.close();
      leftMockBatch.close();
      rightMockBatch.close();
      leftRowSet2.clear();
    }
  }

  /**
   * Test to verify if OK_NEW_SCHEMA is received from left side of LATERAL post build schema phase and EMIT is
   * received from right side of LATERAL for each row on left side, then Lateral sends OK_NEW_SCHEMA downstream with
   * the output batch. LATERAL shouldn't send any batch with EMIT outcome to the downstream operator as it is the
   * consumer of all the EMIT outcomes. It will work fine in case of Multilevel LATERAL too since there the lower
   * LATERAL only sends EMIT after it receives it from left UNNEST.
   * @throws Exception
   */
  @Test
  public void test_OK_NEW_SCHEMAFromLeft_EmitFromRight_PostBuildSchema() throws Exception {
    // Get the left container with dummy data for Lateral Join
    TupleMetadata leftSchema3 = new SchemaBuilder()
      .add("id_left_left", TypeProtos.MinorType.INT)
      .add("cost_left_left", TypeProtos.MinorType.VARCHAR)
      .add("name_left_left", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    final RowSet.SingleRowSet nonEmptyLeftRowSet_leftSchema3 = fixture.rowSetBuilder(leftSchema3)
      .addRow(6, "60", "item6")
      .addRow(7, "70", "item7")
      .build();

    leftContainer.add(emptyLeftRowSet.container());
    leftContainer.add(nonEmptyLeftRowSet_leftSchema3.container());

    // Get the left IterOutcomes for Lateral Join
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);

    // Create Left MockRecordBatch
    final CloseableRecordBatch leftMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer, leftOutcomes, leftContainer.get(0).getSchema());

    // Get the right container with dummy data
    final RowSet.SingleRowSet nonEmptyRightRowSet2 = fixture.rowSetBuilder(rightSchema)
      .addRow(2, 10, 100, "list10")
      .build();

    rightContainer.add(emptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet2.container());

    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    rightOutcomes.add(RecordBatch.IterOutcome.OK);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final CloseableRecordBatch rightMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      rightContainer, rightOutcomes, rightContainer.get(0).getSchema());

    final LateralJoinBatch ljBatch = new LateralJoinBatch(ljPopConfig, fixture.getFragmentContext(),
      leftMockBatch, rightMockBatch);

    try {
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == ljBatch.next());
      assertTrue(ljBatch.getRecordCount() == 0);

      // Since Right batch is empty it should drain left and return NONE
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == ljBatch.next());
      final int expectedOutputCount = nonEmptyRightRowSet.rowCount() + nonEmptyRightRowSet2.rowCount();
      assertTrue(ljBatch.getRecordCount() == expectedOutputCount);
    } catch (AssertionError | Exception error) {
      fail();
    } finally {
      // Close all the resources for this test case
      ljBatch.close();
      leftMockBatch.close();
      rightMockBatch.close();
    }
  }

  /**
   * Verifies that if a non-empty batch with OK_NEW_SCHEMA is received from right side post buildSchema phase then it
   * is handled correctly by sending an empty batch with OK_NEW_SCHEMA and later consuming it to produce actual
   * output batch with some data
   */
  @Test
  public void testPostBuildSchema_OK_NEW_SCHEMA_NonEmptyRightBatch() throws Exception {
    // Create left input schema 2
    TupleMetadata leftSchema2 = new SchemaBuilder()
      .add("id_left", TypeProtos.MinorType.INT)
      .add("cost_left", TypeProtos.MinorType.VARCHAR)
      .add("name_left", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    // Create right input schema
    TupleMetadata rightSchema2 = new SchemaBuilder()
      .add(ljPopConfig.getImplicitRIDColumn(), TypeProtos.MinorType.INT)
      .add("id_right", TypeProtos.MinorType.INT)
      .add("cost_right", TypeProtos.MinorType.VARCHAR)
      .add("name_right", TypeProtos.MinorType.VARCHAR)
      .buildSchema();


    // Create data for left input
    final RowSet.SingleRowSet leftRowSet2 = fixture.rowSetBuilder(leftSchema2)
      .addRow(2, "20", "item20")
      .build();

    // Create data for right input
    final RowSet.SingleRowSet emptyRightRowSet2 = fixture.rowSetBuilder(rightSchema2)
      .build();

    final RowSet.SingleRowSet nonEmptyRightRowSet2 = fixture.rowSetBuilder(rightSchema2)
      .addRow(1, 4, "41", "item41")
      .addRow(1, 5, "51", "item51")
      .build();

    // Get the left container with dummy data for Lateral Join
    leftContainer.add(nonEmptyLeftRowSet.container());
    leftContainer.add(leftRowSet2.container());

    // Get the left IterOutcomes for Lateral Join
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);

    // Create Left MockRecordBatch
    final CloseableRecordBatch leftMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer, leftOutcomes, leftContainer.get(0).getSchema());

    // Get the right container with dummy data
    // first OK_NEW_SCHEMA batch
    rightContainer.add(emptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet2.container()); // non-empty OK_NEW_SCHEMA batch
    rightContainer.add(emptyRightRowSet2.container());

    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);
    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final CloseableRecordBatch rightMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      rightContainer, rightOutcomes, rightContainer.get(0).getSchema());

    final LateralJoinBatch ljBatch = new LateralJoinBatch(ljPopConfig, fixture.getFragmentContext(),
      leftMockBatch, rightMockBatch);

    try {
      int totalRecordCount = 0;
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == ljBatch.next());
      assertTrue(RecordBatch.IterOutcome.OK == ljBatch.next());
      totalRecordCount += ljBatch.getRecordCount();
      // This means 2 output record batches were received because of Schema change
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == ljBatch.next());
      assertEquals(0, ljBatch.getRecordCount());
      totalRecordCount += ljBatch.getRecordCount();
      assertTrue(RecordBatch.IterOutcome.OK == ljBatch.next());
      totalRecordCount += ljBatch.getRecordCount();
      assertTrue(totalRecordCount ==
        (nonEmptyLeftRowSet.rowCount() * nonEmptyRightRowSet.rowCount() +
          leftRowSet2.rowCount() * nonEmptyRightRowSet2.rowCount()));

      assertTrue(RecordBatch.IterOutcome.NONE == ljBatch.next());
    } catch (AssertionError | Exception error) {
      fail();
    } finally {
      // Close all the resources for this test case
      ljBatch.close();
      leftMockBatch.close();
      rightMockBatch.close();
      leftRowSet2.clear();
      emptyRightRowSet2.clear();
      nonEmptyRightRowSet2.clear();
    }
  }

  /**
   * Test to verify in case of Multilevel lateral when a non-empty OK_NEW_SCHEMA batch post build schema phase is
   * received from right most UNNEST of lower LATERAL then pipeline works fine.
   * @throws Exception
   */
  @Test
  public void testMultiLevelLateral_SchemaChange_LeftRightUnnest_NonEmptyBatch() throws Exception {
    // ** Prepare first pair of left batch and right batch for lower level LATERAL Lateral_1 **
    final LateralJoinPOP popConfig_1 = new LateralJoinPOP(null, null, JoinRelType.INNER, DrillLateralJoinRelBase.IMPLICIT_COLUMN, Lists.newArrayList());

    TupleMetadata leftSchemaWithImplicit = new SchemaBuilder()
      .add(popConfig_1.getImplicitRIDColumn(), TypeProtos.MinorType.INT)
      .add("id_left", TypeProtos.MinorType.INT)
      .add("cost_left", TypeProtos.MinorType.INT)
      .add("name_left", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    final RowSet.SingleRowSet emptyLeftRowSet_1 = fixture.rowSetBuilder(leftSchemaWithImplicit)
      .build();
    final RowSet.SingleRowSet nonEmptyLeftRowSet_1 = fixture.rowSetBuilder(leftSchemaWithImplicit)
      .addRow(1, 1, 10, "item1")
      .build();

    // Create left input schema for first batch
    TupleMetadata leftSchema2 = new SchemaBuilder()
      .add(popConfig_1.getImplicitRIDColumn(), TypeProtos.MinorType.INT)
      .add("id_left_new", TypeProtos.MinorType.INT)
      .add("cost_left_new", TypeProtos.MinorType.INT)
      .add("name_left_new", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    final RowSet.SingleRowSet emptyLeftRowSet_leftSchema2 = fixture.rowSetBuilder(leftSchema2).build();
    final RowSet.SingleRowSet nonEmptyLeftRowSet_leftSchema2 = fixture.rowSetBuilder(leftSchema2)
      .addRow(1, 6, 60, "item6")
      .build();

    leftContainer.add(emptyLeftRowSet_1.container());
    leftContainer.add(nonEmptyLeftRowSet_1.container());
    leftContainer.add(emptyLeftRowSet_leftSchema2.container());
    leftContainer.add(nonEmptyLeftRowSet_leftSchema2.container());

    // Get the left IterOutcomes for Lateral Join
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    leftOutcomes.add(RecordBatch.IterOutcome.EMIT);
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    leftOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final CloseableRecordBatch leftMockBatch_1 = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer, leftOutcomes, leftContainer.get(0).getSchema());

    // Get the right container with dummy data
    TupleMetadata rightSchema2 = new SchemaBuilder()
      .add(popConfig_1.getImplicitRIDColumn(), TypeProtos.MinorType.INT)
      .add("id_right_new", TypeProtos.MinorType.INT)
      .add("cost_right_new", TypeProtos.MinorType.VARCHAR)
      .add("name_right_new", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    final RowSet.SingleRowSet emptyRightRowSet_rightSchema2 = fixture.rowSetBuilder(rightSchema2).build();
    final RowSet.SingleRowSet nonEmptyRightRowSet_rightSchema2 = fixture.rowSetBuilder(rightSchema2)
      .addRow(1, 5, "51", "item51")
      .addRow(1, 6, "61", "item61")
      .addRow(1, 7, "71", "item71")
      .build();

    rightContainer.add(emptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet_rightSchema2.container()); // non-empty batch with Ok_new_schema
    rightContainer.add(emptyRightRowSet_rightSchema2.container());

    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);
    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final CloseableRecordBatch rightMockBatch_1 = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      rightContainer, rightOutcomes, rightContainer.get(0).getSchema());

    final LateralJoinBatch lowerLevelLateral = new LateralJoinBatch(popConfig_1, fixture.getFragmentContext(),
      leftMockBatch_1, rightMockBatch_1);

    // ** Prepare second pair of left and right batch for upper level Lateral_2 **

    // Create left input schema for first batch
    TupleMetadata leftSchema3 = new SchemaBuilder()
      .add("id_left_left", TypeProtos.MinorType.INT)
      .add("cost_left_left", TypeProtos.MinorType.INT)
      .add("name_left_left", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    final RowSet.SingleRowSet emptyLeftRowSet_leftSchema3 = fixture.rowSetBuilder(leftSchema3).build();
    final RowSet.SingleRowSet nonEmptyLeftRowSet_leftSchema3 = fixture.rowSetBuilder(leftSchema3)
      .addRow(6, 60, "item6")
      .build();

    // Get left input schema for second left batch
    TupleMetadata leftSchema4 = new SchemaBuilder()
      .add("id_left_left_new", TypeProtos.MinorType.INT)
      .add("cost_left_left_new", TypeProtos.MinorType.VARCHAR)
      .add("name_left_left_new", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    final RowSet.SingleRowSet nonEmptyLeftRowSet_leftSchema4 = fixture.rowSetBuilder(leftSchema4)
      .addRow(100, "100", "item100")
      .build();

    // Build Left container for upper level LATERAL operator
    final List<VectorContainer> leftContainer2 = new ArrayList<>(5);

    // Get the left container with dummy data
    leftContainer2.add(emptyLeftRowSet_leftSchema3.container());
    leftContainer2.add(nonEmptyLeftRowSet_leftSchema3.container());
    leftContainer2.add(nonEmptyLeftRowSet_leftSchema4.container());

    // Get the left container outcomes for upper level LATERAL operator
    final List<RecordBatch.IterOutcome> leftOutcomes2 = new ArrayList<>(5);
    leftOutcomes2.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    leftOutcomes2.add(RecordBatch.IterOutcome.OK);
    leftOutcomes2.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);

    final CloseableRecordBatch leftMockBatch_2 = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer2, leftOutcomes2, leftContainer2.get(0).getSchema());

    final LateralJoinBatch upperLevelLateral = new LateralJoinBatch(popConfig_1, fixture.getFragmentContext(),
      leftMockBatch_2, lowerLevelLateral);

    try {
      // 3 for first batch on left side and another 3 for next left batch
      final int expectedOutputRecordCount = 6;
      int actualOutputRecordCount = 0;

      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == upperLevelLateral.next());
      assertTrue(RecordBatch.IterOutcome.OK == upperLevelLateral.next());
      actualOutputRecordCount += upperLevelLateral.getRecordCount();
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == upperLevelLateral.next());
      actualOutputRecordCount += upperLevelLateral.getRecordCount();
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == upperLevelLateral.next());
      actualOutputRecordCount += upperLevelLateral.getRecordCount();
      assertTrue(RecordBatch.IterOutcome.OK == upperLevelLateral.next());
      actualOutputRecordCount += upperLevelLateral.getRecordCount();
      assertTrue(RecordBatch.IterOutcome.NONE == upperLevelLateral.next());
      assertTrue(actualOutputRecordCount == expectedOutputRecordCount);
    } catch (AssertionError | Exception error) {
      fail();
    } finally {
      // Close all the resources for this test case
      upperLevelLateral.close();
      leftMockBatch_2.close();
      lowerLevelLateral.close();
      leftMockBatch_1.close();
      rightMockBatch_1.close();
      leftContainer2.clear();
      leftOutcomes2.clear();
    }
  }

  /**
   * Test to verify that for first left incoming if there is no right side incoming batch and then second left
   * incoming comes with schema change, then the schema change with empty output batch for first incoming is handled
   * properly.
   * @throws Exception
   */
  @Test
  public void testLateral_SchemaChange_Left_EmptyRightBatchForFirst() throws Exception {
    // Create left input schema 2
    TupleMetadata leftSchema2 = new SchemaBuilder()
      .add("id_left", TypeProtos.MinorType.INT)
      .add("cost_left", TypeProtos.MinorType.VARCHAR)
      .add("name_left", TypeProtos.MinorType.VARCHAR)
      .buildSchema();


    // Create data for left input
    final RowSet.SingleRowSet leftRowSet2 = fixture.rowSetBuilder(leftSchema2)
      .addRow(2, "20", "item20")
      .build();

    // Get the left container with dummy data for Lateral Join
    leftContainer.add(nonEmptyLeftRowSet.container());
    leftContainer.add(leftRowSet2.container());

    // Get the left IterOutcomes for Lateral Join
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);

    // Create Left MockRecordBatch
    final CloseableRecordBatch leftMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer, leftOutcomes, leftContainer.get(0).getSchema());

    // Get the right container with dummy data
    // first OK_NEW_SCHEMA batch
    rightContainer.add(emptyRightRowSet.container());
    rightContainer.add(emptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet.container()); // non-empty OK_NEW_SCHEMA batch

    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final CloseableRecordBatch rightMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      rightContainer, rightOutcomes, rightContainer.get(0).getSchema());

    final LateralJoinBatch ljBatch = new LateralJoinBatch(ljPopConfig, fixture.getFragmentContext(),
      leftMockBatch, rightMockBatch);

    try {
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == ljBatch.next());
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == ljBatch.next());
      // This means 2 output record batches were received because of Schema change
      assertEquals(3, ljBatch.getRecordCount());
      assertTrue(RecordBatch.IterOutcome.NONE == ljBatch.next());
    } catch (AssertionError | Exception error) {
      fail();
    } finally {
      // Close all the resources for this test case
      ljBatch.close();
      leftMockBatch.close();
      rightMockBatch.close();
      leftRowSet2.clear();
    }
  }

  private void testExcludedColumns(List<SchemaPath> excludedCols, CloseableRecordBatch left,
                                   CloseableRecordBatch right, RowSet expectedRowSet) throws Exception {
    LateralJoinPOP lateralPop = new LateralJoinPOP(null, null, JoinRelType.INNER, DrillLateralJoinRelBase.IMPLICIT_COLUMN, excludedCols);
    final LateralJoinBatch ljBatch = new LateralJoinBatch(lateralPop, fixture.getFragmentContext(), left, right);

    try {
      assertTrue(RecordBatch.IterOutcome.OK_NEW_SCHEMA == ljBatch.next());
      assertTrue(RecordBatch.IterOutcome.OK == ljBatch.next());
      RowSet actualRowSet = DirectRowSet.fromContainer(ljBatch.getContainer());
      new RowSetComparison(expectedRowSet).verify(actualRowSet);
      assertTrue(RecordBatch.IterOutcome.NONE == ljBatch.next());
    } finally {
      ljBatch.close();
      left.close();
      right.close();
      expectedRowSet.clear();
    }
  }

  @Test
  public void testFillingUpOutputBatch_WithExcludedColumns() throws Exception {
    // Create data for left input
    final RowSet.SingleRowSet leftRowSet2 = fixture.rowSetBuilder(leftSchema)
      .addRow(2, 20, "item20")
      .build();

    // Create data for right input
    final RowSet.SingleRowSet nonEmptyRightRowSet2 = fixture.rowSetBuilder(rightSchema)
      .addRow(1, 4, 41, "item41")
      .addRow(1, 5, 51, "item51")
      .build();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("id_left", TypeProtos.MinorType.INT)
      .add("name_left", TypeProtos.MinorType.VARCHAR)
      .add("id_right", TypeProtos.MinorType.INT)
      .add("cost_right", TypeProtos.MinorType.INT)
      .add("name_right", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    final RowSet.SingleRowSet expectedRowSet = fixture.rowSetBuilder(expectedSchema)
      .addRow(1, "item1", 1, 11, "item11")
      .addRow(1, "item1", 2, 21, "item21")
      .addRow(1, "item1", 3, 31, "item31")
      .addRow(2, "item20", 4, 41, "item41")
      .addRow(2, "item20", 5, 51, "item51")
      .build();

    // Get the left container with dummy data for Lateral Join
    leftContainer.add(nonEmptyLeftRowSet.container());
    leftContainer.add(leftRowSet2.container());

    // Get the left IterOutcomes for Lateral Join
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    leftOutcomes.add(RecordBatch.IterOutcome.OK);

    // Create Left MockRecordBatch
    final CloseableRecordBatch leftMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer, leftOutcomes, leftContainer.get(0).getSchema());

    // Get the right container with dummy data
    rightContainer.add(emptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet2.container());

    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final CloseableRecordBatch rightMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      rightContainer, rightOutcomes, rightContainer.get(0).getSchema());

    List<SchemaPath> excludedCols = new ArrayList<>();
    excludedCols.add(SchemaPath.getSimplePath("cost_left"));

    try {
      testExcludedColumns(excludedCols, leftMockBatch, rightMockBatch, expectedRowSet);
    } finally {
      // Close all the resources for this test case
      leftRowSet2.clear();
      nonEmptyRightRowSet2.clear();
    }
  }

  @Test
  public void testFillingUpOutputBatch_With2ExcludedColumns() throws Exception {
    // Create data for left input
    final RowSet.SingleRowSet leftRowSet2 = fixture.rowSetBuilder(leftSchema)
      .addRow(2, 20, "item20")
      .build();

    // Create data for right input
    final RowSet.SingleRowSet nonEmptyRightRowSet2 = fixture.rowSetBuilder(rightSchema)
      .addRow(1, 4, 41, "item41")
      .addRow(1, 5, 51, "item51")
      .build();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("name_left", TypeProtos.MinorType.VARCHAR)
      .add("cost_right", TypeProtos.MinorType.INT)
      .add("name_right", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    final RowSet.SingleRowSet expectedRowSet = fixture.rowSetBuilder(expectedSchema)
      .addRow("item1", 11, "item11")
      .addRow("item1", 21, "item21")
      .addRow("item1", 31, "item31")
      .addRow("item20", 41, "item41")
      .addRow("item20", 51, "item51")
      .build();

    // Get the left container with dummy data for Lateral Join
    leftContainer.add(nonEmptyLeftRowSet.container());
    leftContainer.add(leftRowSet2.container());

    // Get the left IterOutcomes for Lateral Join
    leftOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    leftOutcomes.add(RecordBatch.IterOutcome.OK);

    // Create Left MockRecordBatch
    final CloseableRecordBatch leftMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      leftContainer, leftOutcomes, leftContainer.get(0).getSchema());

    // Get the right container with dummy data
    rightContainer.add(emptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet.container());
    rightContainer.add(nonEmptyRightRowSet2.container());

    rightOutcomes.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);
    rightOutcomes.add(RecordBatch.IterOutcome.EMIT);

    final CloseableRecordBatch rightMockBatch = new MockRecordBatch(fixture.getFragmentContext(), operatorContext,
      rightContainer, rightOutcomes, rightContainer.get(0).getSchema());

    List<SchemaPath> excludedCols = new ArrayList<>();
    excludedCols.add(SchemaPath.getSimplePath("cost_left"));
    excludedCols.add(SchemaPath.getSimplePath("id_left"));
    excludedCols.add(SchemaPath.getSimplePath("id_right"));

    try {
      testExcludedColumns(excludedCols, leftMockBatch, rightMockBatch, expectedRowSet);
    } finally {
      // Close all the resources for this test case
      leftRowSet2.clear();
      nonEmptyRightRowSet2.clear();
    }
  }
}
