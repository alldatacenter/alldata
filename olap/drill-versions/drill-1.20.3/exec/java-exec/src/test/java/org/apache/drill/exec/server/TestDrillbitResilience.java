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
package org.apache.drill.exec.server;

import static org.apache.drill.exec.ExecConstants.SLICE_TARGET;
import static org.apache.drill.exec.planner.physical.PlannerSettings.ENABLE_HASH_AGG_OPTION;
import static org.apache.drill.exec.planner.physical.PlannerSettings.PARTITION_SENDER_SET_THREADS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

import ch.qos.logback.classic.Level;
import org.apache.commons.math3.util.Pair;
import org.apache.drill.categories.FlakyTest;
import org.apache.drill.exec.client.DrillClient;
import org.apache.drill.exec.physical.impl.partitionsender.PartitionSenderRootExec;
import org.apache.drill.exec.testing.ExecutionControlsInjector;
import org.apache.drill.exec.work.WorkManager;
import org.apache.drill.exec.work.foreman.Foreman;
import org.apache.drill.exec.work.foreman.ForemanException;
import org.apache.drill.exec.work.foreman.ForemanSetupException;
import org.apache.drill.exec.work.foreman.FragmentsRunner;
import org.apache.drill.exec.work.foreman.QueryStateProcessor;
import org.apache.drill.test.BaseTestQuery;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.test.LogFixture;
import org.apache.drill.test.QueryTestUtil;
import org.apache.drill.SingleRowListener;
import org.apache.drill.common.DrillAutoCloseables;
import org.apache.drill.common.concurrent.ExtendedLatch;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.ZookeeperTestUtil;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.memory.RootAllocatorFactory;
import org.apache.drill.exec.physical.impl.ScreenCreator;
import org.apache.drill.exec.physical.impl.SingleSenderCreator.SingleSenderRootExec;
import org.apache.drill.exec.physical.impl.mergereceiver.MergingRecordBatch;
import org.apache.drill.exec.physical.impl.partitionsender.PartitionerDecorator;
import org.apache.drill.exec.physical.impl.unorderedreceiver.UnorderedReceiverBatch;
import org.apache.drill.exec.physical.impl.xsort.ExternalSortBatch;
import org.apache.drill.exec.planner.sql.DrillSqlWorker;
import org.apache.drill.exec.proto.GeneralRPCProtos.Ack;
import org.apache.drill.exec.proto.UserBitShared.ExceptionWrapper;
import org.apache.drill.exec.proto.UserBitShared.QueryData;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.proto.UserBitShared.QueryResult.QueryState;
import org.apache.drill.exec.proto.UserBitShared.QueryType;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.RecordBatchLoader;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.rpc.ConnectionThrottle;
import org.apache.drill.exec.rpc.DrillRpcFuture;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.apache.drill.exec.rpc.user.UserResultsListener;
import org.apache.drill.exec.store.pojo.PojoRecordReader;
import org.apache.drill.exec.testing.Controls;
import org.apache.drill.exec.testing.ControlsInjectionUtil;
import org.apache.drill.exec.util.Pointer;
import org.apache.drill.exec.work.fragment.FragmentExecutor;
import org.apache.drill.categories.SlowTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.TestInstantiationException;
import org.slf4j.Logger;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

/**
 * <p>Test how resilient drillbits are to throwing exceptions during various phases of query
 * execution by injecting exceptions at various points, and to cancellations in various phases.</p>
 *
 * Note: to debug this test case:<br>
 * <li>use 1000 value for PROBLEMATIC_TEST_NUM_RUNS and possibly NUM_RUNS for RepeatedTest</li>
 * <li>specify Level.DEBUG for CURRENT_LOG_LEVEL</li>
 * <li>compare trace output for successful test case and failed</li>
 */
@Tag(SlowTest.TAG)
@Tag(FlakyTest.TAG)
public class TestDrillbitResilience extends ClusterTest {
  private static final Logger logger = org.slf4j.LoggerFactory.getLogger(TestDrillbitResilience.class);
  protected static LogFixture logFixture;

  /**
   * The number of times test should be repeated. For proper testing this functionality set this value to 1000
   */
  private static final int NUM_RUNS = 3;
  private static final int PROBLEMATIC_TEST_NUM_RUNS = 3;
  private static final int TIMEOUT = 15;
  private final static Level CURRENT_LOG_LEVEL = Level.INFO;

  /**
   * Note: Counting sys.memory executes a fragment on every drillbit. This is a better check in comparison to
   * counting sys.drillbits.
   */
  private static final String TEST_QUERY = "select * from sys.memory";

  /*
   * Canned drillbit names.
   */
  private final static String DRILLBIT_ALPHA = "alpha";
  private final static String DRILLBIT_BETA = "beta";
  private final static String DRILLBIT_GAMMA = "gamma";

