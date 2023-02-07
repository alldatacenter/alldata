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
package org.apache.drill.exec.work.filter;

import io.netty.buffer.DrillBuf;
import org.apache.drill.common.exceptions.DrillRuntimeException;

import org.apache.drill.exec.ops.AccountingDataTunnel;
import org.apache.drill.exec.ops.Consumer;
import org.apache.drill.exec.ops.DataTunnelStatusHandler;
import org.apache.drill.exec.ops.SendingAccountor;
import org.apache.drill.exec.proto.BitData;
import org.apache.drill.exec.proto.CoordinationProtos;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.exec.rpc.RpcOutcomeListener;
import org.apache.drill.exec.rpc.data.DataTunnel;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This sink receives the RuntimeFilters from the netty thread,
 * aggregates them in an async thread, broadcast the final aggregated
 * one to the RuntimeFilterRecordBatch.
 */
public class RuntimeFilterSink implements Closeable
{

  private BlockingQueue<RuntimeFilterWritable> rfQueue = new LinkedBlockingQueue<>();

  private Map<Integer, Integer> joinMjId2rfNumber;

  //HashJoin node's major fragment id to its corresponding probe side nodes's endpoints
  private Map<Integer, List<CoordinationProtos.DrillbitEndpoint>> joinMjId2probeScanEps = new HashMap<>();

  //HashJoin node's major fragment id to its corresponding probe side scan node's belonging major fragment id
  private Map<Integer, Integer> joinMjId2ScanMjId = new HashMap<>();

  //HashJoin node's major fragment id to its aggregated RuntimeFilterWritable
  private Map<Integer, RuntimeFilterWritable> joinMjId2AggregatedRF = new HashMap<>();
  //for debug usage
  private Map<Integer, Stopwatch> joinMjId2Stopwatch = new HashMap<>();

  private DrillbitContext drillbitContext;

  private SendingAccountor sendingAccountor;

  private  AsyncAggregateWorker asyncAggregateWorker;

  private AtomicBoolean running = new AtomicBoolean(true);

  private static final Logger logger = LoggerFactory.getLogger(RuntimeFilterSink.class);


  public RuntimeFilterSink(DrillbitContext drillbitContext, SendingAccountor sendingAccountor)
  {
    this.drillbitContext = drillbitContext;
    this.sendingAccountor = sendingAccountor;
    asyncAggregateWorker = new AsyncAggregateWorker();
    drillbitContext.getExecutor().submit(asyncAggregateWorker);
  }

  public void add(RuntimeFilterWritable runtimeFilterWritable)
  {
    if (!running.get()) {
      runtimeFilterWritable.close();
      return;
    }
    runtimeFilterWritable.retainBuffers(1);
    int joinMjId = runtimeFilterWritable.getRuntimeFilterBDef().getMajorFragmentId();
    if (joinMjId2Stopwatch.get(joinMjId) == null) {
      Stopwatch stopwatch = Stopwatch.createStarted();
      joinMjId2Stopwatch.put(joinMjId, stopwatch);
    }
    synchronized (rfQueue) {
      if (!running.get()) {
        runtimeFilterWritable.close();
        return;
      }
      rfQueue.add(runtimeFilterWritable);
      rfQueue.notify();
    }
  }

