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
package org.apache.drill;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Semaphore;

import org.apache.drill.categories.SlowTest;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.test.BaseTestQuery.SilentListener;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.test.TestTools;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.proto.UserBitShared.QueryResult.QueryState;
import org.apache.drill.exec.rpc.user.UserResultsListener;
import org.apache.drill.test.QueryTestUtil;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestRule;

import org.apache.drill.shaded.guava.com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/*
 * Note that the real interest here is that the drillbit doesn't become
 * unstable from running a lot of queries concurrently -- it's not about
 * any particular order of execution. We ignore the results.
 */
@Category({SlowTest.class})
public class TestTpchDistributedConcurrent extends ClusterTest {
  private static final Logger logger = LoggerFactory.getLogger(TestTpchDistributedConcurrent.class);

  @Rule
  public final TestRule TIMEOUT = TestTools.getTimeoutRule(400_000); // 400 secs

  /*
   * Valid test names taken from TestTpchDistributed. Fuller path prefixes are
   * used so that tests may also be taken from other locations -- more variety
   * is better as far as this test goes.
   */
  private static final String[] queryFile = {
    "queries/tpch/01.sql",
    "queries/tpch/03.sql",
    "queries/tpch/04.sql",
    "queries/tpch/05.sql",
    "queries/tpch/06.sql",
    "queries/tpch/07.sql",
    "queries/tpch/08.sql",
    "queries/tpch/09.sql",
    "queries/tpch/10.sql",
    "queries/tpch/11.sql",
    "queries/tpch/12.sql",
    "queries/tpch/13.sql",
    "queries/tpch/14.sql",
    // "queries/tpch/15.sql", this creates a view
    "queries/tpch/16.sql",
    "queries/tpch/18.sql",
    "queries/tpch/19_1.sql",
    "queries/tpch/20.sql",
  };

  private static final int TOTAL_QUERIES = 115;
  private static final int CONCURRENT_QUERIES = 15;
  private static final Random random = new Random(0xdeadbeef);

  private int remainingQueries = TOTAL_QUERIES - CONCURRENT_QUERIES;
  private final Semaphore completionSemaphore = new Semaphore(0);
  private final Semaphore submissionSemaphore = new Semaphore(0);
  private final Set<UserResultsListener> listeners = Sets.newIdentityHashSet();
  private final List<FailedQuery> failedQueries = new LinkedList<>();
  private Thread testThread = null; // used to interrupt semaphore wait in case of error

  @BeforeClass
  public static void setUp() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
        .configProperty(ExecConstants.USER_RPC_TIMEOUT, 5_000);
    startCluster(builder);
  }

  @Test
  public void testConcurrentQueries() {
    client.alterSession(ExecConstants.SLICE_TARGET, 10);

    testThread = Thread.currentThread();
    final QuerySubmitter querySubmitter = new QuerySubmitter();
    querySubmitter.start();

    // Kick off the initial queries. As they complete, they will submit more.
    submissionSemaphore.release(CONCURRENT_QUERIES);

    // Wait for all the queries to complete.
    InterruptedException interruptedException = null;
    try {
      completionSemaphore.acquire(TOTAL_QUERIES);
    } catch (InterruptedException e) {
      interruptedException = e;

      // List the failed queries.
      for (FailedQuery fq : failedQueries) {
        logger.error("{} failed with {}", fq.queryFile, fq.userEx);
      }
    }

    // Stop the querySubmitter thread.
    querySubmitter.interrupt();

    if (interruptedException != null) {
      logger.error("Interruped Exception ", interruptedException);
    }

    assertNull("Query error caused interruption", interruptedException);

    int nListeners = listeners.size();
    assertEquals(nListeners + " listeners still exist", 0, nListeners);

    assertEquals("Didn't submit all queries", 0, remainingQueries);
    assertEquals("Queries failed", 0, failedQueries.size());
  }

  private void submitRandomQuery() {
    String filename = queryFile[random.nextInt(queryFile.length)];
    String query;
    try {
      query = QueryTestUtil.normalizeQuery(getFile(filename)).replace(';', ' ');
    } catch (IOException e) {
      throw new RuntimeException("Caught exception", e);
    }
    UserResultsListener listener = new ChainingSilentListener(query);
    queryBuilder()
        .query(UserBitShared.QueryType.SQL, query)
        .withListener(listener);
    synchronized (this) {
      listeners.add(listener);
    }
  }

  private static class FailedQuery {
    final String queryFile;
    final UserException userEx;

    public FailedQuery(String queryFile, UserException userEx) {
      this.queryFile = queryFile;
      this.userEx = userEx;
    }
  }


  private class ChainingSilentListener extends SilentListener {
    private final String query;

    public ChainingSilentListener(final String query) {
      this.query = query;
    }

    @Override
    public void queryCompleted(QueryState state) {
      super.queryCompleted(state);

      synchronized (TestTpchDistributedConcurrent.this) {
        final Object object = listeners.remove(this);
        assertNotNull("listener not found", object);

        /* Only submit more queries if there hasn't been an error. */
        if ((failedQueries.size() == 0) && (remainingQueries > 0)) {
          /*
           * We can't directly submit the query from here, because we're on the RPC
           * thread, and it throws an exception if we try to send from here. So we
           * allow the QuerySubmitter thread to advance.
           */
          submissionSemaphore.release();
          --remainingQueries;
        }
      }
      completionSemaphore.release();
    }

    @Override
    public void submissionFailed(UserException uex) {
      super.submissionFailed(uex);

      completionSemaphore.release();
      logger.error("submissionFailed for {} \nwith:", query, uex);
      synchronized (TestTpchDistributedConcurrent.this) {
        final Object object = listeners.remove(this);
        assertNotNull("listener not found", object);
        failedQueries.add(new FailedQuery(query, uex));
        testThread.interrupt();
      }
    }
  }

  private class QuerySubmitter extends Thread {
    @Override
    public void run() {
      while (true) {
        try {
          submissionSemaphore.acquire();
        } catch (InterruptedException e) {
          logger.error("QuerySubmitter quitting.");
          return;
        }

        submitRandomQuery();
      }
    }
  }
}