  @BeforeAll
  public static void startSomeDrillbits() throws Exception {
    logFixture = LogFixture.builder()
      .toConsole()
      .logger(TestDrillbitResilience.class, CURRENT_LOG_LEVEL)
      .logger(DrillClient.class, CURRENT_LOG_LEVEL)
      .logger(QueryStateProcessor.class, CURRENT_LOG_LEVEL)
      .logger(WorkManager.class, CURRENT_LOG_LEVEL)
      .logger(UnorderedReceiverBatch.class, CURRENT_LOG_LEVEL)
      .logger(ExtendedLatch.class, CURRENT_LOG_LEVEL)
      .logger(Foreman.class, CURRENT_LOG_LEVEL)
      .logger(QueryStateProcessor.class, CURRENT_LOG_LEVEL)
      .logger(ExecutionControlsInjector.class, CURRENT_LOG_LEVEL)
      .build();
    ZookeeperTestUtil.setJaasTestConfigFile();
    dirTestWatcher.start(TestDrillbitResilience.class); // until DirTestWatcher is implemented for JUnit5
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
      .configProperty(ExecConstants.HTTP_ENABLE, false) // turn off the HTTP server to avoid port conflicts between the drill bits
      .withBits(DRILLBIT_ALPHA, DRILLBIT_BETA, DRILLBIT_GAMMA);
    startCluster(builder);
    clearAllInjections();
    logger.debug("Start 3 drillbits Test Drill cluster: {}, {}, {}", DRILLBIT_ALPHA, DRILLBIT_BETA, DRILLBIT_GAMMA);
  }

  @AfterAll
  public static void tearDownAfterClass() throws Exception {
    logFixture.close();
  }

  /**
   * Clear all injections.
   */
  private static void clearAllInjections() {
    logger.debug("Clear all injections");
    Preconditions.checkNotNull(client);
    ControlsInjectionUtil.clearControls(client.client());
  }

  /**
   * Check that all the drillbits are ok.
   * <p/>
   * <p>The current implementation does this by counting the number of drillbits using a query.
   */
  private static void assertDrillbitsOk() {
    SingleRowListener listener = new SingleRowListener() {
      private final BufferAllocator bufferAllocator = RootAllocatorFactory.newRoot(cluster.config());
      private final RecordBatchLoader loader = new RecordBatchLoader(bufferAllocator);

      @Override
      public void rowArrived(QueryDataBatch queryResultBatch) {
        // load the single record
        final QueryData queryData = queryResultBatch.getHeader();
        loader.load(queryData.getDef(), queryResultBatch.getData());
        assertEquals(1, loader.getRecordCount());

        // there should only be one column
        final BatchSchema batchSchema = loader.getSchema();
        assertEquals(1, batchSchema.getFieldCount());

        // the column should be an integer
        final MaterializedField countField = batchSchema.getColumn(0);
        final MinorType fieldType = countField.getType().getMinorType();
        assertEquals(MinorType.BIGINT, fieldType);

        // get the column value
        final VectorWrapper<?> vw = loader.iterator().next();
        final Object obj = vw.getValueVector().getAccessor().getObject(0);
        assertTrue(obj instanceof Long);
        final Long countValue = (Long) obj;

        // assume this means all the drillbits are still ok
        assertEquals(cluster.drillbits().size(), countValue.intValue());

        loader.clear();
      }

      @Override
      public void cleanup() {
        loader.clear();
        DrillAutoCloseables.closeNoChecked(bufferAllocator);
      }
    };

    try {
      QueryTestUtil.testWithListener(client.client(), QueryType.SQL, "select count(*) from sys.memory", listener);
      listener.waitForCompletion();
      QueryState state = listener.getQueryState();
      assertSame(state, QueryState.COMPLETED, () -> String.format("QueryState should be COMPLETED (and not %s).", state));
      assertTrue(listener.getErrorList().isEmpty(), "There should not be any errors when checking if Drillbits are OK");
    } catch (final Exception e) {
      throw new RuntimeException("Couldn't query active drillbits", e);
    } finally {
      logger.debug("Cleanup listener");
      listener.cleanup();
    }
    logger.debug("Drillbits are ok.");
  }

  @BeforeEach
  void setUp(TestInfo testInfo) {
    String testName = testInfo.getTestMethod().orElseThrow(() -> new TestInstantiationException("Can't get method neme")).getName();
    String testOrRepetitionName = testInfo.getDisplayName();
    if(testOrRepetitionName.startsWith("repetition")) {
      logger.debug("{} for {} test started", testOrRepetitionName, testName);
    } else {
      logger.debug("{} test started", testName);
    }
  }

  @AfterEach
  public void checkDrillbits(TestInfo testInfo) {
    clearAllInjections(); // so that the drillbit check itself doesn't trigger anything
    assertDrillbitsOk(); // TODO we need a way to do this without using a query
    String testName = testInfo.getTestMethod().orElseThrow(() -> new TestInstantiationException("Can't get method neme")).getName();
    String testOrRepetitionName = testInfo.getDisplayName();
    if(testOrRepetitionName.startsWith("repetition")) {
      logger.debug("{} for {} test finished", testOrRepetitionName, testName);
    } else {
      logger.debug("{} test finished", testName);
    }
  }

