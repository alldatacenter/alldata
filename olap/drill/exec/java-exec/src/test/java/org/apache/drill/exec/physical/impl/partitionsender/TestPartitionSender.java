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
package org.apache.drill.exec.physical.impl.partitionsender;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import org.apache.drill.categories.OperatorTest;
import org.apache.drill.PlanTestBase;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.expr.fn.FunctionImplementationRegistry;
import org.apache.drill.exec.ops.FragmentContextImpl;
import org.apache.drill.exec.ops.OperatorStats;
import org.apache.drill.exec.physical.MinorFragmentEndpoint;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.PhysicalOperatorUtil;
import org.apache.drill.exec.physical.config.HashPartitionSender;
import org.apache.drill.exec.physical.config.HashToRandomExchange;
import org.apache.drill.exec.physical.impl.TopN.TopNBatch;
import org.apache.drill.exec.physical.impl.partitionsender.PartitionSenderRootExec.Metric;
import org.apache.drill.exec.physical.impl.partitionsender.PartitionerDecorator.GeneralExecuteIface;
import org.apache.drill.exec.planner.PhysicalPlanReader;
import org.apache.drill.exec.planner.cost.PrelCostEstimates;
import org.apache.drill.exec.planner.fragment.DefaultQueryParallelizer;
import org.apache.drill.exec.planner.fragment.Fragment;
import org.apache.drill.exec.planner.fragment.PlanningSet;
import org.apache.drill.exec.planner.fragment.SimpleParallelizer;
import org.apache.drill.exec.pop.PopUnitTestBase;
import org.apache.drill.exec.proto.BitControl.PlanFragment;
import org.apache.drill.exec.proto.BitControl.QueryContextInformation;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.proto.UserBitShared.MetricValue;
import org.apache.drill.exec.proto.UserBitShared.OperatorProfile;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.selection.SelectionVector4;
import org.apache.drill.exec.rpc.user.UserSession;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.server.options.OptionList;
import org.apache.drill.exec.server.options.OptionValue;
import org.apache.drill.exec.server.options.OptionValue.AccessibleScopes;
import org.apache.drill.exec.server.options.OptionValue.OptionScope;
import org.apache.drill.exec.util.Utilities;
import org.apache.drill.exec.work.QueryWorkUnit;
import org.apache.drill.test.OperatorFixture.MockExecutorState;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

/**
 * PartitionerSenderRootExec test to cover mostly part that deals with multithreaded
 * ability to copy and flush data
 *
 */
@Category(OperatorTest.class)
public class TestPartitionSender extends PlanTestBase {

  private static final SimpleParallelizer PARALLELIZER = new DefaultQueryParallelizer(
      true,
      1 /*parallelizationThreshold (slice_count)*/,
      6 /*maxWidthPerNode*/,
      1000 /*maxGlobalWidth*/,
      1.2 /*affinityFactor*/);

  private final static UserSession USER_SESSION = UserSession.Builder.newBuilder()
      .withCredentials(UserBitShared.UserCredentials.newBuilder().setUserName("foo").build())
      .build();

  private static final int NUM_DEPTS = 40;
  private static final int NUM_EMPLOYEES = 1000;
  private static final int DRILLBITS_COUNT = 3;
  private static final String TABLE = "table";

  private static String groupByQuery;

  @BeforeClass
  public static void generateTestDataAndQueries() throws Exception {
    // Table consists of two columns "emp_id", "emp_name" and "dept_id"
    final File empTableLocation = dirTestWatcher.makeRootSubDir(Paths.get(TABLE));

    // Write 100 records for each new file
    final int empNumRecsPerFile = 100;
    for(int fileIndex=0; fileIndex<NUM_EMPLOYEES/empNumRecsPerFile; fileIndex++) {
      File file = new File(empTableLocation, fileIndex + ".json");
      PrintWriter printWriter = new PrintWriter(file);
      for (int recordIndex = fileIndex*empNumRecsPerFile; recordIndex < (fileIndex+1)*empNumRecsPerFile; recordIndex++) {
        String record = String.format("{ \"emp_id\" : %d, \"emp_name\" : \"Employee %d\", \"dept_id\" : %d }",
            recordIndex, recordIndex, recordIndex % NUM_DEPTS);
        printWriter.println(record);
      }
      printWriter.close();
    }

    // Initialize test queries
    groupByQuery = String.format("SELECT dept_id, count(*) as numEmployees FROM dfs.`%s` GROUP BY dept_id", TABLE);
  }

