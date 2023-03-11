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

import java.util.NavigableMap;

public class LogKafkaPartitionSplit extends KafkaPartitionSplit {

  /**
   * Denote reader is in retracting read mode.
   * In this mode, data would be read in reverse order and opposite RowKind.
   */
  private final boolean retracting;
  /**
   * The offset where job retract stops, i.e. Read reversely ends.
   */
  private final Long retractStopOffset;
  /**
   * The offset where job revert to normal read starts from. It should skip the flip which has been read.
   */
  private final Long revertStartOffset;
  /**
   * The epic No. which has finished checkpoint. The data whose epic No. larger than it should be retracted.
   */
  private final Long retractingEpicNo;
  /**
   * The upstream JobId which should be retracted.
   */
  private final String retractingUpstreamId;
  /**
   * Key: upstream job id + "_" + epicNo
   * Value: epic start offset
   */
  private final NavigableMap<String, Long> upStreamEpicStartOffsets;

  public boolean isRetracting() {
    return retracting;
  }

  public Long getRetractStopOffset() {
    return retractStopOffset;
  }

  public Long getRevertStartOffset() {
    return revertStartOffset;
  }

  public NavigableMap<String, Long> getUpStreamEpicStartOffsets() {
    return upStreamEpicStartOffsets;
  }

  public Long getRetractingEpicNo() {
    return retractingEpicNo;
  }

  public String getRetractingUpstreamId() {
    return retractingUpstreamId;
  }

  public LogKafkaPartitionSplit(LogKafkaPartitionSplitState splitState) {
    super(splitState.getTopicPartition(), splitState.getStartingOffset(),
        splitState.getStoppingOffset().orElse(NO_STOPPING_OFFSET));
    retracting = splitState.isRetracting();
    retractStopOffset = splitState.getRetractStopOffset();
    revertStartOffset = splitState.getRevertStartOffset();
    upStreamEpicStartOffsets = splitState.getUpstreamEpicStartOffsets();
    retractingEpicNo = splitState.getRetractingEpicNo();
    retractingUpstreamId = splitState.getRetractingUpstreamId();
  }
}
