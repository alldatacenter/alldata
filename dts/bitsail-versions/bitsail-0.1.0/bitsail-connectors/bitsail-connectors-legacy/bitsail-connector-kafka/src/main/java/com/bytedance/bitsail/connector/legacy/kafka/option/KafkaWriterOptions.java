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

package com.bytedance.bitsail.connector.legacy.kafka.option;

import com.bytedance.bitsail.common.annotation.Essential;
import com.bytedance.bitsail.common.option.ConfigOption;
import com.bytedance.bitsail.common.option.WriterOptions;

import static com.bytedance.bitsail.common.option.ConfigOptions.key;
import static com.bytedance.bitsail.common.option.WriterOptions.WRITER_PREFIX;

/**
 * Created 2022/8/16
 */

public interface KafkaWriterOptions extends WriterOptions.BaseWriterOptions {
  ConfigOption<String> KAFKA_SERVERS =
      key(WRITER_PREFIX + "kafka_servers")
          .noDefaultValue(String.class);

  @Essential
  ConfigOption<String> TOPIC_NAME =
      key(WRITER_PREFIX + "topic_name")
          .noDefaultValue(String.class);

  ConfigOption<String> PARTITION_FIELD =
      key(WRITER_PREFIX + "partition_field")
          .noDefaultValue(String.class);

  ConfigOption<Boolean> LOG_FAILURES_ONLY =
      key(WRITER_PREFIX + "log_failures_only")
          .defaultValue(false);

  ConfigOption<Integer> RETRIES =
      key(WRITER_PREFIX + "retries")
          .defaultValue(10);

  /**
   * retry.backoff.ms
   */
  ConfigOption<Long> RETRY_BACKOFF_MS =
      key(WRITER_PREFIX + "retry_backoff_ms")
          .defaultValue(1000L);

  /**
   * linger.ms
   */
  ConfigOption<Long> LINGER_MS =
      key(WRITER_PREFIX + "linger_ms")
          .defaultValue(5000L);
}