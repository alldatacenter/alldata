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
import com.bytedance.bitsail.common.option.ReaderOptions;
import com.bytedance.bitsail.connector.legacy.jdbc.model.ClusterInfo;

import com.alibaba.fastjson.TypeReference;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

import static com.bytedance.bitsail.common.option.ConfigOptions.key;
import static com.bytedance.bitsail.common.option.ReaderOptions.READER_PREFIX;

/**
 * Created 2022/8/15
 */
public interface JdbcReaderOptions extends ReaderOptions.BaseReaderOptions {

  ConfigOption<Boolean> CASE_SENSITIVE =
      key(READER_PREFIX + "case_sensitive")
          .defaultValue(false);

  ConfigOption<String> TABLE_SCHEMA =
      key(READER_PREFIX + "table_schema")
          .defaultValue(StringUtils.EMPTY);

  ConfigOption<List<ClusterInfo>> CONNECTIONS =
      key(READER_PREFIX + "connections")
          .onlyReference(new TypeReference<List<ClusterInfo>>() {
          });

  /**
   * Split key, in most situations, the split_id are same to rdbms's primary key
   */
  ConfigOption<String> SPLIT_PK =
      key(READER_PREFIX + "split_pk")
          .noDefaultValue(String.class);

  /**
   * Split key's type in rdbms.
   */
  ConfigOption<String> SPLIT_PK_JDBC_TYPE =
      key(READER_PREFIX + "split_pk_jdbc_type")
          .defaultValue("int");

  ConfigOption<List<String>> SHARD_KEY =
      key(READER_PREFIX + "shard_key")
          .onlyReference(new TypeReference<List<String>>() {
          });

  ConfigOption<String> FILTER =
      key(READER_PREFIX + "filter")
          .noDefaultValue(String.class);

  ConfigOption<String> SHARD_SPLIT_MODE =
      key(READER_PREFIX + "shard_split_mode")
          .noDefaultValue(String.class);

  ConfigOption<Integer> READER_FETCH_SIZE =
      key(READER_PREFIX + "reader_fetch_size")
          .noDefaultValue(Integer.class);

  /**
   * Connection's timeout value when execute query statement.
   */
  ConfigOption<Integer> QUERY_TIMEOUT_SECONDS =
      key(READER_PREFIX + "query_timeout_seconds")
          .defaultValue(300);

  /**
   * Connection's retry number when execute query statement.
   */
  ConfigOption<Integer> QUERY_RETRY_TIMES =
      key(READER_PREFIX + "query_retry_times")
          .defaultValue(3);

  /**
   * User's custom sql for the jdbc source.
   * If user use the custom sql, the jdbc source's parallelism should be 1.
   */
  ConfigOption<String> CUSTOMIZED_SQL =
      key(READER_PREFIX + "customized_sql")
          .noDefaultValue(String.class);

  ConfigOption<Boolean> FILTER_DEAD_INSTANCES =
      key(READER_PREFIX + "filter_dead_instances")
          .defaultValue(true);

  ConfigOption<String> INIT_SQL =
      key(READER_PREFIX + "init_sql")
          .defaultValue("");

  ConfigOption<String> CONNECTION_PARAMETERS =
      key(READER_PREFIX + "connection_parameters")
          .noDefaultValue(String.class);
}
