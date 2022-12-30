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

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.legacy.messagequeue.source.option.BaseMessageQueueReaderOptions;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created 2022/8/31
 */
public class CountKafkaDeserializationSchemaWrapper<T> implements KafkaDeserializationSchema<T> {
  private final BitSailConfiguration configuration;
  private final DeserializationSchema<T> deserializationSchema;
  private final boolean countModeEnabled;
  private final long threshold;

  protected AtomicLong recordCount;

  public CountKafkaDeserializationSchemaWrapper(BitSailConfiguration configuration,
                                                DeserializationSchema<T> deserializationSchema) {
    this.configuration = configuration;
    this.deserializationSchema = deserializationSchema;
    this.countModeEnabled = configuration.get(BaseMessageQueueReaderOptions.ENABLE_COUNT_MODE);
    this.threshold = configuration.get(BaseMessageQueueReaderOptions.COUNT_MODE_RECORD_THRESHOLD);
  }

  @Override
  public void open(DeserializationSchema.InitializationContext context) throws Exception {
    deserializationSchema.open(context);
    if (countModeEnabled) {
      recordCount = new AtomicLong(0);
    }
  }

  @Override
  public T deserialize(ConsumerRecord<byte[], byte[]> record) throws Exception {
    throw new UnsupportedOperationException("Should never be called");
  }

  @Override
  public void deserialize(ConsumerRecord<byte[], byte[]> message, Collector<T> out)
      throws Exception {
    deserializationSchema.deserialize(message.value(), out);
  }

  @Override
  public boolean isEndOfStream(T nextElement) {
    if (countModeEnabled) {
      return recordCount.incrementAndGet() > threshold;
    }
    return deserializationSchema.isEndOfStream(nextElement);
  }

  @Override
  public TypeInformation<T> getProducedType() {
    return deserializationSchema.getProducedType();
  }
}
