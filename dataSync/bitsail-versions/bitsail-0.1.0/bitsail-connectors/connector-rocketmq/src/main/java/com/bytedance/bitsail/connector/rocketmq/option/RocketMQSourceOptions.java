/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.bytedance.bitsail.connector.rocketmq.option;

import com.bytedance.bitsail.common.option.ConfigOption;
import com.bytedance.bitsail.common.option.ConfigOptions;
import com.bytedance.bitsail.common.option.ReaderOptions;

import com.alibaba.fastjson.TypeReference;
import org.apache.rocketmq.common.message.MessageQueue;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.bytedance.bitsail.common.option.ReaderOptions.READER_PREFIX;

public interface RocketMQSourceOptions extends ReaderOptions.BaseReaderOptions {

  String CONSUMER_OFFSET_LATEST_KEY = "latest";
  String CONSUMER_OFFSET_EARLIEST_KEY = "earliest";
  String CONSUMER_OFFSET_TIMESTAMP_KEY = "timestamp";
  Long CONSUMER_STOPPING_OFFSET = Long.MAX_VALUE;

  ConfigOption<Long> DISCOVERY_INTERNAL =
      ConfigOptions.key(READER_PREFIX + "discovery_internal_ms")
          .defaultValue(TimeUnit.MINUTES.toMillis(5L));

  ConfigOption<String> ACCESS_KEY =
      ConfigOptions.key(READER_PREFIX + "assess_key")
          .noDefaultValue(String.class);

  ConfigOption<String> SECRET_KEY =
      ConfigOptions.key(READER_PREFIX + "secret_key")
          .noDefaultValue(String.class);

  ConfigOption<String> CLUSTER =
      ConfigOptions.key(READER_PREFIX + "cluster")
          .noDefaultValue(String.class);

  ConfigOption<String> TOPIC =
      ConfigOptions.key(READER_PREFIX + "topic")
          .noDefaultValue(String.class);

  ConfigOption<String> CONSUMER_GROUP =
      ConfigOptions.key(READER_PREFIX + "consumer_group")
          .noDefaultValue(String.class);

  ConfigOption<String> CONSUMER_TAG =
      ConfigOptions.key(READER_PREFIX + "consumer_tag")
          .noDefaultValue(String.class);

  ConfigOption<Integer> POLL_BATCH_SIZE =
      ConfigOptions.key(READER_PREFIX + "poll_batch_size")
          .defaultValue(2048);

  ConfigOption<Long> POLL_TIMEOUT =
      ConfigOptions.key(READER_PREFIX + "poll_timeout")
          .defaultValue(TimeUnit.MINUTES.toMillis(1L));

  ConfigOption<Boolean> COMMIT_IN_CHECKPOINT =
      ConfigOptions.key(READER_PREFIX + "commit_in_checkpoint")
          .defaultValue(false);

  ConfigOption<String> CONSUMER_OFFSET_MODE =
      ConfigOptions.key(READER_PREFIX + "consumer_offset_mode")
          .defaultValue(CONSUMER_OFFSET_LATEST_KEY);

  ConfigOption<Long> CONSUMER_OFFSET_TIMESTAMP =
      ConfigOptions.key(READER_PREFIX + "consumer_offset_timestamp")
          .noDefaultValue(Long.class);

  ConfigOption<Map<MessageQueue, Long>> CONSUMER_STOP_OFFSET =
      ConfigOptions.key(READER_PREFIX + "consumer_stop_offset")
          .onlyReference(new TypeReference<Map<MessageQueue, Long>>() {
          });
}
