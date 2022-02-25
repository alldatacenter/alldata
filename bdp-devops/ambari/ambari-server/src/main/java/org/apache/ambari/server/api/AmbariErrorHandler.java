/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.api;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ambari.server.security.authentication.jwt.JwtAuthenticationProperties;
import org.apache.ambari.server.security.authentication.jwt.JwtAuthenticationPropertiesProvider;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpConnection;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Custom error handler for Jetty to return response as JSON instead of stub http page
 */
public class AmbariErrorHandler extends ErrorHandler {
  private static final Logger LOG = LoggerFactory.getLogger(AmbariErrorHandler.class);

  private final Gson gson;

  private JwtAuthenticationPropertiesProvider jwtAuthenticationPropertiesProvider;

  @Inject
  public AmbariErrorHandler(@Named("prettyGson") Gson prettyGson, JwtAuthenticationPropertiesProvider jwtAuthenticationPropertiesProvider) {
    this.gson = prettyGson;
    this.jwtAuthenticationPropertiesProvider = jwtAuthenticationPropertiesProvider;
  }

  @Override
  public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
    HttpChannel connection = HttpConnection.getCurrentConnection().getHttpChannel();
    connection.getRequest().setHandled(true);

    response.setContentType(MimeTypes.Type.TEXT_PLAIN.asString());

    Map<String, Object> errorMap = new LinkedHashMap<>();
    int code = connection.getResponse().getStatus();
    errorMap.put("status", code);
    String message = connection.getResponse().getReason();
    if (message == null) {
      message = HttpStatus.getMessage(code);
    }
    errorMap.put("message", message);

    if ((code == HttpServletResponse.SC_FORBIDDEN) || (code == HttpServletResponse.SC_UNAUTHORIZED)) {
      //if SSO is configured we should provide info about it in case of access error
      JwtAuthenticationProperties jwtProperties = jwtAuthenticationPropertiesProvider.get();
      if ((jwtProperties != null) && jwtProperties.isEnabledForAmbari()) {
        String providerUrl = jwtProperties.getAuthenticationProviderUrl();
        String originalUrl = jwtProperties.getOriginalUrlQueryParam();

        if (StringUtils.isEmpty(providerUrl)) {
          LOG.warn("The SSO provider URL is not available, forwarding to the SSO provider is not possible");
        } else if (StringUtils.isEmpty(originalUrl)) {
          LOG.warn("The original URL parameter name is not available, forwarding to the SSO provider is not possible");
        } else {
          errorMap.put("jwtProviderUrl", String.format("%s?%s=", providerUrl, originalUrl));
        }
      }
    }

    gson.toJson(errorMap, response.getWriter());
  }
}
