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

package com.bytedance.bitsail.common.exception;

public enum CommonErrorCode implements ErrorCode {

  CONFIG_ERROR("Common-00", "The configuration file which you supplied is not correct, please check."),
  CONVERT_NOT_SUPPORT("Common-01", "There is dirty record when doing a transmission which caused data type convert error."),
  CONVERT_OVER_FLOW("Common-02", "There is dirty record when doing a transmission which caused data type convert overflow."),
  RUNTIME_ERROR("Common-03", "Internal runtime error."),
  UNSUPPORTED_ENCODING("Common-04", "Encoding type not support in "),
  INTERNAL_ERROR("Common-05", "Internal error found in BitSail. This is usually a bug."),
  TOO_MANY_DIRTY_RECORDS("Common-06", "BitSail found too many dirty records."),
  LACK_NECESSARY_FIELDS("Common-07", "The configuration file lacks necessary fields."),
  PLUGIN_ERROR("Common-08", "plugin runtime error"),
  DEPENDENCY_ERROR("Common-09", "External dependency error."),
  UNSUPPORTED_COLUMN_TYPE("Common-10", "Unsupported column type."),
  HDFS_EXCEPTION("Common-11", "HDFS error."),
  HIVE_METASTORE_EXCEPTION("Common-12", "Failed to connect to the hive metastore."),
  ;

  private final String code;

  private final String describe;

  CommonErrorCode(String code, String describe) {
    this.code = code;
    this.describe = describe;
  }

  @Override
  public String getCode() {
    return code;
  }

  @Override
  public String getDescription() {
    return describe;
  }

  @Override
  public String toString() {
    return String.format("Code:[%s], Describe:[%s]", this.code,
        this.describe);
  }
}
