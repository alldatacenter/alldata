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

package com.bytedance.bitsail.connector.legacy.messagequeue.source.option;

import com.bytedance.bitsail.common.option.ConfigOption;

import com.alibaba.fastjson.TypeReference;

import java.util.Map;

import static com.bytedance.bitsail.common.option.ConfigOptions.key;
import static com.bytedance.bitsail.common.option.ReaderOptions.READER_PREFIX;

public interface BaseMessageQueueReaderOptions {

  ConfigOption<String> FORMAT_TYPE =
      key(READER_PREFIX + "format_type")
          .defaultValue("streaming_file");

  ConfigOption<Boolean> ENABLE_COUNT_MODE =
      key(READER_PREFIX + "enable_count_mode")
          .defaultValue(false);

  ConfigOption<Long> COUNT_MODE_RECORD_THRESHOLD =
      key(READER_PREFIX + "count_mode_record_threshold")
          .defaultValue(10000L);

  /**
   * time threshold to stop job if count mode enabled
   * unit: second
   */
  ConfigOption<Long> COUNT_MODE_RUN_TIME_THRESHOLD =
      key(READER_PREFIX + "count_mode_run_time_threshold")
          .defaultValue(600L);


  ConfigOption<Map<String, String>> CONNECTOR_PROPERTIES =
      key(READER_PREFIX + "connector")
          .onlyReference(new TypeReference<Map<String, String>>() {
          });
}