  @Test
  /**
   * Main test to go over different scenarios
   * @throws Exception
   */
  public void testPartitionSenderCostToThreads() throws Exception {

    final VectorContainer container = new VectorContainer();
    container.buildSchema(SelectionVectorMode.FOUR_BYTE);
    final SelectionVector4 sv = Mockito.mock(SelectionVector4.class, "SelectionVector4");
    Mockito.when(sv.getCount()).thenReturn(100);
    Mockito.when(sv.getTotalCount()).thenReturn(100);
    for (int i = 0; i < 100; i++) {
      Mockito.when(sv.get(i)).thenReturn(i);
    }

    final TopNBatch.SimpleSV4RecordBatch incoming = new TopNBatch.SimpleSV4RecordBatch(container, sv, null);

    updateTestCluster(DRILLBITS_COUNT, null);

    test("ALTER SESSION SET `planner.slice_target`=1");
    String plan = getPlanInString("EXPLAIN PLAN FOR " + groupByQuery, JSON_FORMAT);

    final DrillbitContext drillbitContext = getDrillbitContext();
    final PhysicalPlanReader planReader = drillbitContext.getPlanReader();
    final PhysicalPlan physicalPlan = planReader.readPhysicalPlan(plan);
    final Fragment rootFragment = PopUnitTestBase.getRootFragmentFromPlanString(planReader, plan);
    final PlanningSet planningSet = new PlanningSet();
    final FunctionImplementationRegistry registry = new FunctionImplementationRegistry(config);

    // Create a planningSet to get the assignment of major fragment ids to fragments.
    PARALLELIZER.initFragmentWrappers(rootFragment, planningSet);

    final List<PhysicalOperator> operators = physicalPlan.getSortedOperators(false);

    // get HashToRandomExchange physical operator
    HashToRandomExchange hashToRandomExchange = null;
    for (PhysicalOperator operator : operators) {
      if (operator instanceof HashToRandomExchange) {
        hashToRandomExchange = (HashToRandomExchange) operator;
        break;
      }
    }

    final OptionList options = new OptionList();
    // try multiple scenarios with different set of options
    options.add(OptionValue.create(OptionValue.AccessibleScopes.SESSION, "planner.slice_target", 1, OptionScope.SESSION));
    testThreadsHelper(hashToRandomExchange, drillbitContext, options,
        incoming, registry, planReader, planningSet, rootFragment, 1);

    options.clear();
    options.add(OptionValue.create(AccessibleScopes.SESSION, "planner.slice_target", 1, OptionScope.SESSION));
    options.add(OptionValue.create(OptionValue.AccessibleScopes.SESSION, "planner.partitioner_sender_max_threads", 10, OptionScope.SESSION));
    hashToRandomExchange.setCost(new PrelCostEstimates(1000, 1000));
    testThreadsHelper(hashToRandomExchange, drillbitContext, options,
        incoming, registry, planReader, planningSet, rootFragment, 10);

    options.clear();
    options.add(OptionValue.create(AccessibleScopes.SESSION, "planner.slice_target", 1000, OptionScope.SESSION));
    options.add(OptionValue.create(AccessibleScopes.SESSION, "planner.partitioner_sender_threads_factor",2, OptionScope.SESSION));
    hashToRandomExchange.setCost(new PrelCostEstimates(14000, 14000));
    testThreadsHelper(hashToRandomExchange, drillbitContext, options,
        incoming, registry, planReader, planningSet, rootFragment, 2);
  }

