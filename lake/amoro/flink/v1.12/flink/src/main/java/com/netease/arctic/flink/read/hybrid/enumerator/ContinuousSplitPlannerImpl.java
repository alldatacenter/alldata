/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.netease.arctic.flink.read.hybrid.enumerator;

import com.netease.arctic.flink.read.FlinkSplitPlanner;
import com.netease.arctic.flink.read.hybrid.split.ArcticSplit;
import com.netease.arctic.flink.read.hybrid.split.ChangelogSplit;
import com.netease.arctic.flink.read.hybrid.split.SnapshotSplit;
import com.netease.arctic.flink.table.ArcticTableLoader;
import com.netease.arctic.scan.TableEntriesScan;
import com.netease.arctic.table.KeyedTable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.flink.annotation.Internal;
import org.apache.iceberg.FileContent;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.expressions.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.netease.arctic.flink.read.FlinkSplitPlanner.planChangeTable;
import static com.netease.arctic.flink.read.hybrid.enumerator.ArcticEnumeratorOffset.EARLIEST_SNAPSHOT_ID;
import static com.netease.arctic.flink.util.ArcticUtils.loadArcticTable;

/**
 * Continuous planning {@link KeyedTable} by {@link ArcticEnumeratorOffset} and generate a
 * {@link ContinuousEnumerationResult}.
 * <p> {@link ContinuousEnumerationResult#splits()} includes the {@link SnapshotSplit}s and {@link ChangelogSplit}s.
 */
@Internal
public class ContinuousSplitPlannerImpl implements ContinuousSplitPlanner {
  private static final Logger LOG = LoggerFactory.getLogger(ContinuousSplitPlannerImpl.class);

  protected transient KeyedTable table;
  protected final ArcticTableLoader loader;
  protected static final AtomicInteger splitCount = new AtomicInteger();

  public ContinuousSplitPlannerImpl(ArcticTableLoader loader) {
    this.loader = loader;
  }

  @Override
  public void close() throws IOException {
  }

  @Override
  public ContinuousEnumerationResult planSplits(ArcticEnumeratorOffset lastOffset, List<Expression> filters) {
    if (table == null) {
      table = loadArcticTable(loader).asKeyedTable();
    }
    table.refresh();
    if (lastOffset != null) {
      return discoverIncrementalSplits(lastOffset, filters);
    } else {
      return discoverInitialSplits(filters);
    }
  }

  protected ContinuousEnumerationResult discoverIncrementalSplits(
      ArcticEnumeratorOffset lastPosition, List<Expression> filters) {
    long fromChangeSnapshotId = lastPosition.changeSnapshotId();
    Snapshot changeSnapshot = table.changeTable().currentSnapshot();
    if (changeSnapshot != null && changeSnapshot.snapshotId() != fromChangeSnapshotId) {
      long snapshotId = changeSnapshot.snapshotId();
      TableEntriesScan.Builder tableEntriesScanBuilder =
          TableEntriesScan.builder(table.changeTable())
              .useSnapshot(snapshotId)
              .includeFileContent(FileContent.DATA);
      if (filters != null) {
        filters.forEach(tableEntriesScanBuilder::withDataFilter);
      }
      TableEntriesScan entriesScan = tableEntriesScanBuilder.build();

      Long fromSequence = null;
      if (fromChangeSnapshotId != Long.MIN_VALUE) {
        Snapshot snapshot = table.changeTable().snapshot(fromChangeSnapshotId);
        fromSequence = snapshot.sequenceNumber();
      }

      List<ArcticSplit> arcticChangeSplit =
          planChangeTable(entriesScan, fromSequence, table.changeTable().spec(), splitCount);
      return new ContinuousEnumerationResult(
          arcticChangeSplit,
          lastPosition,
          ArcticEnumeratorOffset.of(snapshotId, null));
    }
    return ContinuousEnumerationResult.EMPTY;
  }

  protected ContinuousEnumerationResult discoverInitialSplits(List<Expression> filters) {
    Snapshot changeSnapshot = table.changeTable().currentSnapshot();
    List<ArcticSplit> arcticSplits = FlinkSplitPlanner.planFullTable(table, filters, splitCount);

    long changeStartSnapshotId = changeSnapshot != null ? changeSnapshot.snapshotId() : EARLIEST_SNAPSHOT_ID;
    if (changeSnapshot == null && CollectionUtils.isEmpty(arcticSplits)) {
      LOG.info("There have no change snapshot, and no base splits in table: {}.", table);
      return ContinuousEnumerationResult.EMPTY;
    }

    return new ContinuousEnumerationResult(
        arcticSplits,
        null,
        ArcticEnumeratorOffset.of(changeStartSnapshotId, null));
  }
}
