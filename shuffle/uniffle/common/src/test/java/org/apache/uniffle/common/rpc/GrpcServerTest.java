/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.uniffle.common.rpc;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Queues;
import io.prometheus.client.CollectorRegistry;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.common.metrics.GRPCMetrics;
import org.apache.uniffle.common.util.ThreadUtils;

import static org.apache.uniffle.common.metrics.GRPCMetrics.GRPC_SERVER_EXECUTOR_ACTIVE_THREADS_KEY;
import static org.apache.uniffle.common.metrics.GRPCMetrics.GRPC_SERVER_EXECUTOR_BLOCKING_QUEUE_SIZE_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GrpcServerTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(GrpcServerTest.class);

  static class MockedGRPCMetrics extends GRPCMetrics {
    @Override
    public void registerMetrics() {
      // ignore
    }
  }

  @Test
  public void testGrpcExecutorPool() throws Exception {
    GRPCMetrics grpcMetrics = new MockedGRPCMetrics();
    grpcMetrics.register(new CollectorRegistry(true));
    GrpcServer.GrpcThreadPoolExecutor executor = new GrpcServer.GrpcThreadPoolExecutor(
        2,
        2,
        100,
        TimeUnit.MINUTES,
        Queues.newLinkedBlockingQueue(Integer.MAX_VALUE),
        ThreadUtils.getThreadFactory("Grpc-%d"),
        grpcMetrics
    );

    CountDownLatch countDownLatch = new CountDownLatch(3);
    for (int i = 0; i < 3; i++) {
      final int index = i;
      executor.submit(() -> {
        try {
          Thread.sleep(100 * 2);
        } catch (InterruptedException interruptedException) {
          interruptedException.printStackTrace();
        }
        LOGGER.info("Finished task: {}", index);
        countDownLatch.countDown();
      });
    }

    Thread.sleep(100);
    double activeThreads = grpcMetrics.getGaugeMap().get(GRPC_SERVER_EXECUTOR_ACTIVE_THREADS_KEY).get();
    assertEquals(2, activeThreads);
    double queueSize = grpcMetrics.getGaugeMap().get(GRPC_SERVER_EXECUTOR_BLOCKING_QUEUE_SIZE_KEY).get();
    assertEquals(1, queueSize);

    countDownLatch.await();
    // the metrics is updated afterExecute, which means it may take a while for the thread to decrease the metrics
    Thread.sleep(100);
    activeThreads = grpcMetrics.getGaugeMap().get(GRPC_SERVER_EXECUTOR_ACTIVE_THREADS_KEY).get();
    assertEquals(0, activeThreads);
    queueSize = grpcMetrics.getGaugeMap().get(GRPC_SERVER_EXECUTOR_BLOCKING_QUEUE_SIZE_KEY).get();
    assertEquals(0, queueSize);

    executor.shutdown();
  }
}