  /**
   * Core of the testing
   * @param hashToRandomExchange
   * @param drillbitContext
   * @param options
   * @param incoming
   * @param registry
   * @param planReader
   * @param planningSet
   * @param rootFragment
   * @param expectedThreadsCount
   * @throws Exception
   */
  private void testThreadsHelper(HashToRandomExchange hashToRandomExchange, DrillbitContext drillbitContext, OptionList options,
      RecordBatch incoming, FunctionImplementationRegistry registry, PhysicalPlanReader planReader, PlanningSet planningSet, Fragment rootFragment,
      int expectedThreadsCount) throws Exception {

    final QueryContextInformation queryContextInfo = Utilities.createQueryContextInfo("dummySchemaName", "938ea2d9-7cb9-4baf-9414-a5a0b7777e8e");
    final QueryWorkUnit qwu = PARALLELIZER.generateWorkUnit(options, drillbitContext.getEndpoint(),
        QueryId.getDefaultInstance(),
        drillbitContext.getBits(), rootFragment, USER_SESSION, queryContextInfo);
    qwu.applyPlan(planReader);

    final List<MinorFragmentEndpoint> mfEndPoints = PhysicalOperatorUtil.getIndexOrderedEndpoints(Lists.newArrayList(drillbitContext.getBits()));

    for(PlanFragment planFragment : qwu.getFragments()) {
      if (!planFragment.getFragmentJson().contains("hash-partition-sender")) {
        continue;
      }
      MockPartitionSenderRootExec partionSenderRootExec = null;
      FragmentContextImpl context = null;
      try {
        context = new FragmentContextImpl(drillbitContext, planFragment, null, registry);
        context.setExecutorState(new MockExecutorState());
        final int majorFragmentId = planFragment.getHandle().getMajorFragmentId();
        final HashPartitionSender partSender = new HashPartitionSender(majorFragmentId, hashToRandomExchange, hashToRandomExchange.getExpression(), mfEndPoints);
        partionSenderRootExec = new MockPartitionSenderRootExec(context, incoming, partSender);
        assertEquals("Number of threads calculated", expectedThreadsCount, partionSenderRootExec.getNumberPartitions());

        partionSenderRootExec.createPartitioner();
        final PartitionerDecorator partDecor = partionSenderRootExec.getPartitioner();
        assertNotNull(partDecor);

        List<Partitioner> partitioners = partDecor.getPartitioners();
        assertNotNull(partitioners);
        final int actualThreads = DRILLBITS_COUNT > expectedThreadsCount ? expectedThreadsCount : DRILLBITS_COUNT;
        assertEquals("Number of partitioners", actualThreads, partitioners.size());

        for (int i = 0; i < mfEndPoints.size(); i++) {
          assertNotNull("PartitionOutgoingBatch", partDecor.getOutgoingBatches(i));
        }

        // check distribution of PartitionOutgoingBatch - should be even distribution
        boolean isFirst = true;
        int prevBatchCountSize = 0;
        int batchCountSize = 0;
        for (Partitioner part : partitioners) {
          @SuppressWarnings("unchecked")
          final List<PartitionOutgoingBatch> outBatch = (List<PartitionOutgoingBatch>) part.getOutgoingBatches();
          batchCountSize = outBatch.size();
          if (!isFirst) {
            assertTrue(Math.abs(batchCountSize - prevBatchCountSize) <= 1);
          } else {
            isFirst = false;
          }
          prevBatchCountSize = batchCountSize;
        }

        partionSenderRootExec.getStats().startProcessing();
        try {
          partDecor.partitionBatch(incoming);
        } finally {
          partionSenderRootExec.getStats().stopProcessing();
        }
        if (actualThreads == 1) {
          assertEquals("With single thread parent and child waitNanos should match", partitioners.get(0).getStats().getWaitNanos(), partionSenderRootExec.getStats().getWaitNanos());
        }

        // testing values distribution
        partitioners = partDecor.getPartitioners();
        isFirst = true;
        // since we have fake Nullvector distribution is skewed
        for (Partitioner part : partitioners) {
          @SuppressWarnings("unchecked")
          final List<PartitionOutgoingBatch> outBatches = (List<PartitionOutgoingBatch>) part.getOutgoingBatches();
          for (PartitionOutgoingBatch partOutBatch : outBatches) {
            final int recordCount = ((VectorAccessible) partOutBatch).getRecordCount();
            if (isFirst) {
              assertEquals("RecordCount", 100, recordCount);
              isFirst = false;
            } else {
              assertEquals("RecordCount", 0, recordCount);
            }
          }
        }
        // test exceptions within threads
        // test stats merging
        partionSenderRootExec.getStats().startProcessing();
        try {
          partDecor.executeMethodLogic(new InjectExceptionTest());
          fail("executeMethodLogic should throw an exception.");
        } catch (ExecutionException e) {
          final OperatorProfile.Builder oPBuilder = OperatorProfile.newBuilder();
          partionSenderRootExec.getStats().addAllMetrics(oPBuilder);
          final List<MetricValue> metrics = oPBuilder.getMetricList();
          for (MetricValue metric : metrics) {
            if (Metric.BYTES_SENT.metricId() == metric.getMetricId()) {
              assertEquals("Should add metricValue irrespective of exception", 5*actualThreads, metric.getLongValue());
            }
            if (Metric.SENDING_THREADS_COUNT.metricId() == metric.getMetricId()) {
              assertEquals(actualThreads, metric.getLongValue());
            }
          }
          assertTrue(e.getCause() instanceof IOException);
          assertEquals(actualThreads-1, e.getCause().getSuppressed().length);
        } finally {
          partionSenderRootExec.getStats().stopProcessing();
        }
      } finally {
        // cleanup
        partionSenderRootExec.close();
        context.close();
      }
    }
  }

