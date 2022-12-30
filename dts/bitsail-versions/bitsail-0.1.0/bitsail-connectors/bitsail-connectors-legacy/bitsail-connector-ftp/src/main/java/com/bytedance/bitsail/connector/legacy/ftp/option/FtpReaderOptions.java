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

package com.bytedance.bitsail.connector.legacy.ftp.option;

import com.bytedance.bitsail.common.option.ConfigOption;
import com.bytedance.bitsail.common.option.ReaderOptions;

import static com.bytedance.bitsail.common.option.ConfigOptions.key;
import static com.bytedance.bitsail.common.option.ReaderOptions.READER_PREFIX;

/**
 * Ftp Options
 */
public interface FtpReaderOptions extends ReaderOptions.BaseReaderOptions {
  ConfigOption<String> READER_CLASS =
      key(READER_PREFIX + "class")
          .defaultValue("com.bytedance.bitsail.connector.legacy.ftp.source.FtpInputFormat");

  ConfigOption<Integer> PORT =
      key(READER_PREFIX + "port")
          .defaultValue(21);

  ConfigOption<String> HOST =
      key(READER_PREFIX + "host")
          .noDefaultValue(String.class);

  ConfigOption<String> PATH_LIST =
      key(READER_PREFIX + "path_list")
          .noDefaultValue(String.class);

  ConfigOption<String> USER =
      key(READER_PREFIX + "user")
          .noDefaultValue(String.class);

  ConfigOption<String> PASSWORD =
      key(READER_PREFIX + "password")
          .noDefaultValue(String.class);

  ConfigOption<Boolean> SKIP_FIRST_LINE =
      key(READER_PREFIX + "skip_first_line")
          .defaultValue(false);

  ConfigOption<Boolean> ENABLE_SUCCESS_FILE_CHECK =
      key(READER_PREFIX + "enable_success_file_check")
          .defaultValue(true);

  ConfigOption<String> SUCCESS_FILE_PATH =
      key(READER_PREFIX + "success_file_path")
          .noDefaultValue(String.class);

  // client connection timeout in milliseconds
  ConfigOption<Integer> TIME_OUT =
      key(READER_PREFIX + "time_out")
          .defaultValue(5000);

  // max success file retry time, default 60
  ConfigOption<Integer> MAX_RETRY_TIME =
      key(READER_PREFIX + "max_retry_time")
          .defaultValue(60);

  // interval between retries, default 60s
  ConfigOption<Integer> RETRY_INTERVAL_MS =
      key(READER_PREFIX + "retry_interval_ms")
          .defaultValue(60000);

  // protocol, FTP or SFTP
  ConfigOption<String> PROTOCOL =
      key(READER_PREFIX + "protocol")
          .defaultValue("FTP");

  // In ftp mode, connect pattern can be PASV or PORT
  // In sftp mode, connect pattern is NULL
  ConfigOption<String> CONNECT_PATTERN =
      key(READER_PREFIX + "connect_pattern")
          .defaultValue("PASV");

}

