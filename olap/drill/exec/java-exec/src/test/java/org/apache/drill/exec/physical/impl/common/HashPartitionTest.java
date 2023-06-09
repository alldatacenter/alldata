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
package org.apache.drill.exec.physical.impl.common;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.data.JoinCondition;
import org.apache.drill.common.logical.data.NamedExpression;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.config.HashJoinPOP;
import org.apache.drill.exec.physical.impl.MockRecordBatch;
import org.apache.drill.exec.physical.impl.aggregate.SpilledRecordBatch;
import org.apache.drill.exec.physical.impl.join.HashJoinMemoryCalculator;
import org.apache.drill.exec.physical.impl.join.HashJoinMemoryCalculatorImpl;
import org.apache.drill.exec.physical.impl.join.JoinUtils;
import org.apache.drill.exec.physical.impl.spill.SpillSet;
import org.apache.drill.exec.planner.logical.DrillJoinRel;
import org.apache.drill.exec.proto.ExecProtos;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.CloseableRecordBatch;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.test.BaseDirTestWatcher;
import org.apache.drill.test.BaseTest;
import org.apache.drill.test.OperatorFixture;
import org.apache.drill.exec.physical.rowSet.DirectRowSet;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.test.rowSet.RowSetComparison;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

public class HashPartitionTest extends BaseTest {
  @Rule
  public final BaseDirTestWatcher dirTestWatcher = new BaseDirTestWatcher();

  @Test
  public void noSpillBuildSideTest() throws Exception
  {
    new HashPartitionFixture().run(new HashPartitionTestCase() {
      private RowSet buildRowSet;
      private RowSet probeRowSet;

      @Override
      public CloseableRecordBatch createBuildBatch(BatchSchema schema, FragmentContext context) {
        buildRowSet = new RowSetBuilder(context.getAllocator(), schema)
          .addRow(1, "green")
          .addRow(3, "red")
          .addRow(2, "blue")
          .build();
        return new MockRecordBatch.Builder().
          sendData(buildRowSet).
          build(context);
      }

      @Override
      public void createResultBuildBatch(BatchSchema schema, FragmentContext context) {
      }

      @Override
      public CloseableRecordBatch createProbeBatch(BatchSchema schema, FragmentContext context) {
        probeRowSet = new RowSetBuilder(context.getAllocator(), schema)
          .addRow(.5f, "yellow")
          .addRow(1.5f, "blue")
          .addRow(2.5f, "black")
          .build();
        return new MockRecordBatch.Builder().
          sendData(probeRowSet).
          build(context);
      }

      @Override
      public void run(SpillSet spillSet,
                      BatchSchema buildSchema,
                      BatchSchema probeSchema,
                      RecordBatch buildBatch,
                      RecordBatch probeBatch,
                      ChainedHashTable baseHashTable,
                      FragmentContext context,
                      OperatorContext operatorContext) throws Exception {

        final HashPartition hashPartition = new HashPartition(context,
          context.getAllocator(),
          baseHashTable,
          buildBatch,
          probeBatch, false, 10,
          spillSet,
          0,
          0,
          2); // only '1' has a special treatment

        final HashJoinMemoryCalculator.BuildSidePartitioning noopCalc = new HashJoinMemoryCalculatorImpl.NoopBuildSidePartitioningImpl();

        hashPartition.appendInnerRow(buildBatch.getContainer(), 0, 10, noopCalc);
        hashPartition.appendInnerRow(buildBatch.getContainer(), 1, 11, noopCalc);
        hashPartition.appendInnerRow(buildBatch.getContainer(), 2, 12, noopCalc);
        hashPartition.completeAnInnerBatch(false, false);
        hashPartition.buildContainersHashTableAndHelper();

        {
          int compositeIndex = hashPartition.probeForKey(0, 16);
          Assert.assertEquals(-1, compositeIndex);
        }

        {
          int compositeIndex = hashPartition.probeForKey(1, 12);
          int startIndex = hashPartition.getStartIndex(compositeIndex).getLeft();
          int nextIndex = hashPartition.getNextIndex(startIndex);

          Assert.assertEquals(2, startIndex);
          Assert.assertEquals(-1, nextIndex);
        }

        {
          int compositeIndex = hashPartition.probeForKey(2, 15);
          Assert.assertEquals(-1, compositeIndex);
        }

        buildRowSet.clear();
        probeRowSet.clear();
        hashPartition.close();
      }
    });
  }

