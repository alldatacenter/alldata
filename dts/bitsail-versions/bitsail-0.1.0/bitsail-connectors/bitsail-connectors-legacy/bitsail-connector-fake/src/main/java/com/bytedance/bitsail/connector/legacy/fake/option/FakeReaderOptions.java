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

package com.bytedance.bitsail.connector.legacy.fake.option;

import com.bytedance.bitsail.common.option.ConfigOption;
import com.bytedance.bitsail.common.option.ReaderOptions;

import com.alibaba.fastjson.TypeReference;

import java.util.List;
import java.util.Map;

import static com.bytedance.bitsail.common.option.ConfigOptions.key;
import static com.bytedance.bitsail.common.option.ReaderOptions.READER_PREFIX;

/**
 * Created 2022/8/16
 */
public interface FakeReaderOptions extends ReaderOptions.BaseReaderOptions {

  ConfigOption<Integer> TOTAL_COUNT =
      key(READER_PREFIX + "total_count")
          .defaultValue(10000);

  ConfigOption<Integer> RATE =
      key(READER_PREFIX + "rate")
          .defaultValue(10);

  ConfigOption<Double> RANDOM_NULL_RATE =
      key(READER_PREFIX + "random_null_rate")
          .defaultValue(0.1);

  ConfigOption<String> UNIQUE_FIELDS =
      key(READER_PREFIX + "unique_fields")
          .noDefaultValue(String.class);

  ConfigOption<Boolean> USE_BITSAIL_TYPE =
      key(READER_PREFIX + "use_bitsail_type")
          .defaultValue(true);

  /**
   * This option defines the columns with fixed value.<br/>
   * The format is:<br/>
   * [{"name":"column_A", "fixed_value":"value_A"}, {"name":"column_B", "fixed_value":"value_B"}, ...]<br/>
   * Note that the column `name` should appear in `job.writer.columns`.
   */
  ConfigOption<List<Map<String, String>>> COLUMNS_WITH_FIXED_VALUE =
      key(READER_PREFIX + "columns_with_fixed_value")
          .onlyReference(new TypeReference<List<Map<String, String>>>(){});
}
