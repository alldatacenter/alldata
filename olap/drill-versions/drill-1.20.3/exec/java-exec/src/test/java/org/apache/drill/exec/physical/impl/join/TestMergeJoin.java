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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.drill.categories.OperatorTest;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.util.DrillFileUtils;
import org.apache.drill.exec.client.DrillClient;
import org.apache.drill.exec.expr.fn.FunctionImplementationRegistry;
import org.apache.drill.exec.ops.FragmentContextImpl;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.physical.base.FragmentRoot;
import org.apache.drill.exec.physical.impl.ImplCreator;
import org.apache.drill.exec.physical.impl.SimpleRootExec;
import org.apache.drill.exec.planner.PhysicalPlanReader;
import org.apache.drill.exec.planner.PhysicalPlanReaderTestFactory;
import org.apache.drill.exec.pop.PopUnitTestBase;
import org.apache.drill.exec.proto.BitControl.PlanFragment;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.apache.drill.exec.rpc.UserClientConnection;
import org.apache.drill.exec.server.Drillbit;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.server.RemoteServiceSet;
import org.apache.drill.exec.store.StoragePluginRegistryImpl;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.categories.SlowTest;
import org.junit.Ignore;
import org.junit.Test;

import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.io.Files;

import org.junit.experimental.categories.Category;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Category({SlowTest.class, OperatorTest.class})
public class TestMergeJoin extends PopUnitTestBase {

  private static final Logger logger = LoggerFactory.getLogger(TestMergeJoin.class);
  private final DrillConfig c = DrillConfig.create();

  @Test
  @Ignore // this doesn't have a sort.  it also causes an infinite loop.  these may or may not be related.
  public void simpleEqualityJoin() throws Throwable {
    final DrillbitContext bitContext = mockDrillbitContext();
    final UserClientConnection connection = Mockito.mock(UserClientConnection.class);

    final PhysicalPlanReader reader = PhysicalPlanReaderTestFactory.defaultPhysicalPlanReader(c);
    final PhysicalPlan plan = reader.readPhysicalPlan(Files.asCharSource(DrillFileUtils.getResourceAsFile("/join/merge_join.json"), Charsets.UTF_8).read());
    final FunctionImplementationRegistry registry = new FunctionImplementationRegistry(c);
    final FragmentContextImpl context = new FragmentContextImpl(bitContext, PlanFragment.getDefaultInstance(), connection, registry);
    final SimpleRootExec exec = new SimpleRootExec(ImplCreator.getExec(context, (FragmentRoot) plan.getSortedOperators(false).iterator().next()));

    int totalRecordCount = 0;
    final StringBuilder sb = new StringBuilder();
    while (exec.next()) {
      totalRecordCount += exec.getRecordCount();
      for (final ValueVector v : exec) {
        sb.append("[" + v.getField().getName() + "]        ");
      }
      sb.append("\n\n");
      for (int valueIdx = 0; valueIdx < exec.getRecordCount(); valueIdx++) {
        final List<Object> row = new ArrayList<>();
        for (final ValueVector v : exec) {
           row.add(v.getAccessor().getObject(valueIdx));
        }
        for (final Object cell : row) {
          if (cell == null) {
            sb.append("<null>          ");
            continue;
          }
          final int len = cell.toString().length();
          sb.append(cell);
          for (int i = 0; i < (14 - len); ++i) {
            sb.append(" ");
          }
        }
        sb.append("\n");
      }
      sb.append("\n");
    }

    logger.info(sb.toString());
    assertEquals(100, totalRecordCount);
    if (context.getExecutorState().getFailureCause() != null) {
      throw context.getExecutorState().getFailureCause();
    }
    assertTrue(!context.getExecutorState().isFailed());
  }

