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

package com.bytedance.bitsail.connector.legacy.larksheet.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.bytedance.bitsail.connector.legacy.larksheet.api.SheetConfig.FLOW_CONTROL_CODES;
import static com.bytedance.bitsail.connector.legacy.larksheet.api.SheetConfig.INVALID_ACCESS_TOKEN_CODES;
import static com.bytedance.bitsail.connector.legacy.larksheet.api.SheetConfig.REQUEST_FORBIDDEN;
import static com.bytedance.bitsail.connector.legacy.larksheet.api.SheetConfig.REQUEST_SUCCESS;

/**
 * Ref: <a href="https://open.feishu.cn/document/ukTMukTMukTM/ugjM14COyUjL4ITN">Server Error Codes</a>
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public abstract class OpenApiBaseResponse {

  /**
   * Lark open api error code.
   */
  protected int code;

  /**
   * Error message.
   */
  protected String msg;

  /**
   * Judge if trigger flow control by code.
   */
  public boolean isFlowLimited() {
    return FLOW_CONTROL_CODES.contains(code);
  }

  /**
   * Judge if token is expired by code.
   */
  public boolean isTokenExpired() {
    return INVALID_ACCESS_TOKEN_CODES.contains(code);
  }

  /**
   * Check if the pair '(app_id,app_secret)' has permission to api.
   */
  public boolean isForbidden() {
    return REQUEST_FORBIDDEN == code;
  }

  /**
   * Non-zero code means error.
   */
  public boolean isFailed() {
    return REQUEST_SUCCESS != code;
  }

  /**
   * Zero code means success.
   */
  public boolean isSuccessful() {
    return REQUEST_SUCCESS == code;
  }

}

