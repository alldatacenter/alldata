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

package com.bytedance.bitsail.connector.hadoop.common;

import com.bytedance.bitsail.common.exception.ErrorCode;

/**
 * @desc:
 */
public enum TextInputFormatErrorCode implements ErrorCode {

  REQUIRED_VALUE("TextInputFormat-01", "You missed parameter which is required, please check your configuration."),
  UNSUPPORTED_ENCODING("TextInputFormat-02", "Unsupported Encoding."),
  UNSUPPORTED_COLUMN_TYPE("TextInputFormat-03", "Unsupported column type."),
  HDFS_IO("TextInputFormat-04", "IO Exception."),
  ILLEGAL_HIVE("TextInputFormat-05", "It is an illegal hive content"),
  ILLEGAL_ABASE("TextInputFormat-06", "It is an illegal abase content"),
  ILLEGAL_TEXT("TextInputFormat-07", "It is an illegal text content"),
  ILLEGAL_JSON("TextInputFormat-08", "It is an illegal json content");

  private final String code;
  private final String description;

  TextInputFormatErrorCode(String code, String description) {
    this.code = code;
    this.description = description;
  }

  @Override
  public String getCode() {
    return code;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public String toString() {
    return String.format("Code:[%s], Description:[%s].", this.code,
        this.description);
  }
}