  @Test
  public void spillSingleIncompleteBatchBuildSideTest() throws Exception
  {
    new HashPartitionFixture().run(new HashPartitionTestCase() {
      private RowSet buildRowSet;
      private RowSet probeRowSet;
      private RowSet actualBuildRowSet;

      @Override
      public CloseableRecordBatch createBuildBatch(BatchSchema schema, FragmentContext context) {
        buildRowSet = new RowSetBuilder(context.getAllocator(), schema)
          .addRow(1, "green")
          .addRow(3, "red")
          .addRow(2, "blue")
          .build();
        return new MockRecordBatch.Builder().
          sendData(buildRowSet).
          build(context);
      }

      @Override
      public void createResultBuildBatch(BatchSchema schema, FragmentContext context) {
        final BatchSchema newSchema = BatchSchema.newBuilder()
          .addFields(schema)
          .addField(MaterializedField.create(HashPartition.HASH_VALUE_COLUMN_NAME, HashPartition.HVtype))
          .build();
        actualBuildRowSet = new RowSetBuilder(context.getAllocator(), newSchema)
          .addRow(1, "green", 10)
          .addRow(3, "red", 11)
          .addRow(2, "blue", 12)
          .build();
      }

      @Override
      public CloseableRecordBatch createProbeBatch(BatchSchema schema, FragmentContext context) {
        probeRowSet = new RowSetBuilder(context.getAllocator(), schema)
          .addRow(.5f, "yellow")
          .addRow(1.5f, "blue")
          .addRow(2.5f, "black")
          .build();
        return new MockRecordBatch.Builder().
          sendData(probeRowSet).
          build(context);
      }

      @Override
      public void run(SpillSet spillSet,
                      BatchSchema buildSchema,
                      BatchSchema probeSchema,
                      RecordBatch buildBatch,
                      RecordBatch probeBatch,
                      ChainedHashTable baseHashTable,
                      FragmentContext context,
                      OperatorContext operatorContext) {

        final HashPartition hashPartition = new HashPartition(context,
          context.getAllocator(),
          baseHashTable,
          buildBatch,
          probeBatch, false, 10,
          spillSet,
          0,
          0,
          2);

        final HashJoinMemoryCalculator.BuildSidePartitioning noopCalc = new HashJoinMemoryCalculatorImpl.NoopBuildSidePartitioningImpl();

        hashPartition.appendInnerRow(buildBatch.getContainer(), 0, 10, noopCalc);
        hashPartition.appendInnerRow(buildBatch.getContainer(), 1, 11, noopCalc);
        hashPartition.appendInnerRow(buildBatch.getContainer(), 2, 12, noopCalc);
        hashPartition.completeAnInnerBatch(false, false);
        hashPartition.spillThisPartition();
        final String spillFile = hashPartition.getSpillFile();
        final int batchesCount = hashPartition.getPartitionBatchesCount();
        hashPartition.closeWriter();

        SpilledRecordBatch spilledBuildBatch = new SpilledRecordBatch(spillFile, batchesCount, context, buildSchema, operatorContext, spillSet);
        final RowSet actual = DirectRowSet.fromContainer(spilledBuildBatch.getContainer());

        new RowSetComparison(actualBuildRowSet).verify(actual);

        spilledBuildBatch.close();
        buildRowSet.clear();
        actualBuildRowSet.clear();
        probeRowSet.clear();
        hashPartition.close();
      }
    });
  }

