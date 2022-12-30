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

package com.bytedance.bitsail.parser.option;

import com.bytedance.bitsail.common.option.ConfigOption;

import com.alibaba.fastjson.TypeReference;

import java.util.Map;

import static com.bytedance.bitsail.common.option.CommonOptions.COMMON_PREFIX;
import static com.bytedance.bitsail.common.option.ConfigOptions.key;

public class RowParserOptions {

  public static final ConfigOption<Boolean> CASE_INSENSITIVE =
      key(COMMON_PREFIX + "case_insensitive")
          .defaultValue(true);

  public static final ConfigOption<String> JSON_SERIALIZER_FEATURES =
      key(COMMON_PREFIX + "json_serializer_features")
          .noDefaultValue(String.class);

  public static final ConfigOption<Boolean> CONVERT_ERROR_COLUMN_AS_NULL =
      key(COMMON_PREFIX + "convert_error_column_as_null")
          .defaultValue(false);

  public static final ConfigOption<Boolean> USE_ARRAY_MAP_COLUMN =
      key(COMMON_PREFIX + "use_array_map_column")
          .defaultValue(true);

  public static final ConfigOption<String> RAW_COLUMN =
      key(COMMON_PREFIX + "raw_column")
          .defaultValue("");

  public static final ConfigOption<Boolean> LAZY_PARSE =
      key(COMMON_PREFIX + "lazy_parse")
          .defaultValue(false);

  public static final ConfigOption<String> PROTO_DESCRIPTOR =
      key(COMMON_PREFIX + "proto.descriptor")
          .noDefaultValue(String.class);

  public static final ConfigOption<String> PROTO_CLASS_NAME =
      key(COMMON_PREFIX + "proto.class_name")
          .noDefaultValue(String.class);

  public static final ConfigOption<Map<String, String>> PB_FEATURE =
      key(COMMON_PREFIX + "pb_feature")
          .onlyReference(new TypeReference<Map<String, String>>() {
          });

  public static final ConfigOption<String> COLUMN_DELIMITER =
      key(COMMON_PREFIX + "column_delimiter")
          .defaultValue("\\.");
  public static final ConfigOption<Integer> CONTENT_OFFSET =
      key(COMMON_PREFIX + "content_offset")
          .defaultValue(0);

  public static final ConfigOption<String> CSV_DELIMITER =
      key(COMMON_PREFIX + "csv_delimiter")
          .defaultValue(",");

  public static final ConfigOption<Character> CSV_ESCAPE =
      key(COMMON_PREFIX + "csv_escape")
          .noDefaultValue(Character.class);

  public static final ConfigOption<Character> CSV_QUOTE =
      key(COMMON_PREFIX + "csv_quote")
          .noDefaultValue(Character.class);

  public static final ConfigOption<String> CSV_WITH_NULL_STRING =
      key(COMMON_PREFIX + "csv_with_null_string")
          .noDefaultValue(String.class);

  public static final ConfigOption<Character> CSV_MULTI_DELIMITER_REPLACER =
      key(COMMON_PREFIX + "csv_multi_delimiter_replace_char")
          .defaultValue('§');
}
