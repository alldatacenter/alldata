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

package com.bytedance.bitsail.connector.legacy.larksheet.util;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.util.FastJsonUtil;
import com.bytedance.bitsail.common.util.HttpManager;
import com.bytedance.bitsail.connector.legacy.larksheet.api.SheetConfig;
import com.bytedance.bitsail.connector.legacy.larksheet.api.TokenHolder;
import com.bytedance.bitsail.connector.legacy.larksheet.api.response.OpenApiBaseResponse;
import com.bytedance.bitsail.connector.legacy.larksheet.api.response.SheetMetaInfoResponse;
import com.bytedance.bitsail.connector.legacy.larksheet.api.response.SheetRangeResponse;
import com.bytedance.bitsail.connector.legacy.larksheet.error.LarkSheetFormatErrorCode;
import com.bytedance.bitsail.connector.legacy.larksheet.meta.SheetHeader;
import com.bytedance.bitsail.connector.legacy.larksheet.meta.SheetInfo;
import com.bytedance.bitsail.connector.legacy.larksheet.meta.SheetMeta;

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.collect.Maps;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

public class LarkSheetUtil {
  private static final Logger LOG = LoggerFactory.getLogger(LarkSheetUtil.class);

  private static final int CHARACTER_NUM = 26;

  private LarkSheetUtil() {
  }

  private static final Retryer<OpenApiBaseResponse> RETRYER = RetryerBuilder.<OpenApiBaseResponse>newBuilder()
      .retryIfRuntimeException()
      .retryIfResult(OpenApiBaseResponse::isFlowLimited)
      .retryIfResult(res -> res.isFailed() && !res.isForbidden())
      .withStopStrategy(StopStrategies.stopAfterAttempt(SheetConfig.ATTEMPT_NUMBER))
      .withWaitStrategy(WaitStrategies.fixedWait(SheetConfig.WAIT_MILLISECONDS, TimeUnit.MILLISECONDS))
      .withRetryListener(new SheetApiRetryListener())
      .build();

  /**
   * Retry listener.
   * Refresh token if it is expired.
   */
  private static class SheetApiRetryListener implements RetryListener {

    @Override
    public <V> void onRetry(Attempt<V> attempt) {
      if (attempt.hasException()) {
        LOG.warn("retry cause by: ", attempt.getExceptionCause());
        return;
      }

      V result = attempt.getResult();
      OpenApiBaseResponse response;
      if (result instanceof OpenApiBaseResponse) {
        response = (OpenApiBaseResponse) result;
        if (response.isSuccessful()) {
          LOG.info("Lark open api returned successful response!");
          return;
        } else if (response.isTokenExpired()) {
          // Refresh token if needed.
          if (TokenHolder.isGenerateTokenByApi()) {
            LOG.warn("app_access_token is expired, need to refresh it...");
            TokenHolder.refreshToken();
          } else {
            throw new BitSailException(LarkSheetFormatErrorCode.TOKEN_EXPIRED,
                "Token is expired, the job will be failed.");
          }
        } else if (response.isForbidden()) {
          // The app_id and app_secret are not authorized.
          LOG.warn("Request is forbidden, please make sure the sheet has been authorized with target app_id and app_secret");
          throw new BitSailException(LarkSheetFormatErrorCode.REQUEST_FORBIDDEN, "you can check your sheet share settings.");
        }
        LOG.info("Need to retry request, caused by token expired or request failed, response is: {}", response);
      } else {
        LOG.error("Invalid open api response, the job will be failed. response is: [{}]", result);
      }
    }
  }

  /**
   * Transform a integer into character sequence.<br/>
   * 1 --> A, 2 --> B, 26 --> Z, 27 --> AA.
   * @param number Number to transform.
   * @return A sequence of characters (A-Z).
   */
  public static String numberToSequence(int number) {
    StringBuilder sb = new StringBuilder();
    while (number > 0) {
      int c = number % CHARACTER_NUM;
      if (c == 0) {
        c = CHARACTER_NUM;
        number -= 1;
      }
      sb.insert(0, (char) ('A' + c - 1));
      number /= CHARACTER_NUM;
    }
    return sb.toString();
  }

