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

import okhttp3.Authenticator;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccessTokenAuthenticator implements Authenticator {
  private final static Logger logger = LoggerFactory.getLogger(AccessTokenAuthenticator.class);

  private final AccessTokenRepository accessTokenRepository;

  public AccessTokenAuthenticator(AccessTokenRepository accessTokenRepository) {
    this.accessTokenRepository = accessTokenRepository;
  }

  @Override
  public Request authenticate(Route route, Response response) {
    logger.debug("Authenticating {}", response.headers());
    final String accessToken = accessTokenRepository.getAccessToken();
    if (!isRequestWithAccessToken(response) || accessToken == null) {
      return null;
    }
    synchronized (this) {
      final String newAccessToken = accessTokenRepository.getAccessToken();
      // Access token is refreshed in another thread.
      if (!accessToken.equals(newAccessToken)) {
        return newRequestWithAccessToken(response.request(), newAccessToken);
      }

      // Need to refresh an access token
      final String updatedAccessToken;
      updatedAccessToken = accessTokenRepository.refreshAccessToken();
      return newRequestWithAccessToken(response.request(), updatedAccessToken);
    }
  }

  private boolean isRequestWithAccessToken(Response response) {
    String header = response.request().header("Authorization");
    return header != null && header.startsWith("Bearer");
  }

  private Request newRequestWithAccessToken(Request request, String accessToken) {
    logger.debug("Creating a new request with access token.");
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
