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

import org.apache.flink.api.connector.source.SourceOutput;
import org.apache.flink.connector.kafka.source.reader.KafkaRecordEmitter;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.connector.kafka.source.split.KafkaPartitionSplitState;
import org.apache.flink.table.data.RowData;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public class LogKafkaRecordEmitter extends KafkaRecordEmitter<RowData> {

  public LogKafkaRecordEmitter(KafkaRecordDeserializationSchema<RowData> deserializationSchema) {
    super(deserializationSchema);
  }

  @Override
  public void emitRecord(
      ConsumerRecord<byte[], byte[]> consumerRecord,
      SourceOutput<RowData> output,
      KafkaPartitionSplitState splitState)
      throws Exception {
    LogRecordWithRetractInfo<RowData> element = (LogRecordWithRetractInfo) consumerRecord;
    output.collect(element.getActualValue(), element.timestamp());
    ((LogKafkaPartitionSplitState) splitState).updateState(element);
  }
}
