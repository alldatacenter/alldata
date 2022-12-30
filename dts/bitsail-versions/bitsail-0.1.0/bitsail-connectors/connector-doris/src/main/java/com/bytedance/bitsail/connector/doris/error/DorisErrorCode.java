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

package com.bytedance.bitsail.connector.doris.error;

import com.bytedance.bitsail.common.exception.ErrorCode;

public enum DorisErrorCode implements ErrorCode {

  PROXY_INIT_FAILED("DorisWriter-01", "Failed to init write proxy"),
  REQUIRED_VALUE("DorisWriter-02", "You missed parameter which is required, please check your configuration."),
  UNSUPPORTED_TABLE_MODEL("DorisWriter-03", "Unsupported doris table model."),
  LOAD_FAILED("DorisWriter-04", "Failed to load data into doris"),
  ;

  private final String code;
  private final String description;

  DorisErrorCode(String code, String description) {
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
}