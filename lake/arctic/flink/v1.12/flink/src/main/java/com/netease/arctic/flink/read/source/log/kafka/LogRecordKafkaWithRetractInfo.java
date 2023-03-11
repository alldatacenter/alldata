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

import com.netease.arctic.log.LogData;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public class LogRecordKafkaWithRetractInfo<T> extends ConsumerRecord<byte[], byte[]> {

  /**
   * Denote reader is in retracting read mode.
   * In this mode, data would be read in reverse order and opposite RowKind.
   */
  private final boolean retracting;
  /**
   * @see LogKafkaPartitionSplit#retractStopOffset
   */
  private final Long retractStoppingOffset;
  /**
   * @see LogKafkaPartitionSplit#revertStartOffset
   */
  private final Long revertStartingOffset;
  /**
   * @see LogKafkaPartitionSplit#retractingEpicNo
   */
  private final Long retractingEpicNo;
  private final LogData<T> logData;
  private final T actualValue;

  public LogRecordKafkaWithRetractInfo(ConsumerRecord<byte[], byte[]> consumerRecord,
                                       boolean retracting,
                                       Long retractStoppingOffset,
                                       Long revertStartingOffset,
                                       Long retractingEpicNo,
                                       LogData<T> logData,
                                       T actualValue) {
    super(consumerRecord.topic(),
        consumerRecord.partition(),
        consumerRecord.offset(),
        consumerRecord.timestamp(),
        consumerRecord.timestampType(),
        consumerRecord.checksum(),
        consumerRecord.serializedKeySize(),
        consumerRecord.serializedValueSize(),
        consumerRecord.key(),
        consumerRecord.value(),
        consumerRecord.headers(),
        consumerRecord.leaderEpoch());
    this.retracting = retracting;
    this.retractStoppingOffset = retractStoppingOffset;
    this.revertStartingOffset = revertStartingOffset;
    this.retractingEpicNo = retractingEpicNo;
    this.logData = logData;
    this.actualValue = actualValue;
  }

  public static <T> LogRecordKafkaWithRetractInfo<T> ofRetract(ConsumerRecord<byte[], byte[]> consumerRecord,
                                                               Long retractStoppingOffset,
                                                               Long revertStartingOffset,
                                                               Long retractingEpicNo,
                                                               LogData<T> logData,
                                                               T actualValue) {
    return new LogRecordKafkaWithRetractInfo<>(consumerRecord, true, retractStoppingOffset,
        revertStartingOffset, retractingEpicNo, logData, actualValue);
  }

  public static <T> LogRecordKafkaWithRetractInfo<T> of(ConsumerRecord<byte[], byte[]> consumerRecord,
                                                        LogData<T> logData) {
    return new LogRecordKafkaWithRetractInfo<>(consumerRecord, false, null,
        null, null, logData, logData.getActualValue());
  }

  public boolean isRetracting() {
    return retracting;
  }

  public Long getRetractStoppingOffset() {
    return retractStoppingOffset;
  }

  public Long getRevertStartingOffset() {
    return revertStartingOffset;
  }

  public LogData<T> getLogData() {
    return logData;
  }

  public Long getRetractingEpicNo() {
    return retractingEpicNo;
  }

  public T getActualValue() {
    return actualValue;
  }
}