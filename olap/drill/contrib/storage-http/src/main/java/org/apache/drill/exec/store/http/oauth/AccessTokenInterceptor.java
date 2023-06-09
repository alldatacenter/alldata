/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.drill.exec.store.http.oauth;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * This class intercepts HTTP requests without the requisite OAuth credentials and
 * adds them to the request.
 */
public class AccessTokenInterceptor implements Interceptor {

  private static final Logger logger = LoggerFactory.getLogger(AccessTokenInterceptor.class);

  private final AccessTokenRepository accessTokenRepository;

  public AccessTokenInterceptor(AccessTokenRepository accessTokenRepository) {
    this.accessTokenRepository = accessTokenRepository;
  }

  @Override
  public Response intercept(Chain chain) throws IOException {
    logger.debug("Intercepting call {}", chain.toString());
    String accessToken = accessTokenRepository.getAccessToken();
    Request request = newRequestWithAccessToken(chain.request(), accessToken);
    Response response = chain.proceed(request);

    if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
      logger.debug("Unauthorized request.");
      response.close();
      synchronized (this) {
        final String newAccessToken = accessTokenRepository.getAccessToken();
        // Access token is refreshed in another thread
        if (!accessToken.equals(newAccessToken)) {
          return chain.proceed(newRequestWithAccessToken(request, newAccessToken));
        }

        // Need to refresh access token
        final String updatedAccessToken;
        updatedAccessToken = accessTokenRepository.refreshAccessToken();
        // Retry the request
        return chain.proceed(newRequestWithAccessToken(request, updatedAccessToken));
      }
    }
    return response;
  }

  private Request newRequestWithAccessToken(Request request, String accessToken) {
    logger.debug("Interceptor making new request with access token: {}", request.url());
    String tokenType = accessTokenRepository.getTokenType();
    if (StringUtils.isNotEmpty(tokenType)) {
      accessToken = tokenType + " " + accessToken;
    }

    if (accessTokenRepository.getOAuthConfig().isAccessTokenInHeader()) {
      HttpUrl rawUrl = HttpUrl.parse(request.url().toString());
      rawUrl.newBuilder().addQueryParameter("access_token", accessToken);
      return request.newBuilder().url(rawUrl.url()).build();
    } else {
      return request.newBuilder().header("Authorization", accessToken).build();
    }
  }
}