  @Test
  @Timeout(value = TIMEOUT)
  public void settingNoOpInjectionsAndQuery() {
    final long before = countAllocatedMemory();

    final String controls = Controls.newBuilder()
      .addExceptionOnBit(getClass(), "noop", RuntimeException.class, cluster.drillbit(DRILLBIT_BETA).getContext().getEndpoint())
      .build();
    setControls(controls);
    final WaitUntilCompleteListener listener = new WaitUntilCompleteListener();
    QueryTestUtil.testWithListener(client.client(), QueryType.SQL, TEST_QUERY, listener);
    final Pair<QueryState, Exception> pair = listener.waitForCompletion();
    assertStateCompleted(pair, QueryState.COMPLETED);

    final long after = countAllocatedMemory();
    assertEquals(before, after, () -> String.format("We are leaking %d bytes", after - before));
  }

  @RepeatedTest(NUM_RUNS)
  @Timeout(value = TIMEOUT)
  public void foreman_runTryBeginning() {
    final long before = countAllocatedMemory();

    testForeman("run-try-beginning");

    final long after = countAllocatedMemory();
    assertEquals(before, after, () -> String.format("We are leaking %d bytes", after - before));
  }

  @RepeatedTest(PROBLEMATIC_TEST_NUM_RUNS) // for more info: DRILL-3163, DRILL-3167, DRILL-7973, DRILL-8030
  @Timeout(value = TIMEOUT)
  public void foreman_runTryEnd() {
    final long before = countAllocatedMemory();

    testForeman("run-try-end");

    final long after = countAllocatedMemory();
    assertEquals(before, after, () -> String.format("We are leaking %d bytes", after - before));
  }

  @Test // To test pause and resume. Test hangs and times out if resume did not happen.
  @Timeout(value = TIMEOUT)
  public void passThrough() {
    final long before = countAllocatedMemory();

    final WaitUntilCompleteListener listener = new WaitUntilCompleteListener() {
      @Override
      public void queryIdArrived(final QueryId queryId) {
        super.queryIdArrived(queryId);
        final ExtendedLatch trigger = new ExtendedLatch(1);
        (new ResumingThread(queryId, ex, trigger)).start();
        trigger.countDown();
      }
    };

    final String controls = Controls.newBuilder()
      .addPause(PojoRecordReader.class, "read-next")
      .build();
    setControls(controls);

    QueryTestUtil.testWithListener(client.client(), QueryType.SQL, TEST_QUERY, listener);
    final Pair<QueryState, Exception> result = listener.waitForCompletion();
    assertStateCompleted(result, QueryState.COMPLETED);

    final long after = countAllocatedMemory();
    assertEquals(before, after, () -> String.format("We are leaking %d bytes", after - before));
  }


  // DRILL-2383: Cancellation TC 1: cancel before any result set is returned.
  @RepeatedTest(PROBLEMATIC_TEST_NUM_RUNS) // for more info: DRILL-3052, DRILL-3192
  @Timeout(value = TIMEOUT)
  public void cancelWhenQueryIdArrives() {
    final long before = countAllocatedMemory();

    final WaitUntilCompleteListener listener = new WaitUntilCompleteListener() {

      @Override
      public void queryIdArrived(final QueryId queryId) {
        super.queryIdArrived(queryId);
        cancelAndResume();
      }
    };

    final String controls = Controls.newBuilder()
      .addPause(FragmentExecutor.class, "fragment-running")
      .build();
    assertCancelledWithoutException(controls, listener);

    final long after = countAllocatedMemory();
    assertEquals(before, after, () -> String.format("We are leaking %d bytes", after - before));
  }

  // DRILL-2383: Cancellation TC 2: cancel in the middle of fetching result set
  @RepeatedTest(PROBLEMATIC_TEST_NUM_RUNS) // for more info: DRILL-6228, DRILL-8030
  @Timeout(value = TIMEOUT)
  public void cancelInMiddleOfFetchingResults() {
    final long before = countAllocatedMemory();

    final WaitUntilCompleteListener listener = new WaitUntilCompleteListener() {
      private boolean cancelRequested = false;

      @Override
      public void dataArrived(final QueryDataBatch result, final ConnectionThrottle throttle) {
        if (!cancelRequested) {
          check(queryId != null, "Query id should not be null, since we have waited long enough.");
          cancelAndResume();
          cancelRequested = true;
        }
        result.release();
      }
    };

    // skip once i.e. wait for one batch, so that #dataArrived above triggers #cancelAndResume
    final String controls = Controls.newBuilder()
      .addPause(ScreenCreator.class, "sending-data", 1)
      .build();
    assertCancelledWithoutException(controls, listener);

    final long after = countAllocatedMemory();
    assertEquals(before, after, () -> String.format("We are leaking %d bytes", after - before));
  }

