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

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.collect.Queues;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.common.config.RssBaseConf;
import org.apache.uniffle.common.metrics.GRPCMetrics;
import org.apache.uniffle.common.util.ExitUtils;
import org.apache.uniffle.common.util.ThreadUtils;

public class GrpcServer implements ServerInterface {

  private static final Logger LOG = LoggerFactory.getLogger(GrpcServer.class);

  private final Server server;
  private final int port;
  private final ExecutorService pool;

  public GrpcServer(RssBaseConf conf, BindableService service, GRPCMetrics grpcMetrics) {
    this.port = conf.getInteger(RssBaseConf.RPC_SERVER_PORT);
    long maxInboundMessageSize = conf.getLong(RssBaseConf.RPC_MESSAGE_MAX_SIZE);
    int rpcExecutorSize = conf.getInteger(RssBaseConf.RPC_EXECUTOR_SIZE);
    pool = new GrpcThreadPoolExecutor(
        rpcExecutorSize,
        rpcExecutorSize * 2,
        10,
        TimeUnit.MINUTES,
        Queues.newLinkedBlockingQueue(Integer.MAX_VALUE),
        ThreadUtils.getThreadFactory("Grpc-%d"),
        grpcMetrics
    );

    boolean isMetricsEnabled = conf.getBoolean(RssBaseConf.RPC_METRICS_ENABLED);
    if (isMetricsEnabled) {
      MonitoringServerInterceptor monitoringInterceptor =
          new MonitoringServerInterceptor(grpcMetrics);
      this.server = ServerBuilder
          .forPort(port)
          .addService(ServerInterceptors.intercept(service, monitoringInterceptor))
          .executor(pool)
          .addTransportFilter(new MonitoringServerTransportFilter(grpcMetrics))
          .maxInboundMessageSize((int)maxInboundMessageSize)
          .build();
    } else {
      this.server = ServerBuilder
          .forPort(port)
          .addService(service)
          .executor(pool)
          .maxInboundMessageSize((int)maxInboundMessageSize)
          .build();
    }
  }

  public static class GrpcThreadPoolExecutor extends ThreadPoolExecutor {
    private final GRPCMetrics grpcMetrics;
    private final AtomicLong activeThreadSize = new AtomicLong(0L);

    public GrpcThreadPoolExecutor(
        int corePoolSize,
        int maximumPoolSize,
        long keepAliveTime,
        TimeUnit unit,
        BlockingQueue<Runnable> workQueue,
        ThreadFactory threadFactory,
        GRPCMetrics grpcMetrics) {
      super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
      this.grpcMetrics = grpcMetrics;
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
      grpcMetrics.incGauge(GRPCMetrics.GRPC_SERVER_EXECUTOR_ACTIVE_THREADS_KEY);
      grpcMetrics.setGauge(GRPCMetrics.GRPC_SERVER_EXECUTOR_BLOCKING_QUEUE_SIZE_KEY,
          getQueue().size());
      super.beforeExecute(t, r);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
      grpcMetrics.decGauge(GRPCMetrics.GRPC_SERVER_EXECUTOR_ACTIVE_THREADS_KEY);
      grpcMetrics.setGauge(GRPCMetrics.GRPC_SERVER_EXECUTOR_BLOCKING_QUEUE_SIZE_KEY,
          getQueue().size());
      super.afterExecute(r, t);
    }
  }

  public void start() throws IOException {
    try {
      server.start();
    } catch (IOException e) {
      ExitUtils.terminate(1, "Fail to start grpc server", e, LOG);
    }
    LOG.info("Grpc server started, listening on {}.", port);
  }

  public void stop() throws InterruptedException {
    if (server != null) {
      server.shutdown().awaitTermination(10, TimeUnit.SECONDS);
      LOG.info("GRPC server stopped!");
    }
    if (pool != null) {
      pool.shutdown();
    }
  }

  public void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

}
