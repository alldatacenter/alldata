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

package com.netease.arctic.flink.read.source.log.kafka;

import org.apache.flink.connector.kafka.source.split.KafkaPartitionSplit;
import org.apache.flink.connector.kafka.source.split.KafkaPartitionSplitState;
import org.apache.flink.table.data.RowData;

import javax.annotation.Nullable;
import java.util.NavigableMap;
import java.util.TreeMap;

public class LogKafkaPartitionSplitState extends KafkaPartitionSplitState {

  /**
   * Denote reader is in retracting read mode.
   * In this mode, data would be read in reverse order and opposite RowKind.
   */
  private boolean retracting;
  /**
   * @see LogKafkaPartitionSplit#retractStopOffset
   */
  @Nullable
  private Long retractStopOffset;
  /**
   * @see LogKafkaPartitionSplit#revertStartOffset
   */
  @Nullable
  private Long revertStartOffset;
  /**
   * @see LogKafkaPartitionSplit#retractingEpicNo
   */
  @Nullable
  private Long retractingEpicNo;
  /**
   * @see LogKafkaPartitionSplit#retractingUpstreamId
   */
  @Nullable
  private String retractingUpstreamId;
  /**
   * Key: upstream job id + "_" + epicNo
   * Value: epic start offset
   */
  private final NavigableMap<String, Long> upstreamEpicStartOffsets;

  public LogKafkaPartitionSplitState(KafkaPartitionSplit s) {
    super(s);

    if (!(s instanceof LogKafkaPartitionSplit)) {
      retracting = false;
      upstreamEpicStartOffsets = new TreeMap<>();
      return;
    }
    LogKafkaPartitionSplit partitionSplit = (LogKafkaPartitionSplit) s;
    upstreamEpicStartOffsets = partitionSplit.getUpStreamEpicStartOffsets();
    retracting = partitionSplit.isRetracting();
    revertStartOffset = partitionSplit.getRevertStartOffset();
    retractStopOffset = partitionSplit.getRetractStopOffset();
    retractingEpicNo = partitionSplit.getRetractingEpicNo();
    retractingUpstreamId = partitionSplit.getRetractingUpstreamId();
  }

  public void initEpicStartOffsetIfEmpty(String upstreamId, long epicNo, long offset) {
    String key = combineUpstreamIdAndEpicNo(upstreamId, epicNo);
    upstreamEpicStartOffsets.putIfAbsent(key, offset);
  }

  public void updateState(LogRecordWithRetractInfo<RowData> record) {
    if (record.isRetracting()) {
      setCurrentOffset(record.offset() - 1);
      revertStartOffset = record.getRevertStartingOffset();
      retractStopOffset = record.getRetractStoppingOffset();
      retractingEpicNo = record.getRetractingEpicNo();
      retractingUpstreamId = record.getLogData().getUpstreamId();
    } else {
      setCurrentOffset(record.offset() + 1);
    }
    initEpicStartOffsetIfEmpty(record.getLogData().getUpstreamId(), record.getLogData().getEpicNo(),
        record.offset());
    
    // todo: clear useless epic start offset in state
    retracting = record.isRetracting();
  }

  public boolean isRetracting() {
    return retracting;
  }

  public Long getRetractStopOffset() {
    return retractStopOffset;
  }

  public Long getRevertStartOffset() {
    return revertStartOffset;
  }

  public NavigableMap<String, Long> getUpstreamEpicStartOffsets() {
    return upstreamEpicStartOffsets;
  }

  public Long getRetractingEpicNo() {
    return retractingEpicNo;
  }

  public String getRetractingUpstreamId() {
    return retractingUpstreamId;
  }

  private String combineUpstreamIdAndEpicNo(String upstreamId, long epicNo) {
    return upstreamId + "_" + epicNo;
  }

  public LogKafkaPartitionSplit toLogKafkaPartitionSplit() {
    return new LogKafkaPartitionSplit(this);
  }
}
