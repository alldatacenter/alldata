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

import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.sql.SqlKind;
import org.apache.drill.categories.OperatorTest;
import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.common.logical.data.JoinCondition;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.config.HashJoinPOP;
import org.apache.drill.exec.physical.impl.MockRecordBatch;
import org.apache.drill.test.PhysicalOpUnitTestBase;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.store.mock.MockStorePOP;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertSame;

/**
 *  Unit tests of the Hash Join getting various outcomes as input
 *  with uninitialized vector containers
 */
@Category(OperatorTest.class)
public class TestHashJoinOutcome extends PhysicalOpUnitTestBase {
  // private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestHashJoinOutcome.class);

  // input batch schemas
  private static TupleMetadata inputSchemaRight;
  private static TupleMetadata inputSchemaLeft;
  private static BatchSchema batchSchemaRight;
  private static BatchSchema batchSchemaLeft;

  // Input containers -- where row count is not set for the 2nd container !!
  private final List<VectorContainer> uninitialized2ndInputContainersRight = new ArrayList<>(5);
  private final List<VectorContainer> uninitialized2ndInputContainersLeft = new ArrayList<>(5);

  private RowSet.SingleRowSet emptyInputRowSetRight;
  private RowSet.SingleRowSet emptyInputRowSetLeft;

  // default Non-Empty input RowSets
  private RowSet.SingleRowSet nonEmptyInputRowSetRight;
  private RowSet.SingleRowSet nonEmptyInputRowSetLeft;

  // List of incoming containers
  private final List<VectorContainer> inputContainerRight = new ArrayList<>(5);
  private final List<VectorContainer> inputContainerLeft = new ArrayList<>(5);

  // List of incoming IterOutcomes
  private final List<RecordBatch.IterOutcome> inputOutcomesRight = new ArrayList<>(5);
  private final List<RecordBatch.IterOutcome> inputOutcomesLeft = new ArrayList<>(5);

  @BeforeClass
  public static void setUpBeforeClass() {
    inputSchemaRight = new SchemaBuilder()
      .add("rightcol", TypeProtos.MinorType.INT)
      .buildSchema();
    batchSchemaRight = new BatchSchema(BatchSchema.SelectionVectorMode.NONE, inputSchemaRight.toFieldList());
    inputSchemaLeft = new SchemaBuilder()
      .add("leftcol", TypeProtos.MinorType.INT)
      .buildSchema();
    batchSchemaLeft = new BatchSchema(BatchSchema.SelectionVectorMode.NONE, inputSchemaLeft.toFieldList());
  }

  private void prepareUninitContainers(List<VectorContainer> emptyInputContainers,
                                       BatchSchema batchSchema) {
    BufferAllocator allocator = operatorFixture.getFragmentContext().getAllocator();

    VectorContainer vc1 = new VectorContainer(allocator, batchSchema);
    // set for first vc (with OK_NEW_SCHEMA) because record count is checked at AbstractRecordBatch.next
    vc1.setRecordCount(0);
    VectorContainer vc2 = new VectorContainer(allocator, batchSchema);
    // Note - Uninitialized: Record count NOT SET for vc2 !!
    emptyInputContainers.add(vc1);
    emptyInputContainers.add(vc2);
  }

  @Before
  public void beforeTest() throws Exception {

    prepareUninitContainers(uninitialized2ndInputContainersLeft, batchSchemaLeft);

    prepareUninitContainers(uninitialized2ndInputContainersRight, batchSchemaRight);

    nonEmptyInputRowSetRight = operatorFixture.rowSetBuilder(inputSchemaRight)
      .addRow(123)
      .build();
    nonEmptyInputRowSetLeft = operatorFixture.rowSetBuilder(inputSchemaLeft)
      .addRow(123)
      .build();

    // Prepare various (empty/non-empty) containers for each side of the join
    emptyInputRowSetLeft = operatorFixture.rowSetBuilder(inputSchemaLeft).build();
    emptyInputRowSetRight = operatorFixture.rowSetBuilder(inputSchemaRight).build();

    inputContainerRight.add(emptyInputRowSetRight.container());
    inputContainerRight.add(nonEmptyInputRowSetRight.container());

    inputContainerLeft.add(emptyInputRowSetLeft.container());
    inputContainerLeft.add(nonEmptyInputRowSetLeft.container());

    final PhysicalOperator mockPopConfig = new MockStorePOP(null);
    mockOpContext(mockPopConfig, 0, 0);
  }

  @After
  public void afterTest() {
    emptyInputRowSetRight.clear();
    emptyInputRowSetLeft.clear();
    nonEmptyInputRowSetRight.clear();
    nonEmptyInputRowSetLeft.clear();
    inputContainerRight.clear();
    inputOutcomesRight.clear();
    inputContainerLeft.clear();
    inputOutcomesLeft.clear();
  }

  enum UninitializedSide { // which side of the join has an uninitialized container
    Right(true), Left(false);
    public boolean isRight;
    UninitializedSide(boolean which) {this.isRight = which;}
  }

