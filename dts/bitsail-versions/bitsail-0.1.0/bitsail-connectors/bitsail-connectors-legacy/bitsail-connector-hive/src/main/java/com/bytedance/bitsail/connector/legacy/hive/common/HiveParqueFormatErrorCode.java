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

package com.bytedance.bitsail.connector.legacy.hive.common;

import com.bytedance.bitsail.common.exception.ErrorCode;

/**
 * @desc:
 */
public enum HiveParqueFormatErrorCode implements ErrorCode {

  REQUIRED_VALUE("HiveParquetOutput-01", "You missed parameter which is required, please check your configuration."),
  ILLEGAL_VALUE("HiveParquetOutput-02", "The parameter value in your configuration is not valid."),
  HIVE_METASTORE_EXCEPTION("HiveParquetOutput-03", "Connect to or interact with hive metastore error."),
  HDFS_EXCEPTION("HiveParquetOutput-04", "HDFS error."),
  VALIDATION_EXCEPTION("HiveParquetOutput-05", "Result validation error."),
  ;

  private final String code;
  private final String description;

  HiveParqueFormatErrorCode(String code, String description) {
    this.code = code;
    this.description = description;
  }

  @Override
  public String getCode() {
    return this.code;
  }

  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public String toString() {
    return String.format("Code:[%s], Description:[%s].", this.code,
        this.description);
  }
}
