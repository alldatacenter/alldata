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

import com.bytedance.bitsail.common.option.ConfigOption;
import com.bytedance.bitsail.common.option.WriterOptions;

import static com.bytedance.bitsail.common.option.ConfigOptions.key;
import static com.bytedance.bitsail.common.option.WriterOptions.WRITER_PREFIX;

public interface MongoDBWriterOptions extends WriterOptions.BaseWriterOptions {

  /**
   * There are three ways to connect to mongo client:<br/>
   * 1. url: Connect with url.<br/>
   * 2. host_without_credential : Use host/port without authentication.<br/>
   * 3. host_with_credential : Use host/port with authentication.
   */
  ConfigOption<String> CLIENT_MODE =
      key(WRITER_PREFIX + "client_mode")
          .defaultValue("host_with_credential");

  /**
   * Url for connect to mongo client.
   */
  ConfigOption<String> MONGO_URL =
      key(WRITER_PREFIX + "mongo_url")
          .noDefaultValue(String.class);

  /**
   * Multi hosts for connection. Format is: host1:port1,host2:port2...
   */
  ConfigOption<String> MONGO_HOSTS_STR =
      key(WRITER_PREFIX + "mongo_hosts_str")
          .noDefaultValue(String.class);

  ConfigOption<String> MONGO_HOST =
      key(WRITER_PREFIX + "mongo_host")
          .noDefaultValue(String.class);

  ConfigOption<Integer> MONGO_PORT =
      key(WRITER_PREFIX + "mongo_port")
          .noDefaultValue(Integer.class);

  ConfigOption<String> USER_NAME =
      key(WRITER_PREFIX + "user_name")
          .noDefaultValue(String.class);

  ConfigOption<String> PASSWORD =
      key(WRITER_PREFIX + "password")
          .noDefaultValue(String.class);

  ConfigOption<String> DB_NAME =
      key(WRITER_PREFIX + "db_name")
          .noDefaultValue(String.class);

  ConfigOption<String> AUTH_DB_NAME =
      key(WRITER_PREFIX + "auth_db_name")
          .noDefaultValue(String.class);

  ConfigOption<String> COLLECTION_NAME =
      key(WRITER_PREFIX + "collection_name")
          .noDefaultValue(String.class);

  /**
   * The date format when transforming date value.
   */
  ConfigOption<String> DATE_FORMAT_PATTERN =
      key(WRITER_PREFIX + "date_format_pattern")
          .noDefaultValue(String.class);

  /**
   * Timezone when transforming date value.
   */
  ConfigOption<String> DATE_TIME_ZONE =
      key(WRITER_PREFIX + "date_time_zone")
          .defaultValue("GMT+8");

  /**
   * Pre-executed sql before write data to mongodb.
   */
  ConfigOption<String> PRE_SQL =
      key(WRITER_PREFIX + "pre_sql")
          .noDefaultValue(String.class);

  /**
   * Batch write size.
   */
  ConfigOption<Integer> BATCH_SIZE =
      key(WRITER_PREFIX + "batch_size")
          .defaultValue(100);

  /**
   * In insert mode, will clear document according to partition_key.
   */
  ConfigOption<String> PARTITION_KEY =
      key(WRITER_PREFIX + "partition_key")
          .noDefaultValue(String.class);

  /**
   * In insert mode, will clear document according to partition_value.
   */
  ConfigOption<String> PARTITION_VALUE =
      key(WRITER_PREFIX + "partition_value")
          .noDefaultValue(String.class);

  /**
   * Format of partition value. Can be yyyyMMdd or yyyy-MM-dd.
   */
  ConfigOption<String> PARTITION_PATTERN_FORMAT =
      key(WRITER_PREFIX + "partition_pattern_format")
          .noDefaultValue(String.class);

  /**
   * Write mode:<br/>
   * 1. insert: Throw exception when conflicts were raise.<br/>
   * 2. overwrite: Overwrite conflicted key.
   */
  ConfigOption<String> WRITE_MODE =
      key(WRITER_PREFIX + "write_mode")
          .defaultValue("overwrite");

  /**
   * Unique key used in overwrite mode.
   */
  ConfigOption<String> UNIQUE_KEY =
      key(WRITER_PREFIX + "unique_key")
          .defaultValue("_id");

  /**
   * Max connection numbers of connection pool.
   */
  ConfigOption<Integer> MAX_CONNECTION_PER_HOST =
      key(WRITER_PREFIX + "max_connection_per_host")
          .defaultValue(100);

  /**
   * Max number of blocked threads.
   */
  ConfigOption<Integer> THREADS_ALLOWED_TO_BLOCK_FOR_CONNECTION_MULTIPLIER =
      key(WRITER_PREFIX + "thread_allowed_to_block_connection_multiplier")
          .defaultValue(5);

  /**
   * Timeout for create connections.
   */
  ConfigOption<Integer> CONNECT_TIMEOUT_MS =
      key(WRITER_PREFIX + "connect_timeout_ms")
          .defaultValue(10000);

  /**
   * Timeout for get a connection from pool.<br/>
   * 0 means never wait. Negative value means no timeout.
   */
  ConfigOption<Integer> MAX_WAIT_TIME_MS =
      key(WRITER_PREFIX + "max_wait_time_ms")
          .defaultValue(120000);

  /**
   * Timeout for socket. 0 means no limit.
   */
  ConfigOption<Integer> SOCKET_TIMEOUT_MS =
      key(WRITER_PREFIX + "socket_timeout_ms")
          .defaultValue(0);

  /**
   * Write strategy. Values can be 0, 1, 2, 3.
   */
  ConfigOption<Integer> WRITE_CONCERN =
      key(WRITER_PREFIX + "write_concern")
          .defaultValue(1);
}

