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

package com.netease.arctic.flink.read;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

/**
 * This Handler contains the topic offsets of upstream job id, epicNo, topic.
 * <p>
 * @deprecated since 0.4.1, will be removed in 0.7.0;
 */
@Deprecated
public class LogEpicStateHandler implements Serializable {
  private static final String SEPARATOR = "_";
  private static final long serialVersionUID = 203036690144637883L;

  /**
   * Key: combine upstream job id + "_" + epicNo + "_" + topic partition with
   * {@link #combineEpicNoAndPartition(String, long, int)}
   * Value: {@link EpicPartitionOffsets} offset detail information.
   */
  private final Map<String, EpicPartitionOffsets> currentUpStreamEpicOffsets;

  public LogEpicStateHandler(Map<String, EpicPartitionOffsets> upstreamEpicOffsets) {
    currentUpStreamEpicOffsets = upstreamEpicOffsets;
  }

  public Optional<EpicPartitionOffsets> getEpicNoFlip(String upstreamId, long epicNo, int partition) {
    String key = combineEpicNoAndPartition(upstreamId, epicNo, partition);
    return currentUpStreamEpicOffsets.containsKey(key) ?
        Optional.ofNullable(currentUpStreamEpicOffsets.get(key)) :
        Optional.empty();
  }

  public Map<String, EpicPartitionOffsets> getAll() {
    return currentUpStreamEpicOffsets;
  }

  public void registerEpicPartitionStartOffset(
      String upstreamId,
      long epicNo,
      int partition,
      final Long startOffset) {
    registerEpicPartitionOffset(upstreamId, epicNo, partition, startOffset, null, false);
  }

  public void registerEpicPartitionStartOffsetForce(
      String upstreamId,
      long epicNo,
      int partition,
      final Long startOffset) {
    registerEpicPartitionOffset(upstreamId, epicNo, partition, startOffset, null, true);
  }

  public void registerEpicPartitionRetractedOffset(
      String upstreamId,
      long epicNo,
      int partition,
      final Long retractedOffset) {
    registerEpicPartitionOffset(upstreamId, epicNo, partition, null, retractedOffset, false);
  }

  private void registerEpicPartitionOffset(
      String upstreamId,
      long epicNo,
      int partition,
      final Long startOffset,
      final Long retractedOffset,
      boolean forceUpdateStartOffset) {
    String key = combineEpicNoAndPartition(upstreamId, epicNo, partition);
    EpicPartitionOffsets epicPartitionOffsets = currentUpStreamEpicOffsets.get(key);
    boolean shouldUpdate = false;
    if (epicPartitionOffsets == null) {
      epicPartitionOffsets = new EpicPartitionOffsets(epicNo, partition);
      shouldUpdate = true;
    }
    if (forceUpdateStartOffset ||
        (startOffset != null && epicPartitionOffsets.startOffset == null)) {
      epicPartitionOffsets.startOffset = startOffset;
      shouldUpdate = true;
    }

    if (retractedOffset != null) {
      epicPartitionOffsets.retractedOffset = retractedOffset;
      shouldUpdate = true;
    }
    if (shouldUpdate) {
      currentUpStreamEpicOffsets.put(key, epicPartitionOffsets);
    }
  }

  public static String combineEpicNoAndPartition(String upstreamId, long epicNo, int partition) {
    return upstreamId + SEPARATOR + epicNo + SEPARATOR + partition;
  }

  public void clean(String upstreamId, long epicNo, int partition) {
    Iterator<String> keyIterator = currentUpStreamEpicOffsets.keySet().iterator();
    while (keyIterator.hasNext()) {
      String key = keyIterator.next();
      String[] keys = key.split(SEPARATOR);
      String keyUpstreamId = keys[0];
      long keyEpicNo = Long.parseLong(keys[1]);
      int keyPartition = Integer.parseInt(keys[2]);
      if (keyUpstreamId.equals(upstreamId) &&
          keyEpicNo > epicNo &&
          keyPartition == partition) {
        keyIterator.remove();
      }
    }
  }

  /**
   * explain epicNo and topic partition relate to start offset and retracted offset.
   */
  static class EpicPartitionOffsets implements Serializable {
    private static final long serialVersionUID = -6227903241361894539L;
    long epicNo;
    int partition;
    Long startOffset;
    Long retractedOffset;

    private EpicPartitionOffsets(long epicNo, int partition) {
      this.epicNo = epicNo;
      this.partition = partition;
    }
  }
}
