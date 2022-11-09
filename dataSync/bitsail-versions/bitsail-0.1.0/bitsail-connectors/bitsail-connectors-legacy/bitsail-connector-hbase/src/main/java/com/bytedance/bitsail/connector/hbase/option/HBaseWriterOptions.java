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

package com.bytedance.bitsail.connector.hbase.option;

import com.bytedance.bitsail.common.annotation.Essential;
import com.bytedance.bitsail.common.option.ConfigOption;
import com.bytedance.bitsail.common.option.WriterOptions;

import com.alibaba.fastjson.TypeReference;

import java.util.Map;

import static com.bytedance.bitsail.common.option.ConfigOptions.key;
import static com.bytedance.bitsail.common.option.WriterOptions.WRITER_PREFIX;

public interface HBaseWriterOptions extends WriterOptions.BaseWriterOptions {

  /**
   * Table to write.
   */
  @Essential
  ConfigOption<String> TABLE_NAME =
      key(WRITER_PREFIX + "table")
          .noDefaultValue(String.class);

  ConfigOption<String> ENCODING =
      key(WRITER_PREFIX + "encoding")
          .defaultValue("UTF-8");

  /**
   * How to process null value.<br/>
   * 1. SKIP: ignore this value (will not be insert to hbase)<br/>
   * 2. EMPTY: insert empty byte[]
   */
  ConfigOption<String> NULL_MODE =
      key(WRITER_PREFIX + "null_mode")
          .defaultValue("skip");

  /**
   * If enable Write-ahead logging.
   */
  ConfigOption<Boolean> WAL_FLAG =
      key(WRITER_PREFIX + "wal_flag")
          .defaultValue(false);

  ConfigOption<Long> WRITE_BUFFER_SIZE =
      key(WRITER_PREFIX + "write_buffer_size")
          .defaultValue(8 * 1024 * 1024L);

  /**
   * Column name of RowKey. Support expressions like:<br/>
   * md5(test_$(col1)_test_$(col2)_test)<br/>
   * test_$(col1)_test_$(col2)_test<br/>
   * _md5(test_$(col1)_test_$(col2)_test)_<br/>
   * $(cf:name)_md5($(cf:id)_split_$(cf:age))<br/>
   */
  ConfigOption<String> ROW_KEY_COLUMN =
      key(WRITER_PREFIX + "row_key_column")
          .noDefaultValue(String.class);

  /**
   * Determine the timestamp that will be inserted into hbase.
   */
  ConfigOption<Map<String, Object>> VERSION_COLUMN =
      key(WRITER_PREFIX + "version_column")
          .onlyReference(new TypeReference<Map<String, Object>>(){});

  /**
   * HBase configuration for creating connections.
   */
  ConfigOption<Map<String, Object>> HBASE_CONF =
      key(WRITER_PREFIX + "hbase_conf")
          .onlyReference(new TypeReference<Map<String, Object>>(){});

}
