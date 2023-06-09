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
package org.apache.drill.exec.physical.impl.unorderedreceiver;

import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.util.Iterator;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.ops.ExchangeFragmentContext;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.ops.MetricDef;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.ops.OperatorStats;
import org.apache.drill.exec.physical.MinorFragmentEndpoint;
import org.apache.drill.exec.physical.config.UnorderedReceiver;
import org.apache.drill.exec.proto.BitControl.FinishedReceiver;
import org.apache.drill.exec.proto.ExecProtos.FragmentHandle;
import org.apache.drill.exec.proto.GeneralRPCProtos.Ack;
import org.apache.drill.exec.proto.UserBitShared.RecordBatchDef;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.CloseableRecordBatch;
import org.apache.drill.exec.record.RawFragmentBatch;
import org.apache.drill.exec.record.RawFragmentBatchProvider;
import org.apache.drill.exec.record.RecordBatchLoader;
import org.apache.drill.exec.record.TypedFieldId;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.record.WritableBatch;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.exec.record.selection.SelectionVector4;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.exec.rpc.RpcOutcomeListener;
import org.apache.drill.exec.testing.ControlsInjector;
import org.apache.drill.exec.testing.ControlsInjectorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnorderedReceiverBatch implements CloseableRecordBatch {
  private static final Logger logger = LoggerFactory.getLogger(UnorderedReceiverBatch.class);
  private static final ControlsInjector injector = ControlsInjectorFactory.getInjector(UnorderedReceiverBatch.class);

  private final RecordBatchLoader batchLoader;
  private final RawFragmentBatchProvider fragProvider;
  private final ExchangeFragmentContext context;
  private BatchSchema schema;
  private final OperatorStats stats;
  private boolean first = true;
  private final UnorderedReceiver config;
  private final OperatorContext oContext;
  // Represents last outcome of next(). If an Exception is thrown
  // during the method's execution a value IterOutcome.STOP will be assigned.
  private IterOutcome lastOutcome;

  public enum Metric implements MetricDef {
    BYTES_RECEIVED,
    NUM_SENDERS;

    @Override
    public int metricId() {
      return ordinal();
    }
  }

  public UnorderedReceiverBatch(ExchangeFragmentContext context,
      RawFragmentBatchProvider fragProvider, UnorderedReceiver config)
          throws OutOfMemoryException {
    this.fragProvider = fragProvider;
    this.context = context;
    // In normal case, batchLoader does not require an allocator. However, in
    // case of splitAndTransfer of a value vector,
    // we may need an allocator for the new offset vector. Therefore, here we
    // pass the context's allocator to batchLoader.
    oContext = context.newOperatorContext(config);
    this.batchLoader = new RecordBatchLoader(oContext.getAllocator());

    this.stats = oContext.getStats();
    this.stats.setLongStat(Metric.NUM_SENDERS, config.getNumSenders());
    this.config = config;

    // Register this operator's buffer allocator so that incoming buffers are
    // owned by this allocator
    context.getBuffers().getCollector(config.getOppositeMajorFragmentId())
      .setAllocator(oContext.getAllocator());
  }

  @Override
  public FragmentContext getContext() {
    return context;
  }

  @Override
  public BatchSchema getSchema() {
    return schema;
  }

  @Override
  public int getRecordCount() {
    return batchLoader.getRecordCount();
  }

  @Override
  public void cancel() {
    informSenders();
    fragProvider.kill(context);
  }

  @Override
  public Iterator<VectorWrapper<?>> iterator() {
    return batchLoader.iterator();
  }

  @Override
  public SelectionVector2 getSelectionVector2() {
    throw new UnsupportedOperationException();
  }

  @Override
  public SelectionVector4 getSelectionVector4() {
    throw new UnsupportedOperationException();
  }

  @Override
  public TypedFieldId getValueVectorId(SchemaPath path) {
    return batchLoader.getValueVectorId(path);
  }

  @Override
  public VectorWrapper<?> getValueAccessorById(Class<?> clazz, int... ids) {
    return batchLoader.getValueAccessorById(clazz, ids);
  }

  private RawFragmentBatch getNextBatch() {
    try {
      injector.injectInterruptiblePause(context.getExecutionControls(), "waiting-for-data", logger);
      return fragProvider.getNext();
    } catch(InterruptedException e) {
      // Preserve evidence that the interruption occurred so that code higher up
      // on the call stack can learn of the
      // interruption and respond to it if it wants to.
      Thread.currentThread().interrupt();

      return null;
    } catch (IOException e) {
      throw UserException.dataReadError(e)
          .addContext("Failure when reading incoming batch")
          .build(logger);
    }
  }

  @Override
  public IterOutcome next() {
    batchLoader.resetRecordCount();
    stats.startProcessing();
    try {
      RawFragmentBatch batch;
      try {
        stats.startWait();
        batch = getNextBatch();

        // skip over empty batches. we do this since these are basically control messages.
        while (batch != null && batch.getHeader().getDef().getRecordCount() == 0
            && (!first || batch.getHeader().getDef().getFieldCount() == 0)) {
          batch = getNextBatch();
        }
      } finally {
        stats.stopWait();
      }

      first = false;

      if (batch == null) {
        lastOutcome = IterOutcome.NONE;
        batchLoader.zero();
        context.getExecutorState().checkContinue();
        return lastOutcome;
      }

      if (context.getAllocator().isOverLimit()) {
        context.requestMemory(this);
        if (context.getAllocator().isOverLimit()) {
          throw new OutOfMemoryException("Allocator over limit");
        }
      }

      RecordBatchDef rbd = batch.getHeader().getDef();
      boolean schemaChanged = batchLoader.load(rbd, batch.getBody());
      stats.addLongStat(Metric.BYTES_RECEIVED, batch.getByteCount());

      batch.release();
      if (schemaChanged) {
        this.schema = batchLoader.getSchema();
        stats.batchReceived(0, rbd.getRecordCount(), true);
        lastOutcome = IterOutcome.OK_NEW_SCHEMA;
      } else {
        stats.batchReceived(0, rbd.getRecordCount(), false);
        lastOutcome = IterOutcome.OK;
      }
      return lastOutcome;
    } finally {
      stats.stopProcessing();
    }
  }

  @Override
  public WritableBatch getWritableBatch() {
    return batchLoader.getWritableBatch();
  }

  @Override
  public void close() {
    logger.debug("Closing {}", getClass().getCanonicalName());
    batchLoader.clear();
  }

  @Override
  public VectorContainer getOutgoingContainer() {
    throw new UnsupportedOperationException(
        String.format("You should not call getOutgoingContainer() for class %s",
            getClass().getCanonicalName()));
  }

  @Override
  public VectorContainer getContainer() {
    return batchLoader.getContainer();
  }

  private void informSenders() {
    logger.info("Informing senders of request to terminate sending.");
    FragmentHandle handlePrototype = FragmentHandle.newBuilder()
            .setMajorFragmentId(config.getOppositeMajorFragmentId())
            .setQueryId(context.getHandle().getQueryId())
            .build();
    for (MinorFragmentEndpoint providingEndpoint : config.getProvidingEndpoints()) {
      FragmentHandle sender = FragmentHandle.newBuilder(handlePrototype)
              .setMinorFragmentId(providingEndpoint.getId())
              .build();
      FinishedReceiver finishedReceiver = FinishedReceiver.newBuilder()
              .setReceiver(context.getHandle())
              .setSender(sender)
              .build();
      context.getController()
        .getTunnel(providingEndpoint.getEndpoint())
        .informReceiverFinished(new OutcomeListener(), finishedReceiver);
    }
  }

  // TODO: Code duplication. MergingRecordBatch has the same implementation.
  private class OutcomeListener implements RpcOutcomeListener<Ack> {

    @Override
    public void failed(RpcException ex) {
      logger.warn("Failed to inform upstream that receiver is finished");
    }

    @Override
    public void success(Ack value, ByteBuf buffer) {
      // Do nothing
    }

    @Override
    public void interrupted(InterruptedException e) {
      logger.debug("{} interrupted. shouldContinue value: {}", getClass().getCanonicalName(),
        context.getExecutorState().shouldContinue());
      if (context.getExecutorState().shouldContinue()) {
        String errMsg = "Received an interrupt RPC outcome while sending ReceiverFinished message";
        logger.error(errMsg, e);
        context.getExecutorState().fail(new RpcException(errMsg, e));
      }
    }
  }

  @Override
  public void dump() {
    logger.error("UnorderedReceiverBatch[batchLoader={}, schema={}]", batchLoader, schema);
  }
}