  @Test
  @Ignore
  public void orderedEqualityLeftJoin() throws Throwable {
    final DrillbitContext bitContext = mockDrillbitContext();
    final UserClientConnection connection = Mockito.mock(UserClientConnection.class);

    final PhysicalPlanReader reader = PhysicalPlanReaderTestFactory.defaultPhysicalPlanReader(c,
        new StoragePluginRegistryImpl(bitContext));
    final PhysicalPlan plan = reader.readPhysicalPlan(
        Files.asCharSource(
            DrillFileUtils.getResourceAsFile("/join/merge_single_batch.json"), Charsets.UTF_8).read()
            .replace("#{LEFT_FILE}", DrillFileUtils.getResourceAsFile("/join/merge_single_batch.left.json").toURI().toString())
            .replace("#{RIGHT_FILE}", DrillFileUtils.getResourceAsFile("/join/merge_single_batch.right.json").toURI().toString()));
    final FunctionImplementationRegistry registry = new FunctionImplementationRegistry(c);
    final FragmentContextImpl context = new FragmentContextImpl(bitContext, PlanFragment.getDefaultInstance(), connection, registry);
    final SimpleRootExec exec = new SimpleRootExec(ImplCreator.getExec(context, (FragmentRoot) plan.getSortedOperators(false).iterator().next()));

    int totalRecordCount = 0;
    final StringBuilder sb = new StringBuilder();

    while (exec.next()) {
      totalRecordCount += exec.getRecordCount();
      sb.append(String.format("got next with record count: %d (total: %d):\n", exec.getRecordCount(), totalRecordCount));
      sb.append("       t1                 t2\n");

      for (int valueIdx = 0; valueIdx < exec.getRecordCount(); valueIdx++) {
        final List<Object> row = Lists.newArrayList();
        for (final ValueVector v : exec) {
          row.add(v.getField().getName() + ":" + v.getAccessor().getObject(valueIdx));
        }
        for (final Object cell : row) {
          if (cell == null) {
            sb.append("<null>    ");
            continue;
          }
          final int len = cell.toString().length();
          sb.append(cell + " ");
          for (int i = 0; i < (10 - len); ++i) {
            sb.append(" ");
          }
        }
        sb.append('\n');
      }
    }

    logger.info(sb.toString());
    assertEquals(25, totalRecordCount);

    if (context.getExecutorState().getFailureCause() != null) {
      throw context.getExecutorState().getFailureCause();
    }
    assertTrue(!context.getExecutorState().isFailed());
  }

  @Test
  @Ignore
  public void orderedEqualityInnerJoin() throws Throwable {
    final DrillbitContext bitContext = mockDrillbitContext();
    final UserClientConnection connection = Mockito.mock(UserClientConnection.class);

    final PhysicalPlanReader reader = PhysicalPlanReaderTestFactory.defaultPhysicalPlanReader(c,
        new StoragePluginRegistryImpl(bitContext));
    final PhysicalPlan plan = reader.readPhysicalPlan(
        Files.asCharSource(
            DrillFileUtils.getResourceAsFile("/join/merge_inner_single_batch.json"), Charsets.UTF_8).read()
            .replace("#{LEFT_FILE}", DrillFileUtils.getResourceAsFile("/join/merge_single_batch.left.json").toURI().toString())
            .replace("#{RIGHT_FILE}", DrillFileUtils.getResourceAsFile("/join/merge_single_batch.right.json").toURI().toString()));
    final FunctionImplementationRegistry registry = new FunctionImplementationRegistry(c);
    final FragmentContextImpl context = new FragmentContextImpl(bitContext, PlanFragment.getDefaultInstance(), connection, registry);
    final SimpleRootExec exec = new SimpleRootExec(ImplCreator.getExec(context, (FragmentRoot) plan.getSortedOperators(false).iterator().next()));

    int totalRecordCount = 0;
    final StringBuilder sb = new StringBuilder();

    while (exec.next()) {
      totalRecordCount += exec.getRecordCount();
      sb.append(String.format("got next with record count: %d (total: %d):\n", exec.getRecordCount(), totalRecordCount));
      sb.append("       t1                 t2\n");

      for (int valueIdx = 0; valueIdx < exec.getRecordCount(); valueIdx++) {
        final List<Object> row = Lists.newArrayList();
        for (final ValueVector v : exec) {
          row.add(v.getField().getName() + ":" + v.getAccessor().getObject(valueIdx));
        }
        for (final Object cell : row) {
          if (cell == null) {
            sb.append("<null>    ");
            continue;
          }
          final int len = cell.toString().length();
          sb.append(cell + " ");
          for (int i = 0; i < (10 - len); ++i) {
            sb.append(" ");
          }
        }
        sb.append('\n');
      }
    }
    assertEquals(23, totalRecordCount);

    if (context.getExecutorState().getFailureCause() != null) {
      throw context.getExecutorState().getFailureCause();
    }
    assertTrue(!context.getExecutorState().isFailed());
  }

