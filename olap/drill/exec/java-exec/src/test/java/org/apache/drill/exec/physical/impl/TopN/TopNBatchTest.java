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
package org.apache.drill.exec.physical.impl.TopN;

import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.apache.drill.exec.ops.FragmentContextImpl;
import org.apache.drill.exec.proto.BitControl;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.test.BaseDirTestWatcher;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.TestBuilder;
import org.apache.drill.categories.OperatorTest;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.common.logical.data.Order;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.expr.fn.FunctionImplementationRegistry;
import org.apache.drill.exec.memory.RootAllocator;
import org.apache.drill.exec.physical.impl.sort.RecordBatchData;
import org.apache.drill.exec.pop.PopUnitTestBase;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.ExpandableHyperContainer;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.test.ClientFixture;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.exec.physical.rowSet.HyperRowSetImpl;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.test.rowSet.RowSetComparison;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.mockito.Mockito.when;

@Category(OperatorTest.class)
public class TopNBatchTest extends PopUnitTestBase {
  @Rule
  public BaseDirTestWatcher dirTestWatcher = new BaseDirTestWatcher();

  /**
   * Priority queue unit test.
   * @throws Exception
   */
  @Test
  public void priorityQueueOrderingTest() throws Exception {
    Properties properties = new Properties();
    DrillConfig drillConfig = DrillConfig.create(properties);
    DrillbitContext drillbitContext = mockDrillbitContext();
    when(drillbitContext.getFunctionImplementationRegistry()).thenReturn(new FunctionImplementationRegistry(drillConfig));

    FieldReference expr = FieldReference.getWithQuotedRef("colA");
    Order.Ordering ordering = new Order.Ordering(Order.Ordering.ORDER_DESC, expr, Order.Ordering.NULLS_FIRST);
    List<Order.Ordering> orderings = Lists.newArrayList(ordering);

    MaterializedField colA = MaterializedField.create("colA", Types.required(TypeProtos.MinorType.INT));
    MaterializedField colB = MaterializedField.create("colB", Types.required(TypeProtos.MinorType.INT));

    List<MaterializedField> cols = Lists.newArrayList(colA, colB);
    BatchSchema batchSchema = new BatchSchema(BatchSchema.SelectionVectorMode.NONE, cols);
    FragmentContextImpl context = new FragmentContextImpl(drillbitContext,
      BitControl.PlanFragment.getDefaultInstance(), null,
      drillbitContext.getFunctionImplementationRegistry());
    RowSet expectedRowSet;

    try (RootAllocator allocator = new RootAllocator(100_000_000)) {
      expectedRowSet = new RowSetBuilder(allocator, batchSchema)
        .addRow(110, 10)
        .addRow(109, 9)
        .addRow(108, 8)
        .addRow(107, 7)
        .addRow(106, 6)
        .addRow(105, 5)
        .addRow(104, 4)
        .addRow(103, 3)
        .addRow(102, 2)
        .addRow(101, 1)
        .build();

      PriorityQueue queue;
      ExpandableHyperContainer hyperContainer;

      {
        VectorContainer container = new RowSetBuilder(allocator, batchSchema)
          .build()
          .container();
        hyperContainer = new ExpandableHyperContainer(container);

        queue = TopNBatch.createNewPriorityQueue(
          TopNBatch.createMainMappingSet(), TopNBatch.createLeftMappingSet(),
          TopNBatch.createRightMappingSet(),
          orderings, hyperContainer, false,
          true, 10, allocator,
          batchSchema.getSelectionVectorMode(), context);
      }

      List<RecordBatchData> testBatches = Lists.newArrayList();

      try {
        final Random random = new Random();
        final int bound = 100;
        final int numBatches = 11;
        final int numRecordsPerBatch = 100;

        for (int batchCounter = 0; batchCounter < numBatches; batchCounter++) {
          RowSetBuilder rowSetBuilder = new RowSetBuilder(allocator, batchSchema);
          rowSetBuilder.addRow((batchCounter + bound), batchCounter);

          for (int recordCounter = 0; recordCounter < numRecordsPerBatch; recordCounter++) {
            rowSetBuilder.addRow(random.nextInt(bound), random.nextInt(bound));
          }

          VectorContainer vectorContainer = rowSetBuilder.build().container();
          queue.add(new RecordBatchData(vectorContainer, allocator));
        }

        queue.generate();
        VectorContainer resultContainer = queue.getHyperBatch();
        resultContainer.buildSchema(BatchSchema.SelectionVectorMode.NONE);

        RowSet.HyperRowSet actualHyperSet = HyperRowSetImpl.fromContainer(resultContainer, queue.getFinalSv4());
        new RowSetComparison(expectedRowSet).verify(actualHyperSet);
      } finally {
        if (expectedRowSet != null) {
          expectedRowSet.clear();
        }

        queue.cleanup();
        hyperContainer.clear();

        for (RecordBatchData testBatch: testBatches) {
          testBatch.clear();
        }
      }
    }
  }

  /**
   * End to end test of the TopN operator.
   * @throws Throwable
   */
  @Test
  public void sortOneKeyAscending() throws Throwable {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      TestBuilder testBuilder = new TestBuilder(new ClusterFixture.FixtureTestServices(client));
      testBuilder.ordered()
        .physicalPlanFromFile("topN/one_key_sort.json")
        .baselineColumns("blue")
        .go();
    }
  }
}
