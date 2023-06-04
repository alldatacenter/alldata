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

package com.bytedance.bitsail.connector.legacy.ftp.error;

import com.bytedance.bitsail.common.exception.ErrorCode;

public enum FtpInputFormatErrorCode implements ErrorCode {
  REQUIRED_VALUE("FtpInputFormat-01", "You missed parameter which is required, please check your configuration."),
  SUCCESS_FILE_NOT_EXIST("FtpInputFormat-02", "success file isn't ready, please check success file is exist."),
  CONNECTION_ERROR("FtpInputFormat-03", "Error occured while getting ftp server connection."),
  CONFIG_ERROR("FtpInputFormat-04", "Config parameter is error."),
  FILEPATH_NOT_EXIST("FtpInputFormat-05", "File paths isn't existed, please check filePath is correct.");

  private final String code;
  private final String description;

  FtpInputFormatErrorCode(String code, String description) {
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
