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

package com.bytedance.bitsail.connector.hadoop.option;

import com.bytedance.bitsail.common.annotation.Essential;
import com.bytedance.bitsail.common.option.ConfigOption;
import com.bytedance.bitsail.common.option.ReaderOptions;

import static com.bytedance.bitsail.common.option.ConfigOptions.key;
import static com.bytedance.bitsail.common.option.ReaderOptions.READER_PREFIX;

/**
 * Created 2022/8/16
 */
public interface HadoopReaderOptions extends ReaderOptions.BaseReaderOptions {

  @Essential
  ConfigOption<String> PATH_LIST =
      key(READER_PREFIX + "path_list")
          .noDefaultValue(String.class);

  /**
   * Support hadoop configuration.
   */
  ConfigOption<String> HADOOP_CONF =
      key(READER_PREFIX + "hadoop_conf")
          .noDefaultValue(String.class);

  ConfigOption<String> SOURCE_ENGINE =
      key(READER_PREFIX + "source_engine")
          .noDefaultValue(String.class);

  ConfigOption<Long> SKIP_LINES =
      key(READER_PREFIX + "skip_lines")
          .defaultValue(0L);

  ConfigOption<String> HADOOP_INPUT_FORMAT_CLASS =
      key(READER_PREFIX + "hadoop_inputformat_class")
          .defaultValue("org.apache.hadoop.mapred.TextInputFormat");
}