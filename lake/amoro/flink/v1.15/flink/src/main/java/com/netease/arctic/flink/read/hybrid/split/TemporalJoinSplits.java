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

package com.netease.arctic.flink.read.hybrid.split;

import org.apache.flink.api.connector.source.SourceSplit;
import org.apache.flink.metrics.MetricGroup;
import org.apache.flink.util.CollectionUtil;
import org.apache.flink.util.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.netease.arctic.flink.metric.MetricConstant.TEMPORAL_TABLE_INITIALIZATION_END_TIMESTAMP;
import static com.netease.arctic.flink.metric.MetricConstant.TEMPORAL_TABLE_INITIALIZATION_START_TIMESTAMP;

/**
 * If using arctic table as build-table, TemporalJoinSplits can record the first splits planned by Enumerator.
 */
public class TemporalJoinSplits implements Serializable {

  public static final long serialVersionUID = 1L;
  public static final Logger LOGGER = LoggerFactory.getLogger(TemporalJoinSplits.class);

  private final MetricGroup metricGroup;
  private final long startTimeMs = System.currentTimeMillis();
  private Map<String, Boolean> splits;
  private long unfinishedCount;
  /**
   * transient because it is necessary to notify reader again after failover.
   */
  private transient boolean hasNotifiedReader = false;

  public TemporalJoinSplits(Collection<ArcticSplit> splits, MetricGroup metricGroup) {
    Preconditions.checkNotNull(splits, "plan splits should not be null");
    this.splits = splits.stream().map(SourceSplit::splitId).collect(Collectors.toMap((k) -> k, (i) -> false));

    unfinishedCount = this.splits.size();
    LOGGER.info("init splits at {}, size:{}", LocalDateTime.now(), unfinishedCount);
    this.metricGroup = metricGroup;
    if (metricGroup != null) {
      metricGroup.gauge(TEMPORAL_TABLE_INITIALIZATION_START_TIMESTAMP, () -> startTimeMs);
    }
  }

  public Map<String, Boolean> getSplits() {
    return splits;
  }

  public synchronized void addSplitsBack(Collection<ArcticSplit> splits) {
    if (this.splits == null || CollectionUtil.isNullOrEmpty(splits)) {
      return;
    }
    splits.forEach((p) -> {
      Boolean finished = this.splits.get(p.splitId());
      if (finished == null || !finished) {
        return;
      }
      unfinishedCount++;
      LOGGER.debug("add back split:{} at {}", p, LocalDateTime.now());
      this.splits.put(p.splitId(), false);
    });
  }

  /**
   * Remove finished splits.
   *
   * @return True if all splits are finished, otherwise false.
   */
  public synchronized boolean removeAndReturnIfAllFinished(Collection<String> finishedSplitIds) {
    if (splits == null) {
      return true;
    }
    if (CollectionUtil.isNullOrEmpty(finishedSplitIds)) {
      return unfinishedCount == 0;
    }

    finishedSplitIds.forEach((p) -> {
      Boolean finished = this.splits.get(p);
      if (finished == null || finished) {
        return;
      }
      unfinishedCount--;
      this.splits.put(p, true);
      LOGGER.debug("finish split:{} at {}", p, LocalDateTime.now());
    });
    if (unfinishedCount == 0) {
      LOGGER.info("finish all splits at {}", LocalDateTime.now());
      if (metricGroup != null) {
        metricGroup.gauge(TEMPORAL_TABLE_INITIALIZATION_END_TIMESTAMP, System::currentTimeMillis);
      }
      return true;
    }
    return false;
  }

  public synchronized void clear() {
    if (unfinishedCount == 0) {
      this.splits = null;
    }
  }

  public boolean hasNotifiedReader() {
    return hasNotifiedReader;
  }

  public void notifyReader() {
    this.hasNotifiedReader = true;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TemporalJoinSplits that = (TemporalJoinSplits) o;
    return startTimeMs == that.startTimeMs &&
        unfinishedCount == that.unfinishedCount &&
        hasNotifiedReader == that.hasNotifiedReader &&
        Objects.equals(metricGroup, that.metricGroup) &&
        Objects.equals(splits, that.splits);
  }

  @Override
  public int hashCode() {
    return Objects.hash(metricGroup, startTimeMs, splits, unfinishedCount, hasNotifiedReader);
  }

}
