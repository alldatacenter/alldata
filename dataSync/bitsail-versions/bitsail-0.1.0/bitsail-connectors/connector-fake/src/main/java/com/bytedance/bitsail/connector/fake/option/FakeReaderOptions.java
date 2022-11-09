/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.bytedance.bitsail.connector.fake.option;

import com.bytedance.bitsail.common.option.ConfigOption;
import com.bytedance.bitsail.common.option.ReaderOptions;

import static com.bytedance.bitsail.common.option.ConfigOptions.key;
import static com.bytedance.bitsail.common.option.ReaderOptions.READER_PREFIX;

public interface FakeReaderOptions extends ReaderOptions.BaseReaderOptions {

  ConfigOption<Integer> TOTAL_COUNT =
      key(READER_PREFIX + "total_count")
          .defaultValue(10000);

  ConfigOption<Integer> RATE =
      key(READER_PREFIX + "rate")
          .defaultValue(10);

  ConfigOption<Long> LOWER_LIMIT =
      key(READER_PREFIX + "lower_limit")
          .defaultValue(0L);

  ConfigOption<Long> UPPER_LIMIT =
      key(READER_PREFIX + "upper_limit")
          .defaultValue(100_000_00L);

  ConfigOption<String> FROM_TIMESTAMP =
      key(READER_PREFIX + "from_timestamp")
          .defaultValue("1970-01-01 00:00:00");

  ConfigOption<String> TO_TIMESTAMP =
      key(READER_PREFIX + "to_timestamp")
          .defaultValue("2077-07-07 07:07:07");
}
