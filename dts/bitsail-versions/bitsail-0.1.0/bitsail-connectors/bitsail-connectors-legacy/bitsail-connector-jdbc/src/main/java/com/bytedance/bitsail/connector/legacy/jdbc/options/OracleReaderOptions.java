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

package com.bytedance.bitsail.connector.legacy.jdbc.options;

import com.bytedance.bitsail.common.option.ConfigOption;

import static com.bytedance.bitsail.common.option.ConfigOptions.key;
import static com.bytedance.bitsail.common.option.ReaderOptions.READER_PREFIX;

/**
 * Created 2022/8/15
 */
public interface OracleReaderOptions extends JdbcReaderOptions {

  /**
   * Odbc connect type.
   */
  ConfigOption<String> SERVICE_NAME_TYPE =
      key(READER_PREFIX + "service_name_type")
          .defaultValue("service_name");

  ConfigOption<String> SERVICE_NAME =
      key(READER_PREFIX + "service_name")
          .noDefaultValue(String.class);

  ConfigOption<String> INTERVAL_HANDLE_MODE =
      key(READER_PREFIX + "interval_handle_mode")
          .defaultValue("numeric");
}
