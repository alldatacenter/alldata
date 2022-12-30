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

package com.bytedance.bitsail.connector.legacy.rocketmq.option;

import com.bytedance.bitsail.common.annotation.Essential;
import com.bytedance.bitsail.common.option.ConfigOption;
import com.bytedance.bitsail.common.option.WriterOptions;

import static com.bytedance.bitsail.common.option.ConfigOptions.key;
import static com.bytedance.bitsail.common.option.WriterOptions.WRITER_PREFIX;

public interface RocketMQWriterOptions extends WriterOptions.BaseWriterOptions {

  @Essential
  ConfigOption<String> NAME_SERVER_ADDRESS =
      key(WRITER_PREFIX + "name_server_address")
          .noDefaultValue(String.class);

  ConfigOption<String> PRODUCER_GROUP =
      key(WRITER_PREFIX + "producer_group")
          .noDefaultValue(String.class);

  @Essential
  ConfigOption<String> TOPIC =
      key(WRITER_PREFIX + "topic")
          .noDefaultValue(String.class);

  ConfigOption<String> TAG =
      key(WRITER_PREFIX + "tag")
          .noDefaultValue(String.class);

  ConfigOption<Boolean> ENABLE_BATCH_FLUSH =
      key(WRITER_PREFIX + "enable_batch_flush")
          .defaultValue(true);

  ConfigOption<Integer> BATCH_SIZE =
      key(WRITER_PREFIX + "batch_size")
          .defaultValue(100);

  /**
   * when encounter errors while sending:<br/>
   * true:  log the error<br/>
   * false: throw exceptions
   */
  ConfigOption<Boolean> LOG_FAILURES_ONLY =
      key(WRITER_PREFIX + "log_failures_only")
          .defaultValue(false);

  ConfigOption<Boolean> ENABLE_SYNC_SEND =
      key(WRITER_PREFIX + "enable_sync_send")
          .defaultValue(false);

  ConfigOption<String> ACCESS_KEY =
      key(WRITER_PREFIX + "access_key")
        .noDefaultValue(String.class);

  ConfigOption<String> SECRET_KEY =
      key(WRITER_PREFIX + "secret_key")
          .noDefaultValue(String.class);

  ConfigOption<Integer> SEND_FAILURE_RETRY_TIMES =
      key(WRITER_PREFIX + "send_failure_retry_times")
          .defaultValue(3);

  ConfigOption<Integer> SEND_MESSAGE_TIMEOUT =
      key(WRITER_PREFIX + "send_message_timeout_ms")
          .defaultValue(3000);

  ConfigOption<Integer> MAX_MESSAGE_SIZE =
      key(WRITER_PREFIX + "max_message_size_bytes")
          .defaultValue(4194304);

  ConfigOption<String> KEY_FIELDS =
      key(WRITER_PREFIX + "key")
          .noDefaultValue(String.class);

  ConfigOption<String> PARTITION_FIELDS =
      key(WRITER_PREFIX + "partition_fields")
          .noDefaultValue(String.class);

  ConfigOption<String> FORMAT =
      key(WRITER_PREFIX + "format")
          .defaultValue("json");
}
