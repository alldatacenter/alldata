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

package com.bytedance.bitsail.common.option;

import com.bytedance.bitsail.common.model.ColumnInfo;

import com.alibaba.fastjson.TypeReference;

import java.util.List;
import java.util.Map;

import static com.bytedance.bitsail.common.option.ConfigOptions.key;

/**
 * The set of configuration options relating to reader config.
 */
public interface ReaderOptions {
  String JOB_READER = "job.reader";
  String READER_PREFIX = JOB_READER + ".";

  ConfigOption<String> READER_CLASS =
      key(READER_PREFIX + "class")
          .noDefaultValue(String.class);

  /**
   * Metric tag for indicating reader type.
   */
  ConfigOption<String> READER_METRIC_TAG_NAME =
      key(READER_PREFIX + "metric_tag_name")
          .noDefaultValue(String.class);

  /**
   * for describing different source operator
   */
  ConfigOption<String> SOURCE_OPERATOR_DESC =
      key(READER_PREFIX + "source_operator.desc")
          .noDefaultValue(String.class);

  /**
   * "job.reader.reader_conf_list": [
   * {
   * "job.reader.connector.type": "kafka",
   * "job.reader.connector.topic": "topic_1",
   * ...
   * },
   * {
   * "job.reader.connector.type": "kafka",
   * "job.reader.connector.topic": "topic_2",
   * ...
   * }
   * ]
   */
  ConfigOption<List<Map<String, Object>>> READER_CONFIG_LIST =
      key(READER_PREFIX + "reader_conf_list")
          .onlyReference(new TypeReference<List<Map<String, Object>>>() {
          });

  interface BaseReaderOptions {

    ConfigOption<List<ColumnInfo>> COLUMNS =
        key(READER_PREFIX + "columns")
            .onlyReference(new TypeReference<List<ColumnInfo>>() {
            });

    ConfigOption<Integer> READER_PARALLELISM_NUM =
        key(READER_PREFIX + "reader_parallelism_num")
            .noDefaultValue(Integer.class);

    ConfigOption<String> CHARSET_NAME =
        key(READER_PREFIX + "charset_name")
            .defaultValue("");

    ConfigOption<String> DB_NAME =
        key(READER_PREFIX + "db_name")
            .noDefaultValue(String.class);

    ConfigOption<String> TABLE_NAME =
        key(READER_PREFIX + "table_name")
            .noDefaultValue(String.class);

    ConfigOption<String> PARTITION =
        key(READER_PREFIX + "partition")
            .noDefaultValue(String.class);

    ConfigOption<String> USER_NAME =
        key(READER_PREFIX + "user_name")
            .noDefaultValue(String.class);

    ConfigOption<String> PASSWORD =
        key(READER_PREFIX + "password")
            .noDefaultValue(String.class);

    ConfigOption<String> CONTENT_TYPE =
        key(READER_PREFIX + "content_type")
            .noDefaultValue(String.class);
  }
}
