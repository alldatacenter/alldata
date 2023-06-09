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

package com.netease.arctic.flink.write.hidden.kafka;

import com.netease.arctic.flink.shuffle.ShuffleHelper;
import com.netease.arctic.flink.write.hidden.ArcticLogPartitioner;
import com.netease.arctic.flink.write.hidden.LogMsgFactory;
import com.netease.arctic.log.LogDataJsonSerialization;

import java.util.Properties;

import static org.apache.iceberg.relocated.com.google.common.base.Preconditions.checkNotNull;

/**
 * A factory creates kafka log queue producers or consumers.
 */
public class HiddenKafkaFactory<T> implements LogMsgFactory<T> {
  private static final long serialVersionUID = -1L;

  @Override
  public Producer<T> createProducer(
      Properties producerConfig,
      String topic,
      LogDataJsonSerialization<T> logDataJsonSerialization,
      ShuffleHelper helper) {
    checkNotNull(topic);
    return new HiddenKafkaProducer<>(
        producerConfig,
        topic,
        logDataJsonSerialization,
        new ArcticLogPartitioner<>(
            helper
        ));
  }

  @Override
  public Consumer createConsumer() {
    throw new UnsupportedOperationException("not supported right now");
  }
}
