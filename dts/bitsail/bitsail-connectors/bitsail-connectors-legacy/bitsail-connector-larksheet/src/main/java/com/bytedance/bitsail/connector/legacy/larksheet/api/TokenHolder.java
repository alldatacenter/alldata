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

import com.bytedance.bitsail.common.util.FastJsonUtil;
import com.bytedance.bitsail.common.util.HttpManager;
import com.bytedance.bitsail.common.util.Preconditions;
import com.bytedance.bitsail.connector.legacy.larksheet.api.response.AppAccessTokenResponse;
import com.bytedance.bitsail.connector.legacy.larksheet.api.response.OpenApiBaseResponse;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.collect.Maps;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class TokenHolder {
  private static final Logger LOG = LoggerFactory.getLogger(TokenHolder.class);

  private TokenHolder() {
  }

  private static volatile String appAccessToken;

  @Getter
  private static volatile boolean generateTokenByApi = false;

  private static final Retryer<OpenApiBaseResponse> RETRYER = RetryerBuilder.<OpenApiBaseResponse>newBuilder()
      .retryIfException()
      .retryIfResult(OpenApiBaseResponse::isFlowLimited)
      .withStopStrategy(StopStrategies.stopAfterAttempt(SheetConfig.ATTEMPT_NUMBER))
      .withWaitStrategy(WaitStrategies.fixedWait(SheetConfig.WAIT_MILLISECONDS, TimeUnit.MILLISECONDS))
      .build();

  /**
   * Init app_access_token (singleton mode).<br/>
   * 1. If user defines sheet_token in job conf, then use it.<br/>
   * 2. If sheet_token is not defined, then use lark open api to get token.<br/>
   *
   * @return app_access_token
   */
  public static String init(String preDefinedToken) {
    if (StringUtils.isNotEmpty(preDefinedToken)) {
      appAccessToken = preDefinedToken;
      LOG.info("Use pre-defined token from job configuration.");
    }

    if (StringUtils.isBlank(appAccessToken)) {
      synchronized (TokenHolder.class) {
        if (StringUtils.isBlank(appAccessToken)) {
          refreshToken();
        }
      }
    }
    LOG.info("TokenHolder has been initialized successfully!");
    return appAccessToken;
  }

  /**
   * Make sure init() has been executed before getToken().
   *
   * @return app_access_token
   */
  public static String getToken() {
    Preconditions.checkArgument(StringUtils.isNotBlank(appAccessToken),
        "app_access_token is empty, please make sure TokenHolder.init() has been invoked before");
    return appAccessToken;
  }

  /**
   * Generate token by API.<br/>
   * Ref: <a href="https://open.feishu.cn/document/ukTMukTMukTM/uADN14CM0UjLwQTN?lang=en-US">Custom applications get app_access_token</a>
   */
  public static void refreshToken() {
    LOG.info("Start to generate or refresh app_access_token...");
    Map<String, Object> body = Maps.newHashMap();
    body.put("app_id", SheetConfig.APP_ID);
    body.put("app_secret", SheetConfig.APP_SECRET);

    AppAccessTokenResponse response;

    try {
      response = (AppAccessTokenResponse) RETRYER.call(() -> {
        HttpManager.WrappedResponse wrappedResponse;
        String url = SheetConfig.OPEN_API_HOST + SheetConfig.APP_ACCESS_TOKEN_API;
        wrappedResponse = HttpManager.sendPost(url, null, body, ContentType.APPLICATION_JSON);
        AppAccessTokenResponse tmpResponse = FastJsonUtil.parseObject(wrappedResponse.getResult(),
            AppAccessTokenResponse.class);
        if (tmpResponse.isFlowLimited()) {
          LOG.info("trigger flow control when generate app_access_token, maybe retry later...");
        }
        return tmpResponse;
      });
    } catch (ExecutionException | RetryException e) {
      throw new RuntimeException(String.format("Error while get app_access_token from lark open api, caused by: %s",
          e.getCause().getMessage()), e.getCause());
    }

    if (response == null || response.isFailed() || StringUtils.isBlank(response.getAppAccessToken())) {
      throw new RuntimeException(String.format("generate app_access_token from lark open api failed." +
          " please check your app_id and app_secret, response is :%s", response));
    }
    appAccessToken = response.getAppAccessToken();
    generateTokenByApi = true;
    LOG.info("Successfully generate or refresh app_access_token!");
  }

}

