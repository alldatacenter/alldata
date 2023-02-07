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
package org.apache.drill.exec.physical.impl.broadcastsender;

import java.util.List;

import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.ops.AccountingDataTunnel;
import org.apache.drill.exec.ops.MetricDef;
import org.apache.drill.exec.ops.RootFragmentContext;
import org.apache.drill.exec.physical.MinorFragmentEndpoint;
import org.apache.drill.exec.physical.config.BroadcastSender;
import org.apache.drill.exec.physical.impl.BaseRootExec;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.proto.ExecProtos;
import org.apache.drill.exec.record.FragmentWritableBatch;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.WritableBatch;

import org.apache.drill.shaded.guava.com.google.common.collect.ArrayListMultimap;

/**
 * Broadcast Sender broadcasts incoming batches to all receivers (one or more).
 * This is useful in cases such as broadcast join where sending the entire table to join
 * to all nodes is cheaper than merging and computing all the joins in the same node.
 */
public class BroadcastSenderRootExec extends BaseRootExec {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BroadcastSenderRootExec.class);
  private final BroadcastSender config;
  private final int[][] receivingMinorFragments;
  private final AccountingDataTunnel[] tunnels;
  private final ExecProtos.FragmentHandle handle;
  private volatile boolean ok;
  private final RecordBatch incoming;

  public enum Metric implements MetricDef {
    N_RECEIVERS,
    BYTES_SENT;
    @Override
    public int metricId() {
      return ordinal();
    }
  }

  public BroadcastSenderRootExec(RootFragmentContext context,
                                 RecordBatch incoming,
                                 BroadcastSender config) throws OutOfMemoryException {
    super(context, context.newOperatorContext(config, null), config);
    this.ok = true;
    this.incoming = incoming;
    this.config = config;
    this.handle = context.getHandle();
    List<MinorFragmentEndpoint> destinations = config.getDestinations();
    ArrayListMultimap<DrillbitEndpoint, Integer> dests = ArrayListMultimap.create();

    for(MinorFragmentEndpoint destination : destinations) {
      dests.put(destination.getEndpoint(), destination.getId());
    }

    int destCount = dests.keySet().size();
    int i = 0;

    this.tunnels = new AccountingDataTunnel[destCount];
    this.receivingMinorFragments = new int[destCount][];
    for(final DrillbitEndpoint ep : dests.keySet()){
      List<Integer> minorsList= dests.get(ep);
      int[] minorsArray = new int[minorsList.size()];
      int x = 0;
      for(Integer m : minorsList){
        minorsArray[x++] = m;
      }
      receivingMinorFragments[i] = minorsArray;
      tunnels[i] = context.getDataTunnel(ep);
      i++;
    }
  }

  @Override
  public boolean innerNext() {
    RecordBatch.IterOutcome out = next(incoming);
    logger.debug("Outcome of sender next {}", out);
    switch(out){
      case NONE:
        for (int i = 0; i < tunnels.length; ++i) {
          FragmentWritableBatch b2 = FragmentWritableBatch.getEmptyLast(
              handle.getQueryId(),
              handle.getMajorFragmentId(),
              handle.getMinorFragmentId(),
              config.getOppositeMajorFragmentId(),
              receivingMinorFragments[i]);
          stats.startWait();
          try {
            tunnels[i].sendRecordBatch(b2);
          } finally {
            stats.stopWait();
          }
        }
        return false;

      case OK_NEW_SCHEMA:
      case OK:
        WritableBatch writableBatch = incoming.getWritableBatch().transfer(oContext.getAllocator());
        if (tunnels.length > 1) {
          writableBatch.retainBuffers(tunnels.length - 1);
        }
        for (int i = 0; i < tunnels.length; ++i) {
          FragmentWritableBatch batch = new FragmentWritableBatch(
              false,
              handle.getQueryId(),
              handle.getMajorFragmentId(),
              handle.getMinorFragmentId(),
              config.getOppositeMajorFragmentId(),
              receivingMinorFragments[i],
              writableBatch);
          updateStats(batch);
          stats.startWait();
          try {
            tunnels[i].sendRecordBatch(batch);
          } finally {
            stats.stopWait();
          }
        }

        return ok;

      case NOT_YET:
      default:
        throw new IllegalStateException();
    }
  }

  public void updateStats(FragmentWritableBatch writableBatch) {
    stats.setLongStat(Metric.N_RECEIVERS, tunnels.length);
    stats.addLongStat(Metric.BYTES_SENT, writableBatch.getByteCount());
  }
}
