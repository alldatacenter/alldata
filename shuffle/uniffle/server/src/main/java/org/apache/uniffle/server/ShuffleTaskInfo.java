/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.uniffle.server;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.roaringbitmap.longlong.Roaring64NavigableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.common.ShuffleDataDistributionType;

/**
 * ShuffleTaskInfo contains the information of submitting the shuffle,
 * the information of the cache block, user and timestamp corresponding to the app
 */
public class ShuffleTaskInfo {
  private static final Logger LOGGER = LoggerFactory.getLogger(ShuffleTaskInfo.class);

  private final String appId;
  private Long currentTimes;
  /**
   * shuffleId -> commit count
   */
  private Map<Integer, AtomicInteger> commitCounts;
  private Map<Integer, Object> commitLocks;
  /**
   * shuffleId -> blockIds
    */
  private Map<Integer, Roaring64NavigableMap> cachedBlockIds;
  private AtomicReference<String> user;

  private AtomicReference<ShuffleDataDistributionType> dataDistType;

  private AtomicLong totalDataSize = new AtomicLong(0);
  /**
   * shuffleId -> partitionId -> partition shuffle data size
   */
  private Map<Integer, Map<Integer, Long>> partitionDataSizes;
  /**
   * shuffleId -> huge partitionIds set
   */
  private final Map<Integer, Set<Integer>> hugePartitionTags;
  private final AtomicBoolean existHugePartition;

  public ShuffleTaskInfo(String appId) {
    this.appId = appId;
    this.currentTimes = System.currentTimeMillis();
    this.commitCounts = Maps.newConcurrentMap();
    this.commitLocks = Maps.newConcurrentMap();
    this.cachedBlockIds = Maps.newConcurrentMap();
    this.user = new AtomicReference<>();
    this.dataDistType = new AtomicReference<>();
    this.partitionDataSizes = Maps.newConcurrentMap();
    this.hugePartitionTags = Maps.newConcurrentMap();
    this.existHugePartition = new AtomicBoolean(false);
  }

  public Long getCurrentTimes() {
    return currentTimes;
  }

  public void setCurrentTimes(Long currentTimes) {
    this.currentTimes = currentTimes;
  }

  public Map<Integer, AtomicInteger> getCommitCounts() {
    return commitCounts;
  }

  public Map<Integer, Object> getCommitLocks() {
    return commitLocks;
  }

  public Map<Integer, Roaring64NavigableMap> getCachedBlockIds() {
    return cachedBlockIds;
  }

  public String getUser() {
    return user.get();
  }

  public void setUser(String user) {
    this.user.set(user);
  }

  public void setDataDistType(
      ShuffleDataDistributionType dataDistType) {
    this.dataDistType.set(dataDistType);
  }

  public ShuffleDataDistributionType getDataDistType() {
    return dataDistType.get();
  }

  public long addPartitionDataSize(int shuffleId, int partitionId, long delta) {
    totalDataSize.addAndGet(delta);
    partitionDataSizes.computeIfAbsent(shuffleId, key -> Maps.newConcurrentMap());
    Map<Integer, Long> partitions = partitionDataSizes.get(shuffleId);
    partitions.putIfAbsent(partitionId, 0L);
    return partitions.computeIfPresent(partitionId, (k, v) -> v + delta);
  }

  public long getTotalDataSize() {
    return totalDataSize.get();
  }

  public long getPartitionDataSize(int shuffleId, int partitionId) {
    Map<Integer, Long> partitions = partitionDataSizes.get(shuffleId);
    if (partitions == null) {
      return 0;
    }
    Long size = partitions.get(partitionId);
    if (size == null) {
      return 0L;
    }
    return size;
  }

  public boolean hasHugePartition() {
    return existHugePartition.get();
  }

  public int getHugePartitionSize() {
    return hugePartitionTags.values().stream().map(x -> x.size()).reduce((x, y) -> x + y).orElse(0);
  }

  public void markHugePartition(int shuffleId, int partitionId) {
    if (!existHugePartition.get()) {
      boolean markedWithCAS = existHugePartition.compareAndSet(false, true);
      if (markedWithCAS) {
        ShuffleServerMetrics.gaugeAppWithHugePartitionNum.inc();
        ShuffleServerMetrics.counterTotalAppWithHugePartitionNum.inc();
      }
    }

    Set<Integer> partitions = hugePartitionTags.computeIfAbsent(shuffleId, key -> Sets.newConcurrentHashSet());
    if (partitions.add(partitionId)) {
      ShuffleServerMetrics.counterTotalHugePartitionNum.inc();
      ShuffleServerMetrics.gaugeHugePartitionNum.inc();
      LOGGER.warn("Huge partition occurs, appId: {}, shuffleId: {}, partitionId: {}", appId, shuffleId, partitionId);
    }
  }
}
