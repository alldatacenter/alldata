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
import com.netease.arctic.flink.read.hybrid.split.ArcticSplit;
import com.netease.arctic.flink.read.hybrid.split.ChangelogSplit;
import com.netease.arctic.flink.read.hybrid.split.MergeOnReadSplit;
import com.netease.arctic.flink.table.ArcticTableLoader;
import org.apache.commons.collections.CollectionUtils;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.expressions.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.netease.arctic.flink.read.hybrid.enumerator.ArcticEnumeratorOffset.EARLIEST_SNAPSHOT_ID;

/**
 * A planner for merge-on-read scanning by {@link this#discoverInitialSplits}
 * and incremental scanning by {@link this#discoverIncrementalSplits(ArcticEnumeratorOffset, List)}.
 * <p> {@link ContinuousEnumerationResult#splits()} includes the {@link MergeOnReadSplit}s and {@link ChangelogSplit}s.
 */
public class MergeOnReadIncrementalPlanner extends ContinuousSplitPlannerImpl {
  private static final Logger LOG = LoggerFactory.getLogger(MergeOnReadIncrementalPlanner.class);

  public MergeOnReadIncrementalPlanner(ArcticTableLoader loader) {
    super(loader);
  }

  @Override
  protected ContinuousEnumerationResult discoverInitialSplits(List<Expression> filters) {
    Snapshot changeSnapshot = table.changeTable().currentSnapshot();

    List<ArcticSplit> arcticSplits = FlinkSplitPlanner.mergeOnReadPlan(table, filters, splitCount);

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