  public class HashPartitionFixture {
    public void run(HashPartitionTestCase testCase) throws Exception {
      try (OperatorFixture operatorFixture = new OperatorFixture.Builder(HashPartitionTest.this.dirTestWatcher).build()) {

        final FragmentContext context = operatorFixture.getFragmentContext();
        final HashJoinPOP pop = new HashJoinPOP(null, null, null, JoinRelType.FULL, null);
        final OperatorContext operatorContext = operatorFixture.operatorContext(pop);
        final DrillConfig config = context.getConfig();
        final BufferAllocator allocator = operatorFixture.allocator();

        final UserBitShared.QueryId queryId = UserBitShared.QueryId.newBuilder()
          .setPart1(1L)
          .setPart2(2L)
          .build();
        final ExecProtos.FragmentHandle fragmentHandle = ExecProtos.FragmentHandle.newBuilder()
          .setQueryId(queryId)
          .setMinorFragmentId(1)
          .setMajorFragmentId(2)
          .build();

        final SpillSet spillSet = new SpillSet(config, fragmentHandle, pop);

        // Create build batch
        MaterializedField buildColA = MaterializedField.create("buildColA", Types.required(TypeProtos.MinorType.INT));
        MaterializedField buildColB = MaterializedField.create("buildColB", Types.required(TypeProtos.MinorType.VARCHAR));
        List<MaterializedField> buildCols = Lists.newArrayList(buildColA, buildColB);
        final BatchSchema buildSchema = new BatchSchema(BatchSchema.SelectionVectorMode.NONE, buildCols);
        final CloseableRecordBatch buildBatch = testCase.createBuildBatch(buildSchema, operatorContext.getFragmentContext());
        buildBatch.next();
        testCase.createResultBuildBatch(buildSchema, operatorContext.getFragmentContext());

        // Create probe batch
        MaterializedField probeColA = MaterializedField.create("probeColA", Types.required(TypeProtos.MinorType.FLOAT4));
        MaterializedField probeColB = MaterializedField.create("probeColB", Types.required(TypeProtos.MinorType.VARCHAR));
        List<MaterializedField> probeCols = Lists.newArrayList(probeColA, probeColB);
        final BatchSchema probeSchema = new BatchSchema(BatchSchema.SelectionVectorMode.NONE, probeCols);
        final CloseableRecordBatch probeBatch = testCase.createProbeBatch(probeSchema, operatorContext.getFragmentContext());
        probeBatch.next();

        final LogicalExpression buildColExpression = SchemaPath.getSimplePath(buildColB.getName());
        final LogicalExpression probeColExpression = SchemaPath.getSimplePath(probeColB.getName());

        final JoinCondition condition = new JoinCondition(DrillJoinRel.EQUALITY_CONDITION, probeColExpression, buildColExpression);
        final List<Comparator> comparators = Lists.newArrayList(JoinUtils.checkAndReturnSupportedJoinComparator(condition));

        final List<NamedExpression> buildExpressions = Lists.newArrayList(new NamedExpression(buildColExpression, new FieldReference("build_side_0")));
        final List<NamedExpression> probeExpressions = Lists.newArrayList(new NamedExpression(probeColExpression, new FieldReference("probe_side_0")));

        final int hashTableSize = (int) context.getOptions().getOption(ExecConstants.MIN_HASH_TABLE_SIZE);
        final HashTableConfig htConfig = new HashTableConfig(hashTableSize, HashTable.DEFAULT_LOAD_FACTOR, buildExpressions, probeExpressions, comparators);
        final ChainedHashTable baseHashTable = new ChainedHashTable(htConfig, context, allocator, buildBatch, probeBatch, null);
        baseHashTable.updateIncoming(buildBatch, probeBatch);

        testCase.run(spillSet, buildSchema, probeSchema, buildBatch, probeBatch, baseHashTable, context, operatorContext);

        buildBatch.close();
        probeBatch.close();
      }
    }
  }

  interface HashPartitionTestCase {
    CloseableRecordBatch createBuildBatch(BatchSchema schema, FragmentContext context);
    void createResultBuildBatch(BatchSchema schema, FragmentContext context);
    CloseableRecordBatch createProbeBatch(BatchSchema schema, FragmentContext context);

    void run(SpillSet spillSet,
             BatchSchema buildSchema,
             BatchSchema probeSchema,
             RecordBatch buildBatch,
             RecordBatch probeBatch,
             ChainedHashTable baseHashTable,
             FragmentContext context,
             OperatorContext operatorContext) throws Exception;
  }
}
