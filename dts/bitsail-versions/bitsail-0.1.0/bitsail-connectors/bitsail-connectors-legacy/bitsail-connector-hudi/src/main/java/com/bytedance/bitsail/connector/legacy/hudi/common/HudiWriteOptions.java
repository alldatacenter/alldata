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

package com.bytedance.bitsail.connector.legacy.hudi.common;

import com.bytedance.bitsail.common.option.ConfigOption;

import static com.bytedance.bitsail.common.option.ConfigOptions.key;
import static com.bytedance.bitsail.common.option.WriterOptions.WRITER_PREFIX;

public interface HudiWriteOptions {
  ConfigOption<String> FORMAT_TYPE =
      key(WRITER_PREFIX + "format_type")
          .noDefaultValue(String.class);

  ConfigOption<Boolean> FORMAT_IGNORE_PARSE_ERRORS =
      key(WRITER_PREFIX + "ignore-parse-errors")
          .defaultValue(false);

  ConfigOption<String> FORMAT_TIMESTAMP_FORMAT =
      key(WRITER_PREFIX + "timestamp-format")
          .defaultValue("iso_8601");

  ConfigOption<Integer> COMPACTION_MAX_PARALLELISM =
      key(WRITER_PREFIX + "compaction-max-parallelism")
          .defaultValue(50);

  ConfigOption<Integer> COMPACTION_FILE_PER_TASK =
      key(WRITER_PREFIX + "compaction-file-per-task")
          .defaultValue(2);

}
