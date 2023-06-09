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
package org.apache.drill.exec.rpc.data;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.DrillBuf;
import org.apache.drill.exec.exception.FragmentSetupException;
import org.apache.drill.exec.proto.BitData;
import org.apache.drill.exec.proto.BitData.FragmentRecordBatch;
import org.apache.drill.exec.proto.BitData.RpcType;
import org.apache.drill.exec.proto.ExecProtos.FragmentHandle;
import org.apache.drill.exec.proto.helper.QueryIdHelper;
import org.apache.drill.exec.rpc.RequestHandler;
import org.apache.drill.exec.rpc.ResponseSender;
import org.apache.drill.exec.rpc.RpcBus;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.exec.rpc.control.WorkEventBus;
import org.apache.drill.exec.work.WorkManager;
import org.apache.drill.exec.work.filter.RuntimeFilterWritable;
import org.apache.drill.exec.work.fragment.FragmentManager;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

// package private
class DataServerRequestHandler implements RequestHandler<DataServerConnection> {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DataServerRequestHandler.class);

  private final WorkEventBus workBus;
  private final WorkManager.WorkerBee bee;

  public DataServerRequestHandler(WorkEventBus workBus, WorkManager.WorkerBee bee) {
    this.workBus = workBus;
    this.bee = bee;
  }

  @Override
  public void handle(DataServerConnection connection, int rpcType, ByteBuf pBody, ByteBuf dBody,
                     ResponseSender sender) throws RpcException {
    switch (rpcType) {
      case RpcType.REQ_RUNTIME_FILTER_VALUE : {
        handleRuntimeFilterRequest(pBody, dBody, sender);
        break;
      }

      case RpcType.REQ_RECORD_BATCH_VALUE : {
        handleRecordBatchRequest(pBody, dBody, sender);
        break;
      }

      default:
        throw new RpcException("Not yet supported.");
    }
  }

  private void handleRecordBatchRequest(ByteBuf pBody, ByteBuf dBody, ResponseSender sender) throws RpcException {
    final FragmentRecordBatch fragmentBatch = RpcBus.get(pBody, FragmentRecordBatch.PARSER);
    final AckSender ack = new AckSender(sender);

    // increment so we don't get false returns.
    ack.increment();

    try {
      final IncomingDataBatch batch = new IncomingDataBatch(fragmentBatch, (DrillBuf) dBody, ack);
      final int targetCount = fragmentBatch.getReceivingMinorFragmentIdCount();

      // randomize who gets first transfer (and thus ownership) so memory usage is balanced when we're sharing amongst
      // multiple fragments.
      final int firstOwner = ThreadLocalRandom.current().nextInt(targetCount);
      submit(batch, firstOwner, targetCount);
      submit(batch, 0, firstOwner);

    } catch (IOException | FragmentSetupException e) {
      logger.error("Failure while getting fragment manager. {}",
        QueryIdHelper.getQueryIdentifiers(fragmentBatch.getQueryId(),
          fragmentBatch.getReceivingMajorFragmentId(),
          fragmentBatch.getReceivingMinorFragmentIdList()), e);
      ack.clear();
      ack.sendFail();
    } finally {

      // decrement the extra reference we grabbed at the top.
      ack.sendOk();
    }
  }

  private void handleRuntimeFilterRequest(ByteBuf pBody, ByteBuf dBody, ResponseSender sender) throws RpcException {
    BitData.RuntimeFilterBDef runtimeFilterBDef = RpcBus.get(pBody, BitData.RuntimeFilterBDef.PARSER);
    if (dBody == null) {
      return;
    }
    List<Integer> bfSizeInBytes = runtimeFilterBDef.getBloomFilterSizeInBytesList();
    int boomFilterNum = bfSizeInBytes.size();
    DrillBuf data = (DrillBuf) dBody;
    DrillBuf[] bufs = new DrillBuf[boomFilterNum];
    int index = 0;
    for (int i = 0; i < boomFilterNum; i++) {
      int length = bfSizeInBytes.get(i);
      bufs[i] = data.slice(index, length);
      index = index + length;
    }
    RuntimeFilterWritable runtimeFilterWritable = new RuntimeFilterWritable(runtimeFilterBDef, bufs);
    AckSender ackSender = new AckSender(sender);
    ackSender.increment();
    try {
      // hand to WorkerBee to solve the received RuntimeFilter, the receiver maybe Foreman or a scan node.
      bee.receiveRuntimeFilter(runtimeFilterWritable);
    } catch (Exception e) {
      logger.error("error to solve received runtime filter, {}",
        QueryIdHelper.getQueryId(runtimeFilterBDef.getQueryId()), e);
      ackSender.clear();
      ackSender.sendFail();
    } finally {
      ackSender.sendOk();
    }
  }

  private void submit(IncomingDataBatch batch, int minorStart, int minorStopExclusive) throws FragmentSetupException,
      IOException {
    for (int minor = minorStart; minor < minorStopExclusive; minor++) {
      final FragmentManager manager = workBus.getFragmentManager(getHandle(batch.getHeader(), minor));
      if (manager == null) {
        // A missing manager means the query already terminated. We can simply drop this data.
        continue;
      }

      final boolean canRun = manager.handle(batch);
      if (canRun) {
        // logger.debug("Arriving batch means local batch can run, starting local batch.");
        /*
         * If we've reached the canRun threshold, we'll proceed. This expects manager.handle() to only return a single
         * true. This is guaranteed by the interface.
         */
        bee.startFragmentPendingRemote(manager);
      }
    }
  }

  private static FragmentHandle getHandle(final FragmentRecordBatch batch, int index) {
    return FragmentHandle.newBuilder()
        .setQueryId(batch.getQueryId())
        .setMajorFragmentId(batch.getReceivingMajorFragmentId())
        .setMinorFragmentId(batch.getReceivingMinorFragmentId(index))
        .build();
  }

}