  // DRILL-2383: Cancellation TC 3: cancel after all result set are produced but not all are fetched
  @RepeatedTest(PROBLEMATIC_TEST_NUM_RUNS) // for more info: DRILL-6228, DRILL-8030
  @Timeout(value = TIMEOUT)
  public void cancelAfterAllResultsProduced() {
    final long before = countAllocatedMemory();

    final WaitUntilCompleteListener listener = new WaitUntilCompleteListener() {

      @Override
      public void dataArrived(final QueryDataBatch result, final ConnectionThrottle throttle) {
        if (lastDrillbit()) {
          check(queryId != null, "Query id should not be null, since we have waited long enough.");
          cancelAndResume();
        }
        result.release();
      }
    };

    final String controls = Controls.newBuilder()
      .addPause(ScreenCreator.class, "send-complete")
      .build();
    assertCancelledWithoutException(controls, listener);

    final long after = countAllocatedMemory();
    assertEquals(before, after, () -> String.format("We are leaking %d bytes", after - before));
  }

  // DRILL-2383: Cancellation TC 4: cancel after everything is completed and fetched
  @RepeatedTest(PROBLEMATIC_TEST_NUM_RUNS) // for more info: DRILL-3967, DRILL-7973
  @Timeout(value = TIMEOUT)
  public void cancelAfterEverythingIsCompleted() {
    final long before = countAllocatedMemory();

    final WaitUntilCompleteListener listener = new WaitUntilCompleteListener() {

      @Override
      public void dataArrived(final QueryDataBatch result, final ConnectionThrottle throttle) {
        if (lastDrillbit()) {
          check(queryId != null, "Query id should not be null, since we have waited long enough.");
          // need to wait until all batches are processed, since foreman-cleanup - the pause that happened earlier,
          // than the client accepts queryCompleted() from UserResultsListener
          cancelAndResume();
        }
        result.release();
      }
    };

    final String controls = Controls.newBuilder()
      .addPause(Foreman.class, "foreman-cleanup")
      .build();
    assertCompletedWithoutException(controls, listener); // changed from CANCELLED

    final long after = countAllocatedMemory();
    assertEquals(before, after, () -> String.format("We are leaking %d bytes", after - before));
  }

  @Test // DRILL-2383: Completion TC 1: success
  @Timeout(value = TIMEOUT)
  public void successfullyCompletes() {
    final long before = countAllocatedMemory();

    final WaitUntilCompleteListener listener = new WaitUntilCompleteListener();
    QueryTestUtil.testWithListener(client.client(), QueryType.SQL, TEST_QUERY, listener);
    final Pair<QueryState, Exception> result = listener.waitForCompletion();
    assertStateCompleted(result, QueryState.COMPLETED);

    final long after = countAllocatedMemory();
    assertEquals(before, after, () -> String.format("We are leaking %d bytes", after - before));
  }

  @Test // DRILL-2383: Completion TC 2: failed query - before query is executed - while sql parsing
  @Timeout(value = TIMEOUT)
  public void failsWhenParsing() {
    final long before = countAllocatedMemory();

    final String exceptionDesc = "sql-parsing";
    final Class<? extends Throwable> exceptionClass = ForemanSetupException.class;
    // Inject the failure twice since there can be retry after first failure introduced in DRILL-6762. Retry is because
    // of version mismatch in local and remote function registry which syncs up lazily.
    final String controls = Controls.newBuilder()
      .addException(DrillSqlWorker.class, exceptionDesc, exceptionClass, 0, 2)
      .build();
    assertFailsWithException(controls, exceptionClass, exceptionDesc, TEST_QUERY);

    final long after = countAllocatedMemory();
    assertEquals(before, after, () -> String.format("We are leaking %d bytes", after - before));
  }

  @Test // DRILL-2383: Completion TC 3: failed query - before query is executed, while sending fragments to other drillbits
  @Timeout(value = TIMEOUT)
  public void failsWhenSendingFragments() {
    final long before = countAllocatedMemory();

    final String exceptionDesc = "send-fragments";
    final Class<? extends Throwable> exceptionClass = ForemanException.class;
    final String controls = Controls.newBuilder()
      .addException(FragmentsRunner.class, exceptionDesc, exceptionClass)
      .build();
    assertFailsWithException(controls, exceptionClass, exceptionDesc, TEST_QUERY);

    final long after = countAllocatedMemory();
    assertEquals(before, after, () -> String.format("We are leaking %d bytes", after - before));
  }

  @Test // DRILL-2383: Completion TC 4: failed query - during query execution
  @Timeout(value = TIMEOUT)
  public void failsDuringExecution() {
    final long before = countAllocatedMemory();

    final String exceptionDesc = "fragment-execution";
    final Class<? extends Throwable> exceptionClass = IOException.class;
    final String controls = Controls.newBuilder()
      .addException(FragmentExecutor.class, exceptionDesc, exceptionClass)
      .build();
    assertFailsWithException(controls, exceptionClass, exceptionDesc, TEST_QUERY);

    final long after = countAllocatedMemory();
    assertEquals(before, after, () -> String.format("We are leaking %d bytes", after - before));
  }