  @Test
  @Ignore
  public void orderedEqualityMultiBatchJoin() throws Throwable {
    final DrillbitContext bitContext = mockDrillbitContext();
    final UserClientConnection connection = Mockito.mock(UserClientConnection.class);

    final PhysicalPlanReader reader = PhysicalPlanReaderTestFactory.defaultPhysicalPlanReader(c,
        new StoragePluginRegistryImpl(bitContext));
    final PhysicalPlan plan = reader.readPhysicalPlan(
        Files.asCharSource(
            DrillFileUtils.getResourceAsFile("/join/merge_multi_batch.json"), Charsets.UTF_8).read()
            .replace("#{LEFT_FILE}", DrillFileUtils.getResourceAsFile("/join/merge_multi_batch.left.json").toURI().toString())
            .replace("#{RIGHT_FILE}", DrillFileUtils.getResourceAsFile("/join/merge_multi_batch.right.json").toURI().toString()));
    final FunctionImplementationRegistry registry = new FunctionImplementationRegistry(c);
    final FragmentContextImpl context = new FragmentContextImpl(bitContext, PlanFragment.getDefaultInstance(), connection, registry);
    final SimpleRootExec exec = new SimpleRootExec(ImplCreator.getExec(context, (FragmentRoot) plan.getSortedOperators(false).iterator().next()));

    int totalRecordCount = 0;
    final StringBuilder sb = new StringBuilder();

    while (exec.next()) {
      totalRecordCount += exec.getRecordCount();
      sb.append(String.format("got next with record count: %d (total: %d):\n", exec.getRecordCount(), totalRecordCount));

      for (int valueIdx = 0; valueIdx < exec.getRecordCount(); valueIdx++) {
        final List<Object> row = Lists.newArrayList();
        for (final ValueVector v : exec) {
          row.add(v.getField().getName() + ":" + v.getAccessor().getObject(valueIdx));
        }
        for (final Object cell : row) {
          if (cell == null) {
            sb.append("<null>    ");
            continue;
          }
          int len = cell.toString().length();
          sb.append(cell + " ");
          for (int i = 0; i < (10 - len); ++i) {
            sb.append(" ");
          }
        }
        sb.append('\n');
      }
    }
    assertEquals(25, totalRecordCount);

    if (context.getExecutorState().getFailureCause() != null) {
      throw context.getExecutorState().getFailureCause();
    }
    assertTrue(!context.getExecutorState().isFailed());
  }

  @Test
  public void testJoinBatchSize() throws Throwable {
    final DrillbitContext bitContext = mockDrillbitContext();
    final UserClientConnection connection = Mockito.mock(UserClientConnection.class);

    final PhysicalPlanReader reader = PhysicalPlanReaderTestFactory.defaultPhysicalPlanReader(c);
    final PhysicalPlan plan = reader.readPhysicalPlan(Files.asCharSource(DrillFileUtils.getResourceAsFile("/join/join_batchsize.json"), Charsets.UTF_8).read());
    final FunctionImplementationRegistry registry = new FunctionImplementationRegistry(c);
    final FragmentContextImpl context = new FragmentContextImpl(bitContext, PlanFragment.getDefaultInstance(), connection, registry);
    final SimpleRootExec exec = new SimpleRootExec(ImplCreator.getExec(context, (FragmentRoot) plan.getSortedOperators(false).iterator().next()));
    exec.next(); // skip schema batch
    while (exec.next()) {
      assertEquals(100, exec.getRecordCount());
    }

    if (context.getExecutorState().getFailureCause() != null) {
      throw context.getExecutorState().getFailureCause();
    }
    assertTrue(!context.getExecutorState().isFailed());
  }

