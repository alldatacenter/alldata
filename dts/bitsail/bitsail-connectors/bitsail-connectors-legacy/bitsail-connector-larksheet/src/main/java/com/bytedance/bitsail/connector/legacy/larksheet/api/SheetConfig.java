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

package com.bytedance.bitsail.connector.legacy.larksheet.api;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.legacy.larksheet.option.LarkSheetReaderOptions;

import com.google.common.collect.Sets;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Set;
import java.util.regex.Pattern;

@NoArgsConstructor
public class SheetConfig implements Serializable {

  private static final long serialVersionUID = 4138998289023711374L;

  /**
   * Parameters for request retry.
   */
  public static final int ATTEMPT_NUMBER = 3;
  public static final int WAIT_MILLISECONDS = 10;

  /**
   * Response status code for Lark api.<br/>
   * Ref: <a href="https://open.feishu.cn/document/ukTMukTMukTM/ugjM14COyUjL4ITN?lang=en-US">Server Error Codes</a>
   */
  public static final int REQUEST_SUCCESS = 0;
  public static final int REQUEST_TOO_FREQUENT = 99991400;
  public static final int REQUEST_FORBIDDEN = 91403;
  public static final int REQUEST_TOO_MANY = 1000004;
  public static final int INVALID_TENANT_ACCESS_TOKEN = 99991663;
  public static final int INVALID_APP_ACCESS_TOKEN = 99991664;
  public static final int INVALID_ACCESS_TOKEN = 99991671;

  /**
   * Error codes related to flow control.
   */
  public static final Set<Integer> FLOW_CONTROL_CODES = Sets.newHashSet(REQUEST_TOO_FREQUENT, REQUEST_TOO_MANY);

  /**
   * Error codes related to illegal token.
   */
  public static final Set<Integer> INVALID_ACCESS_TOKEN_CODES = Sets.newHashSet(INVALID_TENANT_ACCESS_TOKEN, INVALID_APP_ACCESS_TOKEN, INVALID_ACCESS_TOKEN);

  /**
   * Maximum reader parallelism.<br/>
   * Reason: Feishu open platform has flow control.
   */
  public static final int MAX_READ_PARALLELISM = 5;

  /**
   * SheetId key in user-defined sheet url.
   */
  public static final String SHEET_ID_URL_PARAM_NAME = "sheet";

  /**
   * Regular expression used to parse sheetToken.
   */
  public static final Pattern SHEET_TOKEN_PATTERN = Pattern.compile("/sheets/([0-9a-zA-Z]+)");

  /**
   * Token defined in job conf.
   */
  public static String PRE_DEFINED_SHEET_TOKEN;

  /**
   * APP_ID
   */
  public static String APP_ID;

  /**
   * APP_SECRET
   */
  public static String APP_SECRET;

  /**
   * Feishu open api host.
   */
  public static String OPEN_API_HOST;

  /**
   * Api for custom applications get app_access_token.<br/>
   * Ref: <a href="https://open.feishu.cn/document/ukTMukTMukTM/uADN14CM0UjLwQTN">app_access_token</a>
   */
  public static String APP_ACCESS_TOKEN_API;

  /**
   * Api for get sheet meta info.<br/>
   * Ref: <a href="https://open.feishu.cn/document/ukTMukTMukTM/uETMzUjLxEzM14SMxMTN">Get spreadsheet metadata</a>
   */
  public static String META_INFO_API_FORMAT;

  /**
   * Api for get a range of data from sheet.<br/>
   * Ref: <a href="https://open.feishu.cn/document/ukTMukTMukTM/ugTMzUjL4EzM14COxMTN">Read a single range</a>
   */
  public static String SINGLE_RANGE_API_FORMAT;

  public SheetConfig configure(BitSailConfiguration jobConf) {
    PRE_DEFINED_SHEET_TOKEN = jobConf.get(LarkSheetReaderOptions.SHEET_TOKEN);

    APP_ID = jobConf.get(LarkSheetReaderOptions.APP_ID);
    APP_SECRET = jobConf.get(LarkSheetReaderOptions.APP_SECRET);
    OPEN_API_HOST = jobConf.get(LarkSheetReaderOptions.OPEN_API_HOST);
    APP_ACCESS_TOKEN_API = jobConf.get(LarkSheetReaderOptions.APP_ACCESS_TOKEN_API);
    META_INFO_API_FORMAT = jobConf.get(LarkSheetReaderOptions.META_INFO_API_FORMAT);
    SINGLE_RANGE_API_FORMAT = jobConf.get(LarkSheetReaderOptions.SINGLE_RANGE_API_FORMAT);

    return this;
  }
}
