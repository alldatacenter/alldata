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

package com.bytedance.bitsail.connector.elasticsearch.option;

import com.bytedance.bitsail.common.option.ConfigOption;
import com.bytedance.bitsail.common.option.WriterOptions;

import com.alibaba.fastjson.TypeReference;

import java.util.List;

import static com.bytedance.bitsail.common.option.ConfigOptions.key;

public interface ElasticsearchWriterOptions extends WriterOptions.BaseWriterOptions {
  ConfigOption<List<String>> ES_HOSTS =
      key(WriterOptions.WRITER_PREFIX + "es_hosts")
          .onlyReference(new TypeReference<List<String>>() {
          });

  ConfigOption<String> REQUEST_PATH_PREFIX =
      key(WriterOptions.WRITER_PREFIX + "request_path_prefix")
          .noDefaultValue(String.class);

  ConfigOption<Integer> CONNECTION_REQUEST_TIMEOUT_MS =
      key(WriterOptions.WRITER_PREFIX + "connection_request_timeout_ms")
          .defaultValue(10000);

  ConfigOption<Integer> CONNECTION_TIMEOUT_MS =
      key(WriterOptions.WRITER_PREFIX + "connection_timeout_ms")
          .defaultValue(10000);

  ConfigOption<Integer> SOCKET_TIMEOUT_MS =
      key(WriterOptions.WRITER_PREFIX + "socket_timeout_ms")
          .defaultValue(60000);

  ConfigOption<Integer> MAX_IGNORE_FAILED_REQUEST_THRESHOLD =
      key(WriterOptions.WRITER_PREFIX + "max_ignore_failed_request_threshold")
          .defaultValue(0);

  ConfigOption<Integer> BULK_FLUSH_MAX_ACTIONS =
      key(WriterOptions.WRITER_PREFIX + "bulk_flush_max_actions")
          .defaultValue(300);

  ConfigOption<Integer> BULK_FLUSH_MAX_SIZE_MB =
      key(WriterOptions.WRITER_PREFIX + "bulk_flush_max_size_mb")
          .defaultValue(10);

  ConfigOption<Long> BULK_FLUSH_INTERVAL_MS =
      key(WriterOptions.WRITER_PREFIX + "bulk_flush_interval_ms")
          .defaultValue(10000L);

  ConfigOption<String> BULK_BACKOFF_POLICY =
      key(WriterOptions.WRITER_PREFIX + "bulk_backoff_policy")
          .defaultValue("EXPONENTIAL");

  ConfigOption<Integer> BULK_BACKOFF_DELAY_MS =
      key(WriterOptions.WRITER_PREFIX + "bulk_backoff_delay_ms")
          .defaultValue(100);

  ConfigOption<Integer> BULK_BACKOFF_MAX_RETRY_COUNT =
      key(WriterOptions.WRITER_PREFIX + "bulk_backoff_max_retry_count")
          .defaultValue(5);

  ConfigOption<String> ES_INDEX =
      key(WriterOptions.WRITER_PREFIX + "es_index")
          .noDefaultValue(String.class);

  ConfigOption<String> ES_OPERATION_TYPE =
      key(WriterOptions.WRITER_PREFIX + "es_operation_type")
          .defaultValue("index");

  ConfigOption<String> ES_DYNAMIC_INDEX_FIELD =
      key(WriterOptions.WRITER_PREFIX + "es_dynamic_index_field")
          .noDefaultValue(String.class);

  ConfigOption<String> ES_OPERATION_TYPE_FIELD =
      key(WriterOptions.WRITER_PREFIX + "es_operation_type_field")
          .noDefaultValue(String.class);

  ConfigOption<String> ES_VERSION_FIELD =
      key(WriterOptions.WRITER_PREFIX + "es_version_field")
          .noDefaultValue(String.class);

  /**
   * How to build elasticsearch from the columns.
   */
  ConfigOption<String> ES_ID_FIELDS =
      key(WriterOptions.WRITER_PREFIX + "es_id_fields")
          .defaultValue("");

  ConfigOption<Boolean> IGNORE_BLANK_VALUE =
      key(WriterOptions.WRITER_PREFIX + "ignore_blank_value")
          .defaultValue(false);

  ConfigOption<Boolean> FLATTEN_MAP =
      key(WriterOptions.WRITER_PREFIX + "flatten_map")
          .defaultValue(false);

  ConfigOption<String> SHARD_ROUTING_FIELDS =
      key(WriterOptions.WRITER_PREFIX + "es_shard_routing_fields")
          .defaultValue("");

  ConfigOption<String> DOC_EXCLUDE_FIELDS =
      key(WriterOptions.WRITER_PREFIX + "doc_exclude_fields")
          .defaultValue("");

  ConfigOption<String> DOC_ID_DELIMITER =
      key(WriterOptions.WRITER_PREFIX + "id_delimiter")
          .defaultValue("#");


  /**
   * Custom fastjson serialization method, multiple values are separated by commas
   *
   * @see com.alibaba.fastjson.serializer.SerializerFeature
   */
  ConfigOption<String> JSON_SERIALIZER_FEATURES =
      key(WriterOptions.WRITER_PREFIX + "json_serializer_features")
          .noDefaultValue(String.class);
}