  @Test
  public void testMergeJoinInnerEmptyBatch() throws Exception {
    final RemoteServiceSet serviceSet = RemoteServiceSet.getLocalServiceSet();

    try (final Drillbit bit1 = new Drillbit(CONFIG, serviceSet);
        final DrillClient client = new DrillClient(CONFIG, serviceSet.getCoordinator());) {

      bit1.run();
      client.connect();
      final List<QueryDataBatch> results = client.runQuery(org.apache.drill.exec.proto.UserBitShared.QueryType.PHYSICAL,
              Files.asCharSource(DrillFileUtils.getResourceAsFile("/join/merge_join_empty_batch.json"),
                      Charsets.UTF_8).read()
                      .replace("${JOIN_TYPE}", "INNER"));
      int count = 0;
      for (final QueryDataBatch b : results) {
        if (b.getHeader().getRowCount() != 0) {
          count += b.getHeader().getRowCount();
        }
        b.release();
      }
      assertEquals(0, count);
    }
  }

  @Test
  public void testMergeJoinLeftEmptyBatch() throws Exception {
    final RemoteServiceSet serviceSet = RemoteServiceSet.getLocalServiceSet();

    try (final Drillbit bit1 = new Drillbit(CONFIG, serviceSet);
        final DrillClient client = new DrillClient(CONFIG, serviceSet.getCoordinator());) {

      bit1.run();
      client.connect();
      final List<QueryDataBatch> results = client.runQuery(org.apache.drill.exec.proto.UserBitShared.QueryType.PHYSICAL,
          Files.asCharSource(DrillFileUtils.getResourceAsFile("/join/merge_join_empty_batch.json"),
              Charsets.UTF_8).read()
              .replace("${JOIN_TYPE}", "LEFT"));
      int count = 0;
      for (final QueryDataBatch b : results) {
        if (b.getHeader().getRowCount() != 0) {
          count += b.getHeader().getRowCount();
        }
        b.release();
      }
      assertEquals(50, count);
    }
  }

  @Test
  public void testMergeJoinRightEmptyBatch() throws Exception {
    final RemoteServiceSet serviceSet = RemoteServiceSet.getLocalServiceSet();

    try (final Drillbit bit1 = new Drillbit(CONFIG, serviceSet);
        final DrillClient client = new DrillClient(CONFIG, serviceSet.getCoordinator());) {

      bit1.run();
      client.connect();
      final List<QueryDataBatch> results = client.runQuery(org.apache.drill.exec.proto.UserBitShared.QueryType.PHYSICAL,
              Files.asCharSource(DrillFileUtils.getResourceAsFile("/join/merge_join_empty_batch.json"),
                      Charsets.UTF_8).read()
                      .replace("${JOIN_TYPE}", "RIGHT"));
      int count = 0;
      for (final QueryDataBatch b : results) {
        if (b.getHeader().getRowCount() != 0) {
          count += b.getHeader().getRowCount();
        }
        b.release();
      }
      assertEquals(0, count);
    }
  }

  @Test
  public void testMergeJoinExprInCondition() throws Exception {
    final RemoteServiceSet serviceSet = RemoteServiceSet.getLocalServiceSet();

    try (final Drillbit bit1 = new Drillbit(CONFIG, serviceSet);
        final DrillClient client = new DrillClient(CONFIG, serviceSet.getCoordinator());) {

      bit1.run();
      client.connect();
      final List<QueryDataBatch> results = client.runQuery(org.apache.drill.exec.proto.UserBitShared.QueryType.PHYSICAL,
              Files.asCharSource(DrillFileUtils.getResourceAsFile("/join/mergeJoinExpr.json"), Charsets.UTF_8).read());
      int count = 0;
      for (final QueryDataBatch b : results) {
        if (b.getHeader().getRowCount() != 0) {
          count += b.getHeader().getRowCount();
        }
        b.release();
      }
      assertEquals(10, count);
    }
  }
}
