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

package com.bytedance.bitsail.connector.legacy.messagequeue.serialization;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.legacy.messagequeue.source.option.BaseMessageQueueReaderOptions;
import com.bytedance.bitsail.flink.core.serialization.AbstractDeserializationSchema;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.types.Row;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A simple wrapper for using the DeserializationSchema with the KafkaDeserializationSchema
 * interface for debug. Support record count and running time interval conf.
 */
@Slf4j
public class CountDeserializationSchema extends AbstractDeserializationSchema {
  private static final long serialVersionUID = -2556547991095476383L;
  private final long recordThreshold;
  private final long runTimeThresholdMs;
  private final long startTime;
  protected AtomicLong recordCount;

  @SuppressWarnings("checkstyle:MagicNumber")
  public CountDeserializationSchema(BitSailConfiguration jobConf) {
    this.recordThreshold = jobConf.get(BaseMessageQueueReaderOptions.COUNT_MODE_RECORD_THRESHOLD);
    long runTimeThreshold = jobConf.get(BaseMessageQueueReaderOptions.COUNT_MODE_RUN_TIME_THRESHOLD);
    this.runTimeThresholdMs = runTimeThreshold * 1000;
    this.recordCount = new AtomicLong(0);
    this.startTime = System.currentTimeMillis();
    log.info(String.format("Count mode turns on. And record threshold is %d. Run time threshold is %s s.",
        recordThreshold,
        runTimeThreshold));
  }

  @Override
  protected Row deserialize(byte[] key, byte[] value, String partition, long offset) throws Exception {
    recordCount.addAndGet(1);
    return super.deserialize(key, value, partition, offset);
  }

  @Override
  public boolean isEndOfStream(Row nextElement) {
    if (recordCount.get() > recordThreshold) {
      log.info(String.format("The task has consumed %d records which is larger than record limit %d.",
          recordCount.get(),
          recordThreshold));
      return true;
    }
    if (System.currentTimeMillis() - startTime >= runTimeThresholdMs) {
      log.info(String.format("The task has run for %d ms which is longer than config interval %d ms",
          System.currentTimeMillis() - startTime,
          runTimeThresholdMs));
      return true;
    }
    return false;
  }
}
