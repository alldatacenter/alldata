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

package com.bytedance.bitsail.connector.legacy.hive.option;

import com.bytedance.bitsail.common.option.ConfigOption;
import com.bytedance.bitsail.common.option.WriterOptions;

import com.alibaba.fastjson.TypeReference;

import java.util.Map;

import static com.bytedance.bitsail.common.option.ConfigOptions.key;
import static com.bytedance.bitsail.common.option.WriterOptions.WRITER_PREFIX;

/**
 * Hive Options
 */
public interface HiveWriterOptions extends WriterOptions.BaseWriterOptions {

  ConfigOption<Boolean> CONVERT_ERROR_COLUMN_AS_NULL =
      key(WRITER_PREFIX + "convert_error_column_as_null")
          .defaultValue(false);

  ConfigOption<String> HIVE_PARQUET_COMPRESSION =
      key(WRITER_PREFIX + "hive_parquet_compression")
          .defaultValue("gzip");

  ConfigOption<Boolean> DATE_TO_STRING_AS_LONG =
      key(WRITER_PREFIX + "date_to_string_as_long")
          .defaultValue(false);

  ConfigOption<String> DATE_PRECISION =
      key(WRITER_PREFIX + "date_precision")
          .defaultValue("second");

  ConfigOption<Map<String, String>> CUSTOMIZED_HADOOP_CONF =
      key(WRITER_PREFIX + "customized_hadoop_conf")
          .onlyReference(new TypeReference<Map<String, String>>() {
          });

  ConfigOption<String> HIVE_METASTORE_PROPERTIES =
      key(WRITER_PREFIX + "metastore_properties")
          .noDefaultValue(String.class);
}