  /**
   * Transform a character sequence into integer index.<br/>
   * A  --> 1
   * B  --> 2
   * Z  --> 26
   * AA  --> 27
   *
   * @param s Character sequence to transform.
   * @return Transformed index.
   */
  public static int sequenceToNumber(String s) {
    int ans = 0;
    for (int i = 0; i < s.length(); i++) {
      int num = s.charAt(i) - 'A' + 1;
      ans = ans * CHARACTER_NUM + num;
    }
    return ans;
  }

  /**
   * Get sheet info based on the sheet token and id parsed from sheet url.
   * @param sheetUrls A list of sheet urls.
   * @return A list of sheet info.
   */
  public static List<SheetInfo> resolveSheetUrls(List<String> sheetUrls) throws BitSailException {
    List<Tuple2<String, String>> sheetTokenAndIds = new ArrayList<>();
    for (String sheetUrl : sheetUrls) {
      String trimSheetUrl = sheetUrl.trim();
      Tuple2<String, String> tuple2 = new Tuple2<>();
      URIBuilder uriBuilder;
      try {
        uriBuilder = new URIBuilder(trimSheetUrl);
      } catch (URISyntaxException e) {
        throw new BitSailException(LarkSheetFormatErrorCode.INVALID_SHEET_URL,
            String.format("Invalid sheet url, cannot resolve spreadsheet token" +
                " please check sheet url : [%s]", trimSheetUrl));
      }

      String path = uriBuilder.getPath();

      Matcher matcher = SheetConfig.SHEET_TOKEN_PATTERN.matcher(path);
      if (matcher.find()) {
        tuple2.f0 = matcher.group(1);
      } else {
        throw new BitSailException(LarkSheetFormatErrorCode.INVALID_SHEET_URL,
            String.format("Invalid sheet url, cannot resolve spreadsheet token" +
                " please check sheet url : [%s]", trimSheetUrl));
      }

      List<NameValuePair> queryParams = uriBuilder.getQueryParams();
      tuple2.f1 = queryParams.stream()
          .filter(param -> param.getName().equalsIgnoreCase(SheetConfig.SHEET_ID_URL_PARAM_NAME))
          .map(NameValuePair::getValue)
          .findFirst()
          .orElse(null);
      sheetTokenAndIds.add(tuple2);
    }
    return transformSheetInfo(sheetTokenAndIds);
  }

  /**
   * Initialize sheetInfo for the given sheets.
   *
   * @param sheetTokenAndIds A list of sheet token and ids.
   */
  public static List<SheetInfo> transformSheetInfo(final List<Tuple2<String, String>> sheetTokenAndIds) {
    List<SheetInfo> sheetInfoList = new ArrayList<>();
    for (Tuple2<String, String> sheetTokenAndId : sheetTokenAndIds) {
      String sheetToken = sheetTokenAndId.f0;
      String sheetId = sheetTokenAndId.f1;
      SheetMetaInfoResponse sheetMetaInfoResponse = getMetaInfo(sheetToken);
      SheetMeta sheetMeta = sheetMetaInfoResponse.getSheet(sheetId);
      sheetInfoList.add(new SheetInfo(sheetMeta, sheetId, sheetToken));
    }
    return sheetInfoList;
  }

  /**
   * Get meta data of a sheet (mainly include row number and column number).<br/>
   * Ref: <a href="https://open.feishu.cn/document/ukTMukTMukTM/uETMzUjLxEzM14SMxMTN">Get spreadsheet metadata</a>
   *
   * @return Sheet's metadata.
   */
  public static SheetMetaInfoResponse getMetaInfo(final String sheetToken) {
    SheetMetaInfoResponse response;
    try {
      response = (SheetMetaInfoResponse) RETRYER.call(() -> {
        HttpManager.WrappedResponse wrappedResponse;
        String url = SheetConfig.OPEN_API_HOST + String.format(SheetConfig.META_INFO_API_FORMAT, sheetToken);
        wrappedResponse = HttpManager.sendGet(url, null, genAuthorizationHeader(TokenHolder.getToken()));
        return FastJsonUtil.parseObject(wrappedResponse.getResult(), SheetMetaInfoResponse.class);
      });
    } catch (ExecutionException | RetryException e) {
      throw new RuntimeException(String.format("Error while get meta info from lark open api," +
          " caused by: %s, the sheet token is [%s]", e.getCause().getMessage(), sheetToken), e.getCause());
    }
    if (response == null) {
      throw new RuntimeException(String.format("Fetch sheet meta info from lark open api failed!, response is null, please check sheet token : [%s]", sheetToken));
    }
    return response;
  }