  public void close() {
    running.set(false);
    if (asyncAggregateWorker != null) {
      synchronized (rfQueue) {
        rfQueue.notify();
      }
    }
    while (!asyncAggregateWorker.over.get()) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        logger.error("interrupted while sleeping to wait for the aggregating worker thread to exit", e);
      }
    }
    for (RuntimeFilterWritable runtimeFilterWritable : joinMjId2AggregatedRF.values()) {
      runtimeFilterWritable.close();
    }
  }

  private void aggregate(RuntimeFilterWritable srcRuntimeFilterWritable)
  {
    BitData.RuntimeFilterBDef runtimeFilterB = srcRuntimeFilterWritable.getRuntimeFilterBDef();
    int joinMajorId = runtimeFilterB.getMajorFragmentId();
    int buildSideRfNumber;
    RuntimeFilterWritable toAggregated = null;
    buildSideRfNumber = joinMjId2rfNumber.get(joinMajorId);
    buildSideRfNumber--;
    joinMjId2rfNumber.put(joinMajorId, buildSideRfNumber);
    toAggregated = joinMjId2AggregatedRF.get(joinMajorId);
    if (toAggregated == null) {
      toAggregated = srcRuntimeFilterWritable;
      toAggregated.retainBuffers(1);
    } else {
      toAggregated.aggregate(srcRuntimeFilterWritable);
    }
    joinMjId2AggregatedRF.put(joinMajorId, toAggregated);
    if (buildSideRfNumber == 0) {
      joinMjId2AggregatedRF.remove(joinMajorId);
      route(toAggregated);
      joinMjId2rfNumber.remove(joinMajorId);
      Stopwatch stopwatch = joinMjId2Stopwatch.get(joinMajorId);
      logger.info(
          "received all the RFWs belonging to the majorId {}'s HashJoin nodes and flushed aggregated RFW out elapsed {} ms",
          joinMajorId,
          stopwatch.elapsed(TimeUnit.MILLISECONDS)
      );
    }
  }

  private void route(RuntimeFilterWritable srcRuntimeFilterWritable)
  {
    BitData.RuntimeFilterBDef runtimeFilterB = srcRuntimeFilterWritable.getRuntimeFilterBDef();
    int joinMajorId = runtimeFilterB.getMajorFragmentId();
    UserBitShared.QueryId queryId = runtimeFilterB.getQueryId();
    List<String> probeFields = runtimeFilterB.getProbeFieldsList();
    List<Integer> sizeInBytes = runtimeFilterB.getBloomFilterSizeInBytesList();
    long rfIdentifier = runtimeFilterB.getRfIdentifier();
    DrillBuf[] data = srcRuntimeFilterWritable.getData();
    List<CoordinationProtos.DrillbitEndpoint> scanNodeEps = joinMjId2probeScanEps.get(joinMajorId);
    int scanNodeSize = scanNodeEps.size();
    srcRuntimeFilterWritable.retainBuffers(scanNodeSize - 1);
    int scanNodeMjId = joinMjId2ScanMjId.get(joinMajorId);
    for (int minorId = 0; minorId < scanNodeEps.size(); minorId++) {
      BitData.RuntimeFilterBDef.Builder builder = BitData.RuntimeFilterBDef.newBuilder();
      for (String probeField : probeFields) {
        builder.addProbeFields(probeField);
      }
      BitData.RuntimeFilterBDef runtimeFilterBDef = builder.setQueryId(queryId)
                                                           .setMajorFragmentId(scanNodeMjId)
                                                           .setMinorFragmentId(minorId)
                                                           .setToForeman(false)
                                                           .setRfIdentifier(rfIdentifier)
                                                           .addAllBloomFilterSizeInBytes(sizeInBytes)
                                                           .build();
      RuntimeFilterWritable runtimeFilterWritable = new RuntimeFilterWritable(runtimeFilterBDef, data);
      CoordinationProtos.DrillbitEndpoint drillbitEndpoint = scanNodeEps.get(minorId);

      DataTunnel dataTunnel = drillbitContext.getDataConnectionsPool().getTunnel(drillbitEndpoint);
      Consumer<RpcException> exceptionConsumer = new Consumer<RpcException>()
      {
        @Override
        public void accept(final RpcException e)
        {
          logger.warn("fail to broadcast a runtime filter to the probe side scan node", e);
        }

        @Override
        public void interrupt(final InterruptedException e)
        {
          logger.warn("fail to broadcast a runtime filter to the probe side scan node", e);
        }
      };
      RpcOutcomeListener<BitData.AckWithCredit> statusHandler = new DataTunnelStatusHandler(exceptionConsumer, sendingAccountor);
      AccountingDataTunnel accountingDataTunnel = new AccountingDataTunnel(dataTunnel, sendingAccountor, statusHandler);
      accountingDataTunnel.sendRuntimeFilter(runtimeFilterWritable);
    }
  }

  public void setJoinMjId2rfNumber(Map<Integer, Integer> joinMjId2rfNumber)
  {
    this.joinMjId2rfNumber = joinMjId2rfNumber;
  }

  public void setJoinMjId2probeScanEps(Map<Integer, List<CoordinationProtos.DrillbitEndpoint>> joinMjId2probeScanEps)
  {
    this.joinMjId2probeScanEps = joinMjId2probeScanEps;
  }

  public void setJoinMjId2ScanMjId(Map<Integer, Integer> joinMjId2ScanMjId)
  {
    this.joinMjId2ScanMjId = joinMjId2ScanMjId;
  }

  private class AsyncAggregateWorker implements Runnable
  {
    private AtomicBoolean over = new AtomicBoolean(false);

    @Override
    public void run()
    {
      while ((joinMjId2rfNumber == null || !joinMjId2rfNumber.isEmpty() ) && running.get()) {
        RuntimeFilterWritable toAggregate = null;
        synchronized (rfQueue) {
          try {
            toAggregate = rfQueue.poll();
            while (toAggregate == null && running.get()) {
              rfQueue.wait();
              toAggregate = rfQueue.poll();
            }
          } catch (InterruptedException ex) {
            logger.error("RFW_Aggregator thread being interrupted", ex);
            continue;
          }
        }
        if (toAggregate == null) {
          continue;
        }
        // perform aggregate outside the sync block.
        try {
          aggregate(toAggregate);
        } catch (Exception ex) {
          logger.error("Failed to aggregate or route the RFW", ex);

          // Set running to false and cleanup pending RFW in queue. This will make sure producer
          // thread is also indicated to stop and queue is cleaned up properly in failure cases
          synchronized (rfQueue) {
            running.set(false);
          }
          cleanupQueue();
          throw new DrillRuntimeException(ex);
        } finally {
            toAggregate.close();
        }
      }
      cleanupQueue();
    }

    private void cleanupQueue() {
      if (!running.get()) {
        RuntimeFilterWritable toClose;
        while ((toClose = rfQueue.poll()) != null) {
          toClose.close();
        }
      }
      over.set(true);
    }
  }
}
