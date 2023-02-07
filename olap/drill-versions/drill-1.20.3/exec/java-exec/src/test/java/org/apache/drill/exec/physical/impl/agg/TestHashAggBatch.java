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
package org.apache.drill.exec.physical.impl.agg;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.common.expression.ExpressionPosition;
import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.common.expression.FunctionCall;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.data.NamedExpression;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.physical.config.HashAggregate;
import org.apache.drill.exec.physical.impl.MockRecordBatch;
import org.apache.drill.exec.planner.physical.AggPrelBase;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.test.PhysicalOpUnitTestBase;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.apache.drill.exec.ExecConstants.HASHAGG_NUM_PARTITIONS_KEY;

public class TestHashAggBatch extends PhysicalOpUnitTestBase {
  public static final String FIRST_NAME_COL = "firstname";
  public static final String LAST_NAME_COL = "lastname";
  public static final String STUFF_COL = "stuff";
  public static final String TOTAL_STUFF_COL = "totalstuff";

  public static final List<String> FIRST_NAMES = ImmutableList.of(
    "Strawberry",
    "Banana",
    "Mango",
    "Grape");

  public static final List<String> LAST_NAMES = ImmutableList.of(
    "Red",
    "Green",
    "Blue",
    "Purple");

  public static final TupleMetadata INT_OUTPUT_SCHEMA = new SchemaBuilder()
    .add(FIRST_NAME_COL, TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.REQUIRED)
    .add(LAST_NAME_COL, TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.REQUIRED)
    .add(TOTAL_STUFF_COL, TypeProtos.MinorType.BIGINT, TypeProtos.DataMode.OPTIONAL)
    .buildSchema();

  // TODO remove this in order to test multiple partitions
  @Before
  public void setupSimpleSingleBatchSumTestPhase1of2() {
    operatorFixture.getOptionManager().setLocalOption(HASHAGG_NUM_PARTITIONS_KEY, 1);
  }

  @Test
  public void simpleSingleBatchSumTestPhase1of2() throws Exception {
    batchSumTest(100, Integer.MAX_VALUE, AggPrelBase.OperatorPhase.PHASE_1of2);
  }

  @Test
  public void simpleMultiBatchSumTestPhase1of2() throws Exception {
    batchSumTest(100, 100, AggPrelBase.OperatorPhase.PHASE_1of2);
  }

  @Test
  public void simpleSingleBatchSumTestPhase1of1() throws Exception {
    batchSumTest(100, Integer.MAX_VALUE, AggPrelBase.OperatorPhase.PHASE_1of1);
  }

  @Test
  public void simpleMultiBatchSumTestPhase1of1() throws Exception {
    batchSumTest(100, 100, AggPrelBase.OperatorPhase.PHASE_1of1);
  }

  @Test
  public void simpleSingleBatchSumTestPhase2of2() throws Exception {
    batchSumTest(100, Integer.MAX_VALUE, AggPrelBase.OperatorPhase.PHASE_2of2);
  }

  @Test
  public void simpleMultiBatchSumTestPhase2of2() throws Exception {
    batchSumTest(100, 100, AggPrelBase.OperatorPhase.PHASE_2of2);
  }

  private void batchSumTest(int totalCount, int maxInputBatchSize, AggPrelBase.OperatorPhase phase) throws Exception {
    final HashAggregate hashAggregate = createHashAggPhysicalOperator(phase);
    final List<RowSet> inputRowSets = buildInputRowSets(TypeProtos.MinorType.INT, TypeProtos.DataMode.REQUIRED,
      totalCount, maxInputBatchSize);

    final MockRecordBatch.Builder rowSetBatchBuilder = new MockRecordBatch.Builder();
    inputRowSets.forEach(rowSet -> rowSetBatchBuilder.sendData(rowSet));
    final MockRecordBatch inputRowSetBatch = rowSetBatchBuilder.build(fragContext);

    final RowSet expectedRowSet = buildIntExpectedRowSet(totalCount);

    opTestBuilder()
      .physicalOperator(hashAggregate)
      .combineOutputBatches()
      .unordered()
      .addUpstreamBatch(inputRowSetBatch)
      .addExpectedResult(expectedRowSet)
      .go();
  }

