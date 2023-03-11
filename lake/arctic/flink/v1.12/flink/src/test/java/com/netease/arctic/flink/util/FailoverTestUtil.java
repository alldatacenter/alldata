/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.flink.util;

import org.apache.flink.api.common.JobID;
import org.apache.flink.runtime.highavailability.nonha.embedded.HaLeadershipControl;
import org.apache.flink.runtime.minicluster.MiniCluster;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class FailoverTestUtil {

  public static final Logger LOG = LoggerFactory.getLogger(FailoverTestUtil.class);

  public static class RecordCounterToFail {

    private static AtomicInteger records;
    public static CompletableFuture<Void> fail;
    private static CompletableFuture<Void> continueProcessing;
    
    public static <T> DataStream<T> wrapWithFailureAfter(DataStream<T> stream, int failAfter) {
      records = new AtomicInteger();
      fail = new CompletableFuture<>();
      continueProcessing = new CompletableFuture<>();
      return stream.map(
          record -> {
            final boolean halfOfInputIsRead = records.incrementAndGet() > failAfter;
            final boolean notFailedYet = !fail.isDone();
            if (notFailedYet && halfOfInputIsRead) {
              fail.complete(null);
              continueProcessing.get();
            }
            return record;
          });
    }

    public static void waitToFail() throws ExecutionException, InterruptedException {
      fail.get();
    }

    public static void continueProcessing() {
      LOG.info("after failover");
      continueProcessing.complete(null);
    }
  }

  // ------------------------------------------------------------------------
  //  test utilities
  // ------------------------------------------------------------------------

  public enum FailoverType {
    NONE,
    TM,
    JM
  }

  public static void triggerFailover(
      FailoverType type, JobID jobId, Runnable afterFailAction, MiniCluster miniCluster)
      throws Exception {
    switch (type) {
      case NONE:
        afterFailAction.run();
        break;
      case TM:
        restartTaskManager(afterFailAction, miniCluster);
        break;
      case JM:
        triggerJobManagerFailover(jobId, afterFailAction, miniCluster);
        break;
    }
  }

  private static void triggerJobManagerFailover(
      JobID jobId, Runnable afterFailAction, MiniCluster miniCluster) throws Exception {
    final HaLeadershipControl haLeadershipControl = miniCluster.getHaLeadershipControl().get();
    haLeadershipControl.revokeJobMasterLeadership(jobId).get();
    afterFailAction.run();
    haLeadershipControl.grantJobMasterLeadership(jobId).get();
  }

  private static void restartTaskManager(Runnable afterFailAction, MiniCluster miniCluster)
      throws Exception {
    miniCluster.terminateTaskManager(0).get();
    afterFailAction.run();
    miniCluster.startTaskManager();
  }

}