  /**
   * Test canceling query interrupts currently blocked FragmentExecutor threads waiting for some event to happen.
   * Specifically tests canceling fragment which has {@link MergingRecordBatch} blocked waiting for data.
   */
  @RepeatedTest(NUM_RUNS)
  @Timeout(value = TIMEOUT)
  public void interruptingBlockedMergingRecordBatch() {
    final long before = countAllocatedMemory();

    final String control = Controls.newBuilder()
      .addPause(MergingRecordBatch.class, "waiting-for-data", 1)
      .build();
    interruptingBlockedFragmentsWaitingForData(control);

    final long after = countAllocatedMemory();
    assertEquals(before, after, () -> String.format("We are leaking %d bytes", after - before));
  }

  /**
   * Test canceling query interrupts currently blocked FragmentExecutor threads waiting for some event to happen.
   * Specifically tests canceling fragment which has {@link UnorderedReceiverBatch} blocked waiting for data.
   */
  @RepeatedTest(PROBLEMATIC_TEST_NUM_RUNS)
  @Timeout(value = TIMEOUT)
  public void interruptingBlockedUnorderedReceiverBatch() {
    final long before = countAllocatedMemory();

    final String control = Controls.newBuilder()
      .addPause(UnorderedReceiverBatch.class, "waiting-for-data", 1)
      .build();
    logger.debug("Start interruptingBlockedFragmentsWaitingForData");
    interruptingBlockedFragmentsWaitingForData(control);

    final long after = countAllocatedMemory();
    assertEquals(before, after, () -> String.format("We are leaking %d bytes", after - before));
  }

  /**
   * Tests interrupting the fragment thread that is running {@link PartitionSenderRootExec}.
   * {@link PartitionSenderRootExec} spawns threads for partitioner. Interrupting fragment thread should also interrupt
   * the partitioner threads.
   */
  @RepeatedTest(NUM_RUNS)
  @Timeout(value = TIMEOUT)
  public void interruptingPartitionerThreadFragment() {
    try {
      client.alterSession(SLICE_TARGET, "1");
      client.alterSession(ENABLE_HASH_AGG_OPTION, "true");
      client.alterSession(PARTITION_SENDER_SET_THREADS.getOptionName(), "6");

      final long before = countAllocatedMemory();

      final String controls = Controls.newBuilder()
      .addLatch(PartitionerDecorator.class, "partitioner-sender-latch")
      .addPause(PartitionerDecorator.class, "wait-for-fragment-interrupt", 1)
      .build();

      final String query = "SELECT sales_city, COUNT(*) cnt FROM cp.`region.json` GROUP BY sales_city";
      assertCancelledWithoutException(controls, new ListenerThatCancelsQueryAfterFirstBatchOfData(), query);

      final long after = countAllocatedMemory();
      assertEquals(before, after, () -> String.format("We are leaking %d bytes", after - before));
    } finally {
      client.resetSession(SLICE_TARGET);
      client.resetSession(ENABLE_HASH_AGG_OPTION);
      client.resetSession(PARTITION_SENDER_SET_THREADS.getOptionName());
    }
  }

  @RepeatedTest(PROBLEMATIC_TEST_NUM_RUNS) // for more info: DRILL-3193, DRILL-7973
  @Timeout(value = TIMEOUT)
  public void interruptingWhileFragmentIsBlockedInAcquiringSendingTicket() {
    final long before = countAllocatedMemory();

    final String control = Controls.newBuilder()
      .addPause(SingleSenderRootExec.class, "data-tunnel-send-batch-wait-for-interrupt", 1)
      .build();
    assertCancelledWithoutException(control, new ListenerThatCancelsQueryAfterFirstBatchOfData());

    final long after = countAllocatedMemory();
    assertEquals(before, after, () -> String.format("We are leaking %d bytes", after - before));
  }

  @RepeatedTest(PROBLEMATIC_TEST_NUM_RUNS)
  @Timeout(value = TIMEOUT)
  public void memoryLeaksWhenCancelled() {
    client.alterSession(SLICE_TARGET, "10");

    final long before = countAllocatedMemory();

    try {
      final String controls = Controls.newBuilder()
        .addPause(ScreenCreator.class, "sending-data", 1)
        .build();
      String query = null;
      try {
        query = BaseTestQuery.getFile("queries/tpch/09.sql");
        query = query.substring(0, query.length() - 1); // drop the ";"
      } catch (final IOException e) {
        fail("Failed to get query file", e);
      }

      final WaitUntilCompleteListener listener = new WaitUntilCompleteListener() {
        private volatile boolean cancelRequested = false;

        @Override
        public void dataArrived(final QueryDataBatch result, final ConnectionThrottle throttle) {
          if (!cancelRequested) {
            check(queryId != null, "Query id should not be null, since we have waited long enough.");
            cancelAndResume();
            cancelRequested = true;
          }
          result.release();
        }
      };

      assertCancelledWithoutException(controls, listener, query);

      final long after = countAllocatedMemory();
      assertEquals(before, after, () -> String.format("We are leaking %d bytes", after - before));
    } finally {
      client.resetSession(SLICE_TARGET);
    }
  }

