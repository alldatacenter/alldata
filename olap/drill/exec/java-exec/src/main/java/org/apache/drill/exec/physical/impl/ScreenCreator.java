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

import org.apache.drill.exec.ops.AccountingUserConnection;
import org.apache.drill.exec.ops.ExecutorFragmentContext;
import org.apache.drill.exec.ops.MetricDef;
import org.apache.drill.exec.ops.RootFragmentContext;
import org.apache.drill.exec.physical.config.Screen;
import org.apache.drill.exec.physical.impl.materialize.QueryDataPackage.DataPackage;
import org.apache.drill.exec.physical.impl.materialize.QueryDataPackage.EmptyResultsPackage;
import org.apache.drill.exec.physical.impl.materialize.VectorRecordMaterializer;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.RecordBatch.IterOutcome;
import org.apache.drill.exec.testing.ControlsInjector;
import org.apache.drill.exec.testing.ControlsInjectorFactory;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScreenCreator implements RootCreator<Screen> {
  private static final ControlsInjector injector = ControlsInjectorFactory.getInjector(ScreenCreator.class);

  @Override
  public RootExec getRoot(ExecutorFragmentContext context, Screen config, List<RecordBatch> children) {
    Preconditions.checkNotNull(children);
    Preconditions.checkArgument(children.size() == 1);
    return new ScreenRoot(context, children.iterator().next(), config);
  }

  /**
   * Transfer batches to a user connection. The user connection is typically a
   * network connection, but may be internal for a web or REST client. Data is
   * sent as a "package", allowing the network client to request serialization,
   * and the internal client to just transfer buffer ownership.
   */
  public static class ScreenRoot extends BaseRootExec {
    private static final Logger logger = LoggerFactory.getLogger(ScreenRoot.class);
    private final RecordBatch incoming;
    private final RootFragmentContext context;
    private final AccountingUserConnection userConnection;
    private DataPackage dataPackage;
    private boolean firstBatch = true;

    public enum Metric implements MetricDef {
      BYTES_SENT;

      @Override
      public int metricId() {
        return ordinal();
      }
    }

    public ScreenRoot(RootFragmentContext context, RecordBatch incoming, Screen config) {
      super(context, config);
      this.context = context;
      this.incoming = incoming;
      this.userConnection = context.getUserDataTunnel();
    }

    @Override
    public boolean innerNext() {
      IterOutcome outcome = next(incoming);
      logger.trace("Screen Outcome {}", outcome);
      switch (outcome) {
        case NONE:
          if (firstBatch) {

            stats.startWait();
            try {
              // This is the only data message sent to the client and does not contain the schema
              userConnection.sendData(new EmptyResultsPackage(context.getHandle().getQueryId()));
            } finally {
              stats.stopWait();
            }
            firstBatch = false; // we don't really need to set this. But who knows!
          }
          return false;

        case OK_NEW_SCHEMA:
          dataPackage = new DataPackage(new VectorRecordMaterializer(context, oContext, incoming), stats);
          //$FALL-THROUGH$
        case OK:
          injector.injectPause(context.getExecutionControls(), "sending-data", logger);
          stats.startWait();
          try {
            // Stats updated if connection serializes the batch
            userConnection.sendData(dataPackage);
          } finally {
            stats.stopWait();
          }
          firstBatch = false;
          return true;

        default:
          throw new UnsupportedOperationException(outcome.name());
      }
    }

    public RootFragmentContext getContext() { return context; }
    protected RecordBatch getIncoming() { return incoming; }

    @Override
    public void close() throws Exception {
      injector.injectPause(context.getExecutionControls(), "send-complete", logger);
      super.close();
    }
  }
}
