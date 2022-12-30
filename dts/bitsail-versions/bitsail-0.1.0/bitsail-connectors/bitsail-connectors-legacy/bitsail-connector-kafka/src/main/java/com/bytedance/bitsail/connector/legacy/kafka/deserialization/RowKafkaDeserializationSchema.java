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

package com.bytedance.bitsail.connector.legacy.kafka.deserialization;

import com.bytedance.bitsail.flink.core.serialization.AbstractDeserializationSchema;

import org.apache.flink.annotation.Internal;
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema;
import org.apache.flink.types.Row;
import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * A simple wrapper for using the DeserializationSchema with the KafkaDeserializationSchema
 * interface.
 *
 * @param {@link Row}. The type created by the deserialization schema.
 */
@Internal
public class RowKafkaDeserializationSchema extends AbstractDeserializationSchema implements KafkaDeserializationSchema<Row> {
  private static final long serialVersionUID = -1939184849109300915L;
  private final int sourceIndex;

  public RowKafkaDeserializationSchema(int sourceIndex) {
    this.sourceIndex = sourceIndex;
  }

  @Override
  public Row deserialize(ConsumerRecord<byte[], byte[]> record) throws Exception {
    return super.deserialize(record.key(), record.value(), String.valueOf(record.partition()), record.offset(), this.sourceIndex);
  }
}


