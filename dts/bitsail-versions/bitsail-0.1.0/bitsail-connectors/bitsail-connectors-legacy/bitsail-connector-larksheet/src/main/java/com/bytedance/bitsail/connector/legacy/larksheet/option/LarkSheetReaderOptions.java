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

package com.bytedance.bitsail.connector.legacy.larksheet.option;

import com.bytedance.bitsail.common.option.ConfigOption;
import com.bytedance.bitsail.common.option.ReaderOptions;

import com.alibaba.fastjson.TypeReference;

import java.util.List;
import java.util.Map;

import static com.bytedance.bitsail.common.option.ConfigOptions.key;
import static com.bytedance.bitsail.common.option.ReaderOptions.READER_PREFIX;

/**
 * Options for LarkSheet reader.
 */
public interface LarkSheetReaderOptions extends ReaderOptions.BaseReaderOptions {

  /**
   * LarkSheet urls list separated by comma.<br/>
   * Single url format is: https://{xxx}.feishu.cn/sheets/{sheetToken}?sheet={sheetId}
   */
  ConfigOption<String> SHEET_URL =
      key(READER_PREFIX + "sheet_urls")
          .noDefaultValue(String.class);

  /**
   * LarkSheet token.
   */
  ConfigOption<String> SHEET_TOKEN =
      key(READER_PREFIX + "sheet_token")
          .noDefaultValue(String.class);

  /**
   * Customized parameters which are appended after request urls.
   */
  ConfigOption<Map<String, String>> LARK_PROPERTIES =
      key(READER_PREFIX + "lark")
          .onlyReference(new TypeReference<Map<String, String>>() {});

  /**
   * Number of lines extracted once.
   * <br>
   * Api ref: <a href="https://open.feishu.cn/document/ukTMukTMukTM/ugTMzUjL4EzM14COxMTN?lang=en-US">range api</a>
   */
  ConfigOption<Integer> BATCH_SIZE =
      key(READER_PREFIX + "batch_size")
          .defaultValue(2000);

  /**
   * Number of lines that are skipped.
   */
  ConfigOption<List<Integer>> SKIP_NUMS =
      key(READER_PREFIX + "skip_nums")
      .onlyReference(new TypeReference<List<Integer>>(){});

  /**
   * app_id for generating token.
   */
  ConfigOption<String> APP_ID =
      key(READER_PREFIX + "lark_app_id")
          .noDefaultValue(String.class);

  /**
   * app_secret for generating token.
   */
  ConfigOption<String> APP_SECRET =
      key(READER_PREFIX + "lark_app_secret")
          .noDefaultValue(String.class);

  /**
   * Host of lark open api.
   */
  ConfigOption<String> OPEN_API_HOST =
      key(READER_PREFIX + "lark_open_api_host")
          .defaultValue("https://open.feishu.cn");

  /**
   * API for app_access_token.
   */
  ConfigOption<String> APP_ACCESS_TOKEN_API =
      key(READER_PREFIX + "lark_app_access_token_api")
          .defaultValue("/open-apis/auth/v3/app_access_token/internal");

  /**
   * API for get metadata.
   */
  ConfigOption<String> META_INFO_API_FORMAT =
      key(READER_PREFIX + "lark_meta_info_api_format")
          .defaultValue("/open-apis/sheet/v2/spreadsheets/%s/metainfo");

  /**
   * API for get a range of data from sheet.
   */
  ConfigOption<String> SINGLE_RANGE_API_FORMAT =
      key(READER_PREFIX + "lark_sheet_single_range_api_format")
          .defaultValue("/open-apis/sheet/v2/spreadsheets/%s/values/%s!%s");
}
