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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.drill.common.expression.ExpressionPosition;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.test.TestTools;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.record.RecordBatchLoader;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.apache.drill.exec.vector.BigIntVector;
import org.apache.drill.test.BaseDirTestWatcher;
import org.apache.drill.test.ClientFixture;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.DrillTest;
import org.apache.drill.categories.SlowTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestRule;

@Category({SlowTest.class})
public class TestSimpleExternalSort extends DrillTest {
  @Rule
  public final TestRule TIMEOUT = TestTools.getTimeoutRule(160_000);

  @Rule
  public final BaseDirTestWatcher dirTestWatcher = new BaseDirTestWatcher();

  /**
   * Tests the external sort using an in-memory sort. Relies on default memory
   * settings to be large enough to do the in-memory sort (there is,
   * unfortunately, no way to double-check that no spilling was done.)
   * This must be checked manually by setting a breakpoint in the in-memory
   * sort routine.
   *
   * @param testLegacy
   * @throws Exception
   */

  @Test
  public void mergeSortWithSv2() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      List<QueryDataBatch> results = client.queryBuilder().physicalResource("xsort/one_key_sort_descending_sv2.json").results();
      assertEquals(500_000, client.countResults(results));
      validateResults(client.allocator(), results);
    }
  }

  @Test
  public void sortOneKeyDescendingMergeSort() throws Throwable {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      List<QueryDataBatch> results = client.queryBuilder().physicalResource("xsort/one_key_sort_descending.json").results();
      assertEquals(1_000_000, client.countResults(results));
      validateResults(client.allocator(), results);
    }
  }

  private void validateResults(BufferAllocator allocator, List<QueryDataBatch> results) throws SchemaChangeException {
    long previousBigInt = Long.MAX_VALUE;

    for (QueryDataBatch b : results) {
      RecordBatchLoader loader = new RecordBatchLoader(allocator);
      if (b.getHeader().getRowCount() > 0) {
        loader.load(b.getHeader().getDef(),b.getData());
        @SuppressWarnings("deprecation")
        BigIntVector c1 = (BigIntVector) loader.getValueAccessorById(BigIntVector.class, loader.getValueVectorId(new SchemaPath("blue", ExpressionPosition.UNKNOWN)).getFieldIds()).getValueVector();
        BigIntVector.Accessor a1 = c1.getAccessor();

        for (int i = 0; i < c1.getAccessor().getValueCount(); i++) {
          assertTrue(String.format("%d > %d", previousBigInt, a1.get(i)), previousBigInt >= a1.get(i));
          previousBigInt = a1.get(i);
        }
      }
      loader.clear();
      b.release();
    }
  }

  @Test
  public void sortOneKeyDescendingExternalSort() throws Throwable {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
        .configProperty(ExecConstants.EXTERNAL_SORT_SPILL_THRESHOLD, 4)
        .configProperty(ExecConstants.EXTERNAL_SORT_SPILL_GROUP_SIZE, 4)
        .configProperty(ExecConstants.EXTERNAL_SORT_BATCH_LIMIT, 4);
    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      List<QueryDataBatch> results = client.queryBuilder().physicalResource("/xsort/one_key_sort_descending.json").results();
      assertEquals(1_000_000, client.countResults(results));
      validateResults(client.allocator(), results);
    }
  }

  @Test
  public void outOfMemoryExternalSort() throws Throwable{
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
        // Probably do nothing in modern Drill
        .configProperty("drill.memory.fragment.max", 50_000_000)
        .configProperty("drill.memory.fragment.initial", 2_000_000)
        .configProperty("drill.memory.operator.max", 30_000_000)
        .configProperty("drill.memory.operator.initial", 2_000_000);
    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      List<QueryDataBatch> results = client.queryBuilder().physicalResource("/xsort/oom_sort_test.json").results();
      assertEquals(10_000_000, client.countResults(results));

      long previousBigInt = Long.MAX_VALUE;

      for (QueryDataBatch b : results) {
        RecordBatchLoader loader = new RecordBatchLoader(client.allocator());
        if (b.getHeader().getRowCount() > 0) {
          loader.load(b.getHeader().getDef(),b.getData());
          BigIntVector c1 = (BigIntVector) loader.getValueAccessorById(BigIntVector.class, loader.getValueVectorId(new SchemaPath("blue", ExpressionPosition.UNKNOWN)).getFieldIds()).getValueVector();
          BigIntVector.Accessor a1 = c1.getAccessor();

          for (int i = 0; i < c1.getAccessor().getValueCount(); i++) {
            assertTrue(String.format("%d < %d", previousBigInt, a1.get(i)), previousBigInt >= a1.get(i));
            previousBigInt = a1.get(i);
          }
          assertTrue(String.format("%d == %d", a1.get(0), a1.get(a1.getValueCount() - 1)), a1.get(0) != a1.get(a1.getValueCount() - 1));
        }
        loader.clear();
        b.release();
      }
    }
  }
}
