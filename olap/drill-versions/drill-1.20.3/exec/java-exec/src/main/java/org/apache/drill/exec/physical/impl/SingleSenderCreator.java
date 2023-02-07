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
package org.apache.drill.exec.physical.impl;

import java.util.List;

import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.ops.AccountingDataTunnel;
import org.apache.drill.exec.ops.ExecutorFragmentContext;
import org.apache.drill.exec.ops.MetricDef;
import org.apache.drill.exec.ops.RootFragmentContext;
import org.apache.drill.exec.physical.config.SingleSender;
import org.apache.drill.exec.proto.ExecProtos.FragmentHandle;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.FragmentWritableBatch;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.RecordBatch.IterOutcome;
import org.apache.drill.exec.testing.ControlsInjector;
import org.apache.drill.exec.testing.ControlsInjectorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingleSenderCreator implements RootCreator<SingleSender>{

  @Override
  public RootExec getRoot(ExecutorFragmentContext context, SingleSender config, List<RecordBatch> children) {
    assert children != null && children.size() == 1;
    return new SingleSenderRootExec(context, children.iterator().next(), config);
  }

  public static class SingleSenderRootExec extends BaseRootExec {
    private static final Logger logger = LoggerFactory.getLogger(SingleSenderRootExec.class);
    private static final ControlsInjector injector = ControlsInjectorFactory.getInjector(SingleSenderRootExec.class);

    private final FragmentHandle oppositeHandle;

    private final RecordBatch incoming;
    private AccountingDataTunnel tunnel;
    private final FragmentHandle handle;
    private final int recMajor;
    private volatile boolean done = false;

    public enum Metric implements MetricDef {
      BYTES_SENT;

      @Override
      public int metricId() {
        return ordinal();
      }
    }

    public SingleSenderRootExec(RootFragmentContext context, RecordBatch batch, SingleSender config) throws OutOfMemoryException {
      super(context, context.newOperatorContext(config, null), config);
      this.incoming = batch;
      assert incoming != null;
      handle = context.getHandle();
      recMajor = config.getOppositeMajorFragmentId();
      tunnel = context.getDataTunnel(config.getDestination());
      oppositeHandle = handle.toBuilder()
          .setMajorFragmentId(config.getOppositeMajorFragmentId())
          .setMinorFragmentId(config.getOppositeMinorFragmentId())
          .build();
      tunnel = context.getDataTunnel(config.getDestination());
      tunnel.setTestInjectionControls(injector, context.getExecutionControls(), logger);
    }

    @Override
    public boolean innerNext() {

      IterOutcome out;
      if (!done) {
        out = next(incoming);
      } else {
        incoming.cancel();
        out = IterOutcome.NONE;
      }
      switch (out) {
      case NONE:
        // if we didn't do anything yet, send an empty schema.
        final BatchSchema sendSchema = incoming.getSchema() == null ?
            BatchSchema.newBuilder().build() : incoming.getSchema();

        final FragmentWritableBatch b2 = FragmentWritableBatch.getEmptyLastWithSchema(handle.getQueryId(),
            handle.getMajorFragmentId(), handle.getMinorFragmentId(), recMajor, oppositeHandle.getMinorFragmentId(),
            sendSchema);
        stats.startWait();
        try {
          tunnel.sendRecordBatch(b2);
        } finally {
          stats.stopWait();
        }
        return false;

      case OK_NEW_SCHEMA:
      case OK:
        final FragmentWritableBatch batch = new FragmentWritableBatch(
            false, handle.getQueryId(), handle.getMajorFragmentId(),
            handle.getMinorFragmentId(), recMajor, oppositeHandle.getMinorFragmentId(),
            incoming.getWritableBatch().transfer(oContext.getAllocator()));
        updateStats(batch);
        stats.startWait();
        try {
          tunnel.sendRecordBatch(batch);
        } finally {
          stats.stopWait();
        }
        return true;

      case NOT_YET:
      default:
        throw new IllegalStateException();
      }
    }

    public void updateStats(FragmentWritableBatch writableBatch) {
      stats.addLongStat(Metric.BYTES_SENT, writableBatch.getByteCount());
    }

    @Override
    public void receivingFragmentFinished(FragmentHandle handle) {
      done = true;
    }
  }
}