  @Test
  /**
   * Testing partitioners distribution algorithm
   * @throws Exception
   */
  public void testAlgorithm() throws Exception {
    int outGoingBatchCount;
    int numberPartitions;
    int k = 0;
    final Random rand = new Random();
    while (k < 1000) {
      outGoingBatchCount = rand.nextInt(1000)+1;
      numberPartitions = rand.nextInt(32)+1;
      final int actualPartitions = outGoingBatchCount > numberPartitions ? numberPartitions : outGoingBatchCount;
      final int divisor = Math.max(1, outGoingBatchCount/actualPartitions);

      final int longTail = outGoingBatchCount % actualPartitions;
      int startIndex = 0;
      int endIndex = 0;
      for (int i = 0; i < actualPartitions; i++) {
        startIndex = endIndex;
        endIndex = startIndex + divisor;
        if (i < longTail) {
          endIndex++;
        }
      }
      assertTrue("endIndex can not be > outGoingBatchCount", endIndex == outGoingBatchCount);
      k++;
    }
  }

  /**
   * Helper class to expose some functionality of PartitionSenderRootExec
   *
   */
  private static class MockPartitionSenderRootExec extends PartitionSenderRootExec {

    public MockPartitionSenderRootExec(FragmentContextImpl context,
        RecordBatch incoming, HashPartitionSender operator)
        throws OutOfMemoryException {
      super(context, incoming, operator);
    }

    @Override
    public void close() {
      // Don't close the context here; it is closed
      // separately. Close only resources this sender
      // controls.
//      ((AutoCloseable) oContext).close();
    }

    public int getNumberPartitions() {
      return numberPartitions;
    }

    public OperatorStats getStats() {
      return this.stats;
    }
  }

  /**
   * Helper class to inject exceptions in the threads
   *
   */
  private static class InjectExceptionTest implements GeneralExecuteIface {

    @Override
    public void execute(Partitioner partitioner) throws IOException {
      // throws IOException
      partitioner.getStats().addLongStat(Metric.BYTES_SENT, 5);
      throw new IOException("Test exception handling");
    }
  }
}