  @RepeatedTest(NUM_RUNS) // for more info: DRILL-3194, DRILL-7973
  @Timeout(value = TIMEOUT)
  public void memoryLeaksWhenFailed() {
    client.alterSession(SLICE_TARGET, "10");

    final long before = countAllocatedMemory();

    try {
      final String exceptionDesc = "fragment-execution";
      final Class<? extends Throwable> exceptionClass = IOException.class;
      final String controls = Controls.newBuilder()
        .addException(FragmentExecutor.class, exceptionDesc, exceptionClass)
        .build();

      String query = null;
      try {
        query = BaseTestQuery.getFile("queries/tpch/09.sql");
        query = query.substring(0, query.length() - 1); // drop the ";"
      } catch (final IOException e) {
        fail("Failed to get query file: " + e);
      }

      assertFailsWithException(controls, exceptionClass, exceptionDesc, query);

      final long after = countAllocatedMemory();
      assertEquals(before, after, () -> String.format("We are leaking %d bytes", after - before));

    } finally {
      client.resetSession(SLICE_TARGET);
    }
  }

  @Test
  @Timeout(value = TIMEOUT) // for more info: DRILL-3065
  public void failsAfterMSorterSorting() {

    // Note: must use an input table that returns more than one
    // batch. The sort uses an optimization for single-batch inputs
    // which bypasses the code where this partiucular fault is
    // injected.

    final String query = "select n_name from cp.`tpch/lineitem.parquet` order by n_name";
    final Class<? extends Exception> typeOfException = RuntimeException.class;

    final long before = countAllocatedMemory();
    final String controls = Controls.newBuilder()
      .addException(ExternalSortBatch.class, ExternalSortBatch.INTERRUPTION_AFTER_SORT, typeOfException)
      .build();
    assertFailsWithException(controls, typeOfException, ExternalSortBatch.INTERRUPTION_AFTER_SORT, query);

    final long after = countAllocatedMemory();
    assertEquals(before, after, () -> String.format("We are leaking %d bytes", after - before));
  }

  @Test
  @Timeout(value = TIMEOUT) // for more info: DRILL-3085
  public void failsAfterMSorterSetup() {

    // Note: must use an input table that returns more than one
    // batch. The sort uses an optimization for single-batch inputs
    // which bypasses the code where this partiucular fault is
    // injected.

    final String query = "select n_name from cp.`tpch/lineitem.parquet` order by n_name";
    final Class<? extends Exception> typeOfException = RuntimeException.class;

    final long before = countAllocatedMemory();
    final String controls = Controls.newBuilder()
    .addException(ExternalSortBatch.class, ExternalSortBatch.INTERRUPTION_AFTER_SETUP, typeOfException)
      .build();
    assertFailsWithException(controls, typeOfException, ExternalSortBatch.INTERRUPTION_AFTER_SETUP, query);

    final long after = countAllocatedMemory();
    assertEquals(before, after, () -> String.format("We are leaking %d bytes", after - before));
  }

  /**
   * Tests can use this listener to wait, until the submitted query completes or fails, by
   * calling #waitForCompletion.
   */
  private static class WaitUntilCompleteListener implements UserResultsListener {
    protected final ExtendedLatch latch = new ExtendedLatch(1); // to signal completion
    protected QueryId queryId = null;
    protected volatile Pointer<Exception> ex = new Pointer<>();
    protected volatile QueryState state = null;
    private int count = 0;

    /**
     * Method that sets the exception if the condition is not met.
     */
    protected final void check(final boolean condition, final String format, final Object... args) {
      if (!condition) {
        ex.value = new IllegalStateException(String.format(format, args));
      }
    }

    /**
     * Method that cancels and resumes the query, in order.
     */
    protected final void cancelAndResume() {
      Preconditions.checkNotNull(queryId);
      final ExtendedLatch trigger = new ExtendedLatch(1);
      (new CancellingThread(queryId, ex, trigger)).start();
      (new ResumingThread(queryId, ex, trigger)).start();
    }

    @Override
    public void queryIdArrived(final QueryId queryId) {
      this.queryId = queryId;
    }

    @Override
    public void submissionFailed(final UserException ex) {
      this.ex.value = ex;
      state = QueryState.FAILED;
      latch.countDown();
    }

    @Override
    public void queryCompleted(final QueryState state) {
      this.state = state;
      latch.countDown();
    }

    @Override
    public void dataArrived(final QueryDataBatch result, final ConnectionThrottle throttle) {
      result.release();
    }