  /**
   * @param sheetInfoList A list of sheet info.
   * @param readerColumns Columns defined in job configuration.
   * @return Sheet header.
   */
  public static SheetHeader getSheetHeader(List<SheetInfo> sheetInfoList, List<ColumnInfo> readerColumns) {

    List<SheetHeader> sheetHeaders = new ArrayList<>(sheetInfoList.size());
    SheetRangeResponse response;
    for (SheetInfo sheetInfo : sheetInfoList) {
      String sheetToken = sheetInfo.getSheetToken();
      SheetMeta sheetMeta = sheetInfo.getSheetMeta();
      try {
        response = (SheetRangeResponse) RETRYER.call(() -> {
          HttpManager.WrappedResponse wrappedResponse;
          String url = String.format(SheetConfig.OPEN_API_HOST + SheetConfig.SINGLE_RANGE_API_FORMAT,
              sheetToken,
              sheetMeta.getSheetId(),
              sheetMeta.getMaxHeaderRange());
          wrappedResponse = HttpManager.sendGet(url, null, genAuthorizationHeader(TokenHolder.getToken()));
          return FastJsonUtil.parseObject(wrappedResponse.getResult(), SheetRangeResponse.class);
        });
      } catch (ExecutionException | RetryException e) {
        throw new RuntimeException(String.format("Error while get sheet header from lark open api," +
            " caused by: %s", e.getCause().getMessage()), e.getCause());
      }
      LOG.info("Sheet header response is: {}", response);
      if (response == null || response.getValues() == null || response.getValues().isEmpty()) {
        throw new RuntimeException(String.format("Fetch sheet header from lark open api failed! response is invalid" +
            " please check sheet token : [%s] and sheet id : [%s]", sheetToken, sheetMeta.getSheetId()));
      }
      // Verify that the target schema is met
      sheetHeaders.add(new SheetHeader(response.getValues().get(0), readerColumns, sheetToken, sheetMeta.getSheetId()));
    }

    // return SheetHeader
    return sheetHeaders.get(0);
  }

  /**
   * Get a range of data from the sheet.
   *
   * @param sheetToken Token of target sheet.
   * @param sheetId Id of target sheet.
   * @param range Data range.
   * @param paramMap Params used to visit lark open api.
   * @return Data in the range of cells. Null means empty cell.
   */
  public static List<List<Object>> getRange(final String sheetToken,
                                            final String sheetId,
                                            final String range,
                                            final Map<String, String> paramMap) {
    SheetRangeResponse response;
    try {
      response = (SheetRangeResponse) RETRYER.call(() -> {
        HttpManager.WrappedResponse wrappedResponse;
        String url = String.format(SheetConfig.OPEN_API_HOST + SheetConfig.SINGLE_RANGE_API_FORMAT, sheetToken, sheetId,
            range);
        wrappedResponse = HttpManager.sendGet(url, paramMap, genAuthorizationHeader(TokenHolder.getToken()));
        return FastJsonUtil.parseObject(wrappedResponse.getResult(), SheetRangeResponse.class);
      });
    } catch (ExecutionException | RetryException e) {
      throw new RuntimeException(String.format("Error while get range[%s] from lark open api," +
          " caused by: %s, sheet_token:[%s], sheet_id:[%s]", range, e.getCause().getMessage(), sheetToken, sheetId), e.getCause());
    }

    if (response == null || response.getValues() == null) {
      throw new RuntimeException(String.format("Fetch range[%s] failed! response is invalid. " +
          "sheet_token:[%s], sheet_id:[%s]", range, sheetToken, sheetId));
    }
    LOG.info("Fetch range[{}] succeed! sheet_token:[{}], sheet_id:[{}]", range, sheetToken, sheetId);
    return response.getValues();
  }

  /**
   * @param accessToken Can be user_access_token or app_access_token.
   * @return A header with authorization.
   */
  private static Map<String, String> genAuthorizationHeader(String accessToken) {
    Map<String, String> header = Maps.newHashMap();
    header.put("Content-Type", ContentType.APPLICATION_JSON.toString());
    header.put("Authorization", "Bearer " + accessToken);
    return header;
  }
}
