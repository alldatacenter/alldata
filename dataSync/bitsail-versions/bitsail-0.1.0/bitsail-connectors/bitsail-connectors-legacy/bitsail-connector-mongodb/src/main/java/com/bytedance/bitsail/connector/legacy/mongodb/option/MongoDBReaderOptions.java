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

package com.bytedance.bitsail.connector.legacy.mongodb.option;

import com.bytedance.bitsail.common.annotation.Essential;
import com.bytedance.bitsail.common.option.ConfigOption;
import com.bytedance.bitsail.common.option.ReaderOptions;

import static com.bytedance.bitsail.common.option.ConfigOptions.key;
import static com.bytedance.bitsail.common.option.ReaderOptions.READER_PREFIX;

/**
 * MongoDB Reader Options
 */
public interface MongoDBReaderOptions extends ReaderOptions.BaseReaderOptions {

  ConfigOption<String> USER_NAME =
      key(READER_PREFIX + "user_name")
          .noDefaultValue(String.class);

  ConfigOption<String> PASSWORD =
      key(READER_PREFIX + "password")
          .noDefaultValue(String.class);

  @Essential
  ConfigOption<String> DB_NAME =
      key(READER_PREFIX + "db_name")
          .noDefaultValue(String.class);

  ConfigOption<String> AUTH_DB_NAME =
      key(READER_PREFIX + "auth_db_name")
          .noDefaultValue(String.class);

  @Essential
  ConfigOption<String> COLLECTION_NAME =
      key(READER_PREFIX + "collection_name")
          .noDefaultValue(String.class);

  ConfigOption<String> HOSTS_STR =
      key(READER_PREFIX + "hosts_str")
          .noDefaultValue(String.class);

  ConfigOption<String> HOST =
      key(READER_PREFIX + "host")
          .noDefaultValue(String.class);

  ConfigOption<Integer> PORT =
      key(READER_PREFIX + "port")
          .noDefaultValue(Integer.class);

  /**
   * Columns for ranging. Must belong to index key(s).
   */
  @Essential
  ConfigOption<String> SPLIT_PK =
      key(READER_PREFIX + "split_pk")
          .noDefaultValue(String.class);

  ConfigOption<Integer> READER_FETCH_SIZE =
      key(READER_PREFIX + "reader_fetch_size")
          .defaultValue(100000);

  ConfigOption<String> FILTER =
      key(READER_PREFIX + "filter")
          .noDefaultValue(String.class);

  ConfigOption<String> SPLIT_MODE =
      key(READER_PREFIX + "split_mode")
          .defaultValue("parallelism");
}