    public final Pair<QueryState, Exception> waitForCompletion() {
      try {
        logger.debug("Wait for completion. latch: {}", latch.getCount());
        latch.await(); // awaitUninterruptibly method usage here prevents JUnit timeout work
      } catch (InterruptedException e) {
        logger.error("Interrupted while waiting for event latch");
      }
      logger.debug("Completed. Wait finished");
      return new Pair<>(state, ex.value);
    }

    /**
     * It is useful for queries returned the data from all drillbits. Use this method to make sure {@link #dataArrived}
     * returns the data from every drillbit
     *
     * @return true, in case current listener methods return results from last drillbit already
     */
    boolean lastDrillbit() {
      return ++count == cluster.drillbits().size();
    }
  }

  private static class ListenerThatCancelsQueryAfterFirstBatchOfData extends WaitUntilCompleteListener {
    private boolean cancelRequested = false;

    @Override
    public void dataArrived(final QueryDataBatch result, final ConnectionThrottle throttle) {
      if (!cancelRequested) {
        logger.debug("First batch arrived, so cancelling thread started");
        check(queryId != null, "Query id should not be null, since we have waited long enough.");
        (new CancellingThread(queryId, ex, null)).start();
        cancelRequested = true;
      }
      result.release();
    }
  }

  /**
   * Thread that cancels the given query id. After the cancel is acknowledged, the latch is counted down.
   */
  private static class CancellingThread extends Thread {
    private final QueryId queryId;
    private final Pointer<Exception> ex;
    private final ExtendedLatch latch;

    public CancellingThread(QueryId queryId, Pointer<Exception> ex, ExtendedLatch latch) {
      this.queryId = queryId;
      this.ex = ex;
      this.latch = latch;
      logger.debug("Cancelling thread created");
    }

