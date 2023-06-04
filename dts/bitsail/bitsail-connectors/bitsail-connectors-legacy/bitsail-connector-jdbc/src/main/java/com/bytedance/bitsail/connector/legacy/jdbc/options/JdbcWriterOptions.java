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

package com.bytedance.bitsail.connector.legacy.jdbc.options;

import com.bytedance.bitsail.common.annotation.Essential;
import com.bytedance.bitsail.common.option.ConfigOption;
import com.bytedance.bitsail.common.option.WriterOptions;

import com.alibaba.fastjson.TypeReference;

import java.util.List;
import java.util.Map;

import static com.bytedance.bitsail.common.option.ConfigOptions.key;
import static com.bytedance.bitsail.common.option.WriterOptions.WRITER_PREFIX;

/**
 * Jdbc Options
 */
public interface JdbcWriterOptions extends WriterOptions.BaseWriterOptions {

  ConfigOption<String> PARTITION_NAME =
      key(WRITER_PREFIX + "partition_name")
          .noDefaultValue(String.class);

  ConfigOption<String> PARTITION_VALUE =
      key(WRITER_PREFIX + "partition_value")
          .noDefaultValue(String.class);

  ConfigOption<String> PARTITION_PATTERN_FORMAT =
      key(WRITER_PREFIX + "partition_pattern_format")
          .noDefaultValue(String.class);

  /**
   * Extra partition setting. example:
   * "extra_partitions": [{
   * "partition_name" : "key1",
   * "partition_type" : "type",
   * "partition_value" : "value1"
   * }]
   */
  ConfigOption<List<Map<String, Object>>> EXTRA_PARTITIONS =
      key(WRITER_PREFIX + "extra_partitions")
          .onlyReference(new TypeReference<List<Map<String, Object>>>() {
          });

  @Essential
  ConfigOption<List<Map<String, Object>>> CONNECTIONS =
      key(WRITER_PREFIX + "connections")
          .onlyReference(new TypeReference<List<Map<String, Object>>>() {
          });

  ConfigOption<String> CONNECTION_PARAMETERS =
      key(WRITER_PREFIX + "connection_parameters")
          .noDefaultValue(String.class);

  ConfigOption<Integer> MYSQL_DATA_TTL =
      key(WRITER_PREFIX + "mysql_data_ttl")
          .defaultValue(0);

  ConfigOption<String> SHARD_KEY =
      key(WRITER_PREFIX + "shard_key")
          .noDefaultValue(String.class);

  ConfigOption<String> PRE_QUERY =
      key(WRITER_PREFIX + "pre_query")
          .noDefaultValue(String.class);

  ConfigOption<String> VERIFY_QUERY =
      key(WRITER_PREFIX + "verify_query")
          .noDefaultValue(String.class);

  ConfigOption<Integer> WRITE_BATCH_INTERVAL =
      key(WRITER_PREFIX + "write_batch_interval")
          .noDefaultValue(Integer.class);

  ConfigOption<Integer> WRITE_RETRY_TIMES =
      key(WRITER_PREFIX + "write_retry_times")
          .noDefaultValue(Integer.class);

  ConfigOption<Integer> RETRY_INTERVAL_SECONDS =
      key(WRITER_PREFIX + "retry_interval_seconds")
          .noDefaultValue(Integer.class);

  ConfigOption<Boolean> IS_SUPPORT_TRANSACTION =
      key(WRITER_PREFIX + "is_support_transaction")
          .noDefaultValue(Boolean.class);

  ConfigOption<Integer> DELETE_THRESHOLD =
      key(WRITER_PREFIX + "delete_threshold")
          .defaultValue(10000);

  ConfigOption<Integer> DELETE_INTERVAL_MS =
      key(WRITER_PREFIX + "delete_interval_ms")
          .defaultValue(100);
}