  /**
   *  Run the Hash Join where one side has an uninitialized container (the 2nd one)
   * @param uninitializedSide Which side (right or left) is the uninitialized
   * @param specialOutcome What outcome the uninitialized container has
   * @param expectedOutcome what result outcome is expected
   */
  private void testHashJoinOutcomes(UninitializedSide uninitializedSide, RecordBatch.IterOutcome specialOutcome,
                                    RecordBatch.IterOutcome expectedOutcome) {

    inputOutcomesLeft.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomesLeft.add( uninitializedSide.isRight ? RecordBatch.IterOutcome.OK : specialOutcome);

    inputOutcomesRight.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomesRight.add( uninitializedSide.isRight ? specialOutcome : RecordBatch.IterOutcome.OK);

    final MockRecordBatch mockInputBatchRight = new MockRecordBatch(operatorFixture.getFragmentContext(), opContext,
      uninitializedSide.isRight ? uninitialized2ndInputContainersRight : inputContainerRight,
      inputOutcomesRight, batchSchemaRight);
    final MockRecordBatch mockInputBatchLeft = new MockRecordBatch(operatorFixture.getFragmentContext(), opContext,
      uninitializedSide.isRight ? inputContainerLeft : uninitialized2ndInputContainersLeft,
      inputOutcomesLeft, batchSchemaLeft);

    List<JoinCondition> conditions = Lists.newArrayList();

    conditions.add(new JoinCondition( SqlKind.EQUALS.toString(),
      FieldReference.getWithQuotedRef("leftcol"),
      FieldReference.getWithQuotedRef("rightcol")));

    HashJoinPOP hjConf = new HashJoinPOP(null, null, conditions, JoinRelType.INNER);

    HashJoinBatch hjBatch = new HashJoinBatch(hjConf,operatorFixture.getFragmentContext(), mockInputBatchLeft, mockInputBatchRight );

    RecordBatch.IterOutcome gotOutcome = hjBatch.next();
    assertSame(gotOutcome, RecordBatch.IterOutcome.OK_NEW_SCHEMA);

    gotOutcome = hjBatch.next();
    assertSame(gotOutcome, expectedOutcome); // verify returned outcome
  }

  @Test
  public void testHashJoinNoneOutcomeUninitRightSide() {
    testHashJoinOutcomes(UninitializedSide.Right, RecordBatch.IterOutcome.NONE, RecordBatch.IterOutcome.NONE);
  }

  @Test
  public void testHashJoinNoneOutcomeUninitLeftSide() {
    testHashJoinOutcomes(UninitializedSide.Left, RecordBatch.IterOutcome.NONE, RecordBatch.IterOutcome.NONE);
  }

  /**
   * Testing for DRILL-6755: No Hash Table is built when the first probe batch is NONE
   */
  @Test
  public void testHashJoinWhenProbeIsNONE() {

    inputOutcomesLeft.add(RecordBatch.IterOutcome.NONE);

    inputOutcomesRight.add(RecordBatch.IterOutcome.OK_NEW_SCHEMA);
    inputOutcomesRight.add(RecordBatch.IterOutcome.OK);
    inputOutcomesRight.add(RecordBatch.IterOutcome.NONE);

    // for the probe side input - use multiple batches (to check that they are all cleared/drained)
    final List<VectorContainer> buildSideinputContainer = new ArrayList<>(5);
    buildSideinputContainer.add(emptyInputRowSetRight.container());
    buildSideinputContainer.add(nonEmptyInputRowSetRight.container());
    RowSet.SingleRowSet secondInputRowSetRight = operatorFixture.rowSetBuilder(inputSchemaRight).addRow(456).build();
    RowSet.SingleRowSet thirdInputRowSetRight = operatorFixture.rowSetBuilder(inputSchemaRight).addRow(789).build();
    buildSideinputContainer.add(secondInputRowSetRight.container());
    buildSideinputContainer.add(thirdInputRowSetRight.container());

    final MockRecordBatch mockInputBatchRight = new MockRecordBatch(operatorFixture.getFragmentContext(), opContext, buildSideinputContainer, inputOutcomesRight, batchSchemaRight);
    final MockRecordBatch mockInputBatchLeft = new MockRecordBatch(operatorFixture.getFragmentContext(), opContext, inputContainerLeft, inputOutcomesLeft, batchSchemaLeft);

    List<JoinCondition> conditions = Lists.newArrayList();

    conditions.add(new JoinCondition(SqlKind.EQUALS.toString(), FieldReference.getWithQuotedRef("leftcol"), FieldReference.getWithQuotedRef("rightcol")));

    HashJoinPOP hjConf = new HashJoinPOP(null, null, conditions, JoinRelType.INNER);

    HashJoinBatch hjBatch = new HashJoinBatch(hjConf, operatorFixture.getFragmentContext(), mockInputBatchLeft, mockInputBatchRight);

    RecordBatch.IterOutcome gotOutcome = hjBatch.next();
    assertSame(gotOutcome, RecordBatch.IterOutcome.OK_NEW_SCHEMA);

    gotOutcome = hjBatch.next();
    assertSame(gotOutcome, RecordBatch.IterOutcome.NONE);

    secondInputRowSetRight.clear();
    thirdInputRowSetRight.clear();
    buildSideinputContainer.clear();
  }
}