    @Override
    public void run() {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        logger.debug("Cancelling thread interrupted. Ignore it");
        // just ignore
      }
      logger.debug("Cancelling {} query started", queryId);
      final DrillRpcFuture<Ack> cancelAck = client.client().cancelQuery(queryId);
      logger.debug("Check future: {}", cancelAck);
      try {
        Ack ack = cancelAck.checkedGet();
        logger.debug("Sleep thread for 0.01 seconds");
        Thread.sleep(10);
        logger.debug("Ack: {}", ack);
      } catch (RpcException ex) {
        this.ex.value = ex;
        logger.debug("The query wasn't canceled." + ex);
      } catch (InterruptedException e) {
        logger.debug("Sleep thread interrupted. Ignore it");
        // just ignore
      }
      if (latch != null) {
        latch.countDown();
      }
      logger.debug("Finish cancelling thread");
    }
  }

  /**
   * Thread that resumes the given query id. After the latch is counted down, the resume signal is sent, until then
   * the thread waits without interruption.
   */
  private static class ResumingThread extends Thread {
    private final QueryId queryId;
    private final Pointer<Exception> ex;
    private final ExtendedLatch latch;

    public ResumingThread(final QueryId queryId, final Pointer<Exception> ex, final ExtendedLatch latch) {
      this.queryId = queryId;
      this.ex = ex;
      this.latch = latch;
      logger.debug("Cancelling thread created");
    }

    @Override
    public void run() {
      latch.awaitUninterruptibly();
      final DrillRpcFuture<Ack> resumeAck = client.client().resumeQuery(queryId);
      try {
        resumeAck.checkedGet();
      } catch (final RpcException ex) {
        this.ex.value = ex;
      }
    }
  }

  /**
   * Given the result of {@link WaitUntilCompleteListener#waitForCompletion},
   * this method fails if the completed state is not as expected, or if an
   * exception is thrown. The completed state could be COMPLETED or CANCELED.
   * This state is set when {@link WaitUntilCompleteListener#queryCompleted} is
   * called.
   */
  private void assertStateCompleted(final Pair<QueryState, Exception> result, final QueryState expectedState) {
    final QueryState actualState = result.getFirst();
    final Exception exception = result.getSecond();
    if (actualState != expectedState || exception != null) {
      fail(String.format("Query state is incorrect (expected: %s, actual: %s) AND/OR \nException thrown: %s",
        expectedState, actualState, exception == null ? "none." : exception));
    }
  }

  /**
   * Given a set of controls, this method ensures that the given query completes with a {@link QueryState#CANCELED} state.</p>
   */
  private void assertCancelledWithoutException(final String controls, final WaitUntilCompleteListener listener,
                                                      final String query) {
    setControls(controls);

    QueryTestUtil.testWithListener(client.client(), QueryType.SQL, query, listener);
    final Pair<QueryState, Exception> result = listener.waitForCompletion();
    assertStateCompleted(result, QueryState.CANCELED);
  }

  /**
   * Given a set of controls, this method ensures that the given query completes with a {@link QueryState#COMPLETED} state.</p>
   * {@link QueryState#COMPLETED} is a terminal state and can't be canceled.
   * See more: {@link org.apache.drill.exec.work.foreman.QueryStateProcessor#cancel}
   */
  private void assertCompletedWithoutException(final String controls, final WaitUntilCompleteListener listener) {
    setControls(controls);

    QueryTestUtil.testWithListener(client.client(), QueryType.SQL, TEST_QUERY, listener);
    assertStateCompleted(listener.waitForCompletion(), QueryState.COMPLETED);
  }

  /**
   * Given a set of controls, this method ensures that the TEST_QUERY completes with a CANCELED state.
   */
  private void assertCancelledWithoutException(final String controls, final WaitUntilCompleteListener listener) {
    assertCancelledWithoutException(controls, listener, TEST_QUERY);
  }

  private long countAllocatedMemory() {
    // wait to make sure all fragments finished cleaning up
    try {
      logger.debug("Sleep thread for 2 seconds");
      Thread.sleep(1500); // 1500
    } catch (InterruptedException e) {
      logger.debug("Sleep thread interrupted. Ignore it", e);
      // just ignore
    }

    long allocated = 0;
    for (Drillbit drillbit : cluster.drillbits()) {
      allocated += drillbit.getContext().getAllocator().getAllocatedMemory();
    }
    logger.debug("Allocated memory: " + allocated);
    return allocated;
  }

  private void interruptingBlockedFragmentsWaitingForData(final String control) {
    try {
      client.alterSession(SLICE_TARGET, "1");
      client.alterSession(ENABLE_HASH_AGG_OPTION, "false");

      final String query = "SELECT sales_city, COUNT(*) cnt FROM cp.`region.json` GROUP BY sales_city";
      assertCancelledWithoutException(control, new ListenerThatCancelsQueryAfterFirstBatchOfData(), query);
    } finally {
      client.resetSession(SLICE_TARGET);
      client.resetSession(ENABLE_HASH_AGG_OPTION);
    }
  }

  /**
   * Given a set of controls, this method ensures TEST_QUERY fails with the given class and desc.
   */
  private void assertFailsWithException(final String controls, final Class<? extends Throwable> exceptionClass,
                                               final String exceptionDesc, final String query) {
    setControls(controls);
    final WaitUntilCompleteListener listener = new WaitUntilCompleteListener();
    QueryTestUtil.testWithListener(client.client(), QueryType.SQL, query, listener);
    final Pair<QueryState, Exception> result = listener.waitForCompletion();
    final QueryState state = result.getFirst();
    assertSame(state, QueryState.FAILED, () -> String.format("Query state should be FAILED (and not %s).", state));
    assertExceptionMessage(result.getSecond(), exceptionClass, exceptionDesc);
  }

  /**
   * The same as {@link #assertFailsWithException}, but also allows COMPLETED state for the query.<br>
   * Note: in some cases we are completing the query faster than run-try-end exception is injecetd and thrown in
   * Foreman. The completed state is fine for such cases
   */
  private void assertFailsOrCompletedWithException(final String controls, final Class<? extends Throwable> exceptionClass,
                                        final String exceptionDesc, final String query) {
    setControls(controls);
    final WaitUntilCompleteListener listener = new WaitUntilCompleteListener();
    QueryTestUtil.testWithListener(client.client(), QueryType.SQL, query, listener);
    final Pair<QueryState, Exception> result = listener.waitForCompletion();
    final QueryState state = result.getFirst();

    assertTrue(state.equals(QueryState.FAILED) || state.equals(QueryState.COMPLETED),
      () -> String.format("Query state should be FAILED (and not %s).", state));
    assertExceptionMessage(result.getSecond(), exceptionClass, exceptionDesc);
  }

  /**
   * Test throwing exceptions from sites within the Foreman class, as specified by the site
   * description
   *
   * @param desc site description
   */
  private void testForeman(final String desc) {
    final String controls = Controls.newBuilder()
      .addException(Foreman.class, desc, ForemanException.class)
      .build();
    assertFailsOrCompletedWithException(controls, ForemanException.class, desc, TEST_QUERY);
  }

  /**
   * Set the given controls.
   */
  private void setControls(final String controls) {
    ControlsInjectionUtil.setControls(client.client(), controls);
  }

  /**
   * Check that the injected exception is what we were expecting.
   *
   * @param throwable      the throwable that was caught (by the test)
   * @param exceptionClass the expected exception class
   * @param desc           the expected exception site description
   */
  private void assertExceptionMessage(final Throwable throwable, final Class<? extends Throwable> exceptionClass,
                                      final String desc) {
    assertTrue(throwable instanceof UserException, "Throwable was not of UserException type");
    final ExceptionWrapper cause = ((UserException) throwable).getOrCreatePBError(false).getException();
    assertEquals(exceptionClass.getName(), cause.getExceptionClass(), "Exception class names should match");
    assertEquals(desc, cause.getMessage(), "Exception sites should match.");
  }
}
