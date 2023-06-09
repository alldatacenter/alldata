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

import com.google.protobuf.MessageLite;
import io.netty.buffer.ByteBuf;
import org.apache.drill.exec.proto.BitData;
import org.apache.drill.exec.proto.BitData.RpcType;
import org.apache.drill.exec.record.FragmentWritableBatch;
import org.apache.drill.exec.rpc.DynamicSemaphore;
import org.apache.drill.exec.rpc.ListeningCommand;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.exec.rpc.RpcOutcomeListener;
import org.apache.drill.exec.testing.ControlsInjector;
import org.apache.drill.exec.testing.ExecutionControls;
import org.apache.drill.exec.work.filter.RuntimeFilterWritable;


public class DataTunnel {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DataTunnel.class);

  private final DataConnectionManager manager;
  private final DynamicSemaphore sendingSemaphore = new DynamicSemaphore();

  // Needed for injecting a test pause
  private boolean isInjectionControlSet;
  private ControlsInjector testInjector;
  private ExecutionControls testControls;
  private org.slf4j.Logger testLogger;

  public DataTunnel(DataConnectionManager manager) {
    this.manager = manager;
  }

  /**
   * Once a DataTunnel is created, clients of DataTunnel can pass injection controls to enable setting injections at
   * pre-defined places. Currently following injection sites are available.
   *
   * 1. In method {@link #sendRecordBatch(RpcOutcomeListener, FragmentWritableBatch)}, an interruptible pause injection
   *    is available before acquiring the sending slot. Site name is: "data-tunnel-send-batch-wait-for-interrupt"
   *
   * @param testInjector
   * @param testControls
   * @param testLogger
   */
  public void setTestInjectionControls(final ControlsInjector testInjector,
      final ExecutionControls testControls, final org.slf4j.Logger testLogger) {
    isInjectionControlSet = true;
    this.testInjector = testInjector;
    this.testControls = testControls;
    this.testLogger = testLogger;
  }

  public void sendRecordBatch(RpcOutcomeListener<BitData.AckWithCredit> outcomeListener, FragmentWritableBatch batch) {
    SendBatchAsyncListen b = new SendBatchAsyncListen(outcomeListener, batch);
    try {
      if (isInjectionControlSet) {
        // Wait for interruption if set. Used to simulate the fragment interruption while the fragment is waiting for
        // semaphore acquire.
        testInjector.injectInterruptiblePause(testControls, "data-tunnel-send-batch-wait-for-interrupt", testLogger);
      }

      sendingSemaphore.acquire();
      manager.runCommand(b);
    } catch (final InterruptedException e) {
      // Release the buffers first before informing the listener about the interrupt.
      for (ByteBuf buffer : batch.getBuffers()) {
        buffer.release();
      }

      outcomeListener.interrupted(e);

      // Preserve evidence that the interruption occurred so that code higher up on the call stack can learn of the
      // interruption and respond to it if it wants to.
      Thread.currentThread().interrupt();
    }
  }

  public void sendRuntimeFilter(RpcOutcomeListener<BitData.AckWithCredit> outcomeListener, RuntimeFilterWritable runtimeFilter) {
    SendRuntimeFilterAsyncListen cmd = new SendRuntimeFilterAsyncListen(outcomeListener, runtimeFilter);
    try{
      if (isInjectionControlSet) {
        // Wait for interruption if set. Used to simulate the fragment interruption while the fragment is waiting for
        // semaphore acquire.
        testInjector.injectInterruptiblePause(testControls, "data-tunnel-send-runtime_filter-wait-for-interrupt", testLogger);
      }

      manager.runCommand(cmd);
    } catch(final InterruptedException e){
      // Release the buffers first before informing the listener about the interrupt.
      runtimeFilter.close();
      outcomeListener.interrupted(e);
      // Preserve evidence that the interruption occurred so that code higher up on the call stack can learn of the
      // interruption and respond to it if it wants to.
      Thread.currentThread().interrupt();
    }
  }

  private class ThrottlingOutcomeListener implements RpcOutcomeListener<BitData.AckWithCredit>{
    RpcOutcomeListener<BitData.AckWithCredit> inner;

    public ThrottlingOutcomeListener(RpcOutcomeListener<BitData.AckWithCredit> inner) {
      super();
      this.inner = inner;
    }

    @Override
    public void failed(RpcException ex) {
      sendingSemaphore.release();
      inner.failed(ex);
    }

    @Override
    public void success(BitData.AckWithCredit value, ByteBuf buffer) {
      int credit = value.getAllowedCredit();
      if (credit > 0) {
        //received an explicit runtime advice to transfer to the new credit
        sendingSemaphore.tryToIncreaseCredit(credit);
      }
      sendingSemaphore.release();
      inner.success(value, buffer);
    }

    @Override
    public void interrupted(InterruptedException e) {
      sendingSemaphore.release();
      inner.interrupted(e);
    }
  }

  private class SendBatchAsyncListen extends ListeningCommand<BitData.AckWithCredit, DataClientConnection, RpcType, MessageLite> {
    final FragmentWritableBatch batch;

    public SendBatchAsyncListen(RpcOutcomeListener<BitData.AckWithCredit> listener, FragmentWritableBatch batch) {
      super(listener);
      this.batch = batch;
    }

    @Override
    public void doRpcCall(RpcOutcomeListener<BitData.AckWithCredit> outcomeListener, DataClientConnection connection) {
      connection.send(new ThrottlingOutcomeListener(outcomeListener), getRpcType(), batch.getHeader(),
        BitData.AckWithCredit.class, batch.getBuffers());
    }

    @Override
    public RpcType getRpcType() {
      return RpcType.REQ_RECORD_BATCH;
    }

    @Override
    public MessageLite getMessage() {
      return batch.getHeader();
    }

    @Override
    public String toString() {
      return "SendBatch [batch.header=" + batch.getHeader() + "]";
    }

    @Override
    public void connectionFailed(FailureType type, Throwable t) {
      for (ByteBuf buffer : batch.getBuffers()) {
        buffer.release();
      }
      super.connectionFailed(type, t);
    }
  }

  private class SendRuntimeFilterAsyncListen extends ListeningCommand<BitData.AckWithCredit, DataClientConnection, RpcType, MessageLite> {
    final RuntimeFilterWritable runtimeFilter;

    public SendRuntimeFilterAsyncListen(RpcOutcomeListener<BitData.AckWithCredit> listener, RuntimeFilterWritable runtimeFilter) {
      super(listener);
      this.runtimeFilter = runtimeFilter;
    }

    @Override
    public void doRpcCall(RpcOutcomeListener<BitData.AckWithCredit> outcomeListener, DataClientConnection connection) {
      connection.send(outcomeListener, RpcType.REQ_RUNTIME_FILTER, runtimeFilter.getRuntimeFilterBDef(), BitData.AckWithCredit.class, runtimeFilter.getData());
    }

    @Override
    public RpcType getRpcType() {
      return RpcType.REQ_RUNTIME_FILTER;
    }

    @Override
    public MessageLite getMessage() {
      return runtimeFilter.getRuntimeFilterBDef();
    }

    @Override
    public void connectionFailed(FailureType type, Throwable t) {
      runtimeFilter.close();
      super.connectionFailed(type, t);
    }
  }
}
