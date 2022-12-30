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

import com.bytedance.bitsail.common.annotation.Essential;
import com.bytedance.bitsail.common.model.ColumnInfo;

import com.alibaba.fastjson.TypeReference;

import java.util.List;
import java.util.Map;

import static com.bytedance.bitsail.common.option.ConfigOptions.key;

/**
 * The set of configuration options relating to writer config.
 */
public interface WriterOptions {
  String JOB_WRITER = "job.writer";
  String WRITER_PREFIX = JOB_WRITER + ".";

  /**
   * A list of writer configurations, each of which is applied to one writer.
   */
  ConfigOption<List<Map<String, Object>>> WRITER_CONFIG_LIST =
      key(WRITER_PREFIX + "writer_conf_list")
          .onlyReference(new TypeReference<List<Map<String, Object>>>() {
          });

  /**
   * Class name of writer.
   */
  @Essential
  ConfigOption<String> WRITER_CLASS =
      key(WRITER_PREFIX + "class")
          .noDefaultValue(String.class);

  /**
   * Metric tag for indicating writer type.
   */
  ConfigOption<String> WRITER_METRIC_TAG_NAME =
      key(WRITER_PREFIX + "metric_tag_name")
          .noDefaultValue(String.class);

  /**
   * Basic options for all writer.
   */
  interface BaseWriterOptions {

    /**
     * Columns information of writer schema.
     */
    ConfigOption<List<ColumnInfo>> COLUMNS =
        key(WRITER_PREFIX + "columns")
            .onlyReference(new TypeReference<List<ColumnInfo>>() {
            });

    /**
     * Parallelism num of writer.
     */
    ConfigOption<Integer> WRITER_PARALLELISM_NUM =
        key(WRITER_PREFIX + "writer_parallelism_num")
            .noDefaultValue(Integer.class);

    /**
     * Partitions to write.
     */
    ConfigOption<String> PARTITION =
        key(WRITER_PREFIX + "partition")
            .noDefaultValue(String.class);

    /**
     * Database to write.
     */
    ConfigOption<String> DB_NAME =
        key(WRITER_PREFIX + "db_name")
            .noDefaultValue(String.class);

    /**
     * Target table to write.
     */
    ConfigOption<String> TABLE_NAME =
        key(WRITER_PREFIX + "table_name")
            .noDefaultValue(String.class);

    ConfigOption<String> USER_NAME =
        key(WriterOptions.WRITER_PREFIX + "user_name")
            .noDefaultValue(String.class);

    ConfigOption<String> PASSWORD =
        key(WriterOptions.WRITER_PREFIX + "password")
            .noDefaultValue(String.class);

    ConfigOption<String> WRITE_MODE =
        key(WRITER_PREFIX + "write_mode")
            .defaultValue("overwrite");
  }
}
