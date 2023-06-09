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

package com.netease.arctic.flink.read.hybrid.enumerator;

import com.netease.arctic.flink.read.FlinkSplitPlanner;
import com.netease.arctic.flink.read.hybrid.assigner.SplitAssigner;
import com.netease.arctic.flink.read.hybrid.split.ArcticSplit;
import com.netease.arctic.flink.read.source.ArcticScanContext;
import com.netease.arctic.flink.table.ArcticTableLoader;
import com.netease.arctic.table.KeyedTable;
import org.apache.flink.api.connector.source.SplitEnumeratorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.netease.arctic.flink.util.ArcticUtils.loadArcticTable;

/**
 * This is a static arctic source enumerator, used for bounded source scan.
 * Working enabled only just {@link ScanContext#STREAMING} is equal to false.
 */
public class StaticArcticSourceEnumerator extends AbstractArcticEnumerator {
  private static final Logger LOG = LoggerFactory.getLogger(StaticArcticSourceEnumerator.class);
  private final SplitAssigner assigner;
  private final ArcticTableLoader loader;
  private transient KeyedTable keyedTable;
  private final ArcticScanContext scanContext;
  private final boolean shouldEnumerate;

  public StaticArcticSourceEnumerator(
      SplitEnumeratorContext<ArcticSplit> enumeratorContext,
      SplitAssigner assigner,
      ArcticTableLoader loader,
      ArcticScanContext scanContext,
      @Nullable ArcticSourceEnumState enumState) {
    super(enumeratorContext, assigner);
    this.loader = loader;
    this.assigner = assigner;
    this.scanContext = scanContext;
    // split enumeration is not needed during restore scenario
    this.shouldEnumerate = enumState == null;
  }

  @Override
  public void start() {
    super.start();
    if (keyedTable == null) {
      keyedTable = loadArcticTable(loader).asKeyedTable();
    }
    if (shouldEnumerate) {
      keyedTable.baseTable().refresh();
      keyedTable.changeTable().refresh();
      List<ArcticSplit> splits = FlinkSplitPlanner.planFullTable(keyedTable, new AtomicInteger());
      assigner.onDiscoveredSplits(splits);
      LOG.info("Discovered {} splits from table {} during job initialization",
          splits.size(), keyedTable.name());
    }
  }

  @Override
  protected boolean shouldWaitForMoreSplits() {
    return false;
  }

  @Override
  public ArcticSourceEnumState snapshotState(long checkpointId) throws Exception {
    return new ArcticSourceEnumState(assigner.state(), null, null, null);
  }
}