  private HashAggregate createHashAggPhysicalOperator(AggPrelBase.OperatorPhase phase) {
    final List<NamedExpression> keyExpressions = Lists.newArrayList(
      new NamedExpression(SchemaPath.getSimplePath(FIRST_NAME_COL), new FieldReference(FIRST_NAME_COL)),
      new NamedExpression(SchemaPath.getSimplePath(LAST_NAME_COL), new FieldReference(LAST_NAME_COL)));

    final List<NamedExpression> aggExpressions = Lists.newArrayList(
      new NamedExpression(
        new FunctionCall("sum", ImmutableList.of(SchemaPath.getSimplePath(STUFF_COL)),
          new ExpressionPosition(null, 0)),
        new FieldReference(TOTAL_STUFF_COL)));

    return new HashAggregate(
      null,
      phase,
      keyExpressions,
      aggExpressions,
      0.0f);
  }

  private TupleMetadata buildInputSchema(TypeProtos.MinorType minorType, TypeProtos.DataMode dataMode) {
    return new SchemaBuilder()
      .add(FIRST_NAME_COL, TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.REQUIRED)
      .add(LAST_NAME_COL, TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.REQUIRED)
      .add(STUFF_COL, minorType, dataMode)
      .buildSchema();
  }

  private List<RowSet> buildInputRowSets(final TypeProtos.MinorType minorType,
                                  final TypeProtos.DataMode dataMode,
                                  final int dataCount,
                                  final int maxBatchSize) {
    Preconditions.checkArgument(dataCount > 0);
    Preconditions.checkArgument(maxBatchSize > 0);

    List<RowSet> inputRowSets = new ArrayList<>();
    int currentBatchSize = 0;
    RowSetBuilder inputRowSetBuilder = null;

    for (int multiplier = 1, firstNameIndex = 0; firstNameIndex < FIRST_NAMES.size(); firstNameIndex++) {
      final String firstName = FIRST_NAMES.get(firstNameIndex);

      for (int lastNameIndex = 0; lastNameIndex < LAST_NAMES.size(); lastNameIndex++, multiplier++) {
        final String lastName = LAST_NAMES.get(lastNameIndex);

        for (int index = 1; index <= dataCount; index++) {
          final int num = index * multiplier;

          if (currentBatchSize == 0) {
            final TupleMetadata inputSchema = buildInputSchema(minorType, dataMode);
            inputRowSetBuilder = new RowSetBuilder(operatorFixture.allocator(), inputSchema);
          }

          inputRowSetBuilder.addRow(firstName, lastName, num);
          currentBatchSize++;

          if (currentBatchSize == maxBatchSize) {
            final RowSet rowSet = inputRowSetBuilder.build();
            inputRowSets.add(rowSet);
            currentBatchSize = 0;
          }
        }
      }
    }

    if (currentBatchSize != 0) {
      inputRowSets.add(inputRowSetBuilder.build());
    }

    return inputRowSets;
  }

  private RowSet buildIntExpectedRowSet(final int dataCount) {
    final RowSetBuilder expectedRowSetBuilder = new RowSetBuilder(operatorFixture.allocator(), INT_OUTPUT_SCHEMA);

    for (int multiplier = 1, firstNameIndex = 0; firstNameIndex < FIRST_NAMES.size(); firstNameIndex++) {
      final String firstName = FIRST_NAMES.get(firstNameIndex);

      for (int lastNameIndex = 0; lastNameIndex < LAST_NAMES.size(); lastNameIndex++, multiplier++) {
        final String lastName = LAST_NAMES.get(lastNameIndex);
        final long total = ((dataCount * (dataCount + 1)) / 2) * multiplier;

        expectedRowSetBuilder.addRow(firstName, lastName, total);
      }
    }

    return expectedRowSetBuilder.build();
  }
}
