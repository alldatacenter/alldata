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
package org.apache.drill.exec.server.rest;

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.work.WorkManager;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.ssl.SSLContexts;

import javax.net.ssl.SSLContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.stream.Collectors;

public class WebUtils {

  private static CloseableHttpAsyncClient httpClient;

  /**
   * Retrieves the CSRF protection token from the HTTP request.
   *
   * @param request HTTP request that contains a session that stores a CSRF protection token.
   *                If there is no session, that means that authentication is disabled.
   * @return CSRF protection token, or an empty string if there is no session present.
   */
  public static String getCsrfTokenFromHttpRequest(HttpServletRequest request) {
    // No need to create a session if not present (i.e. if a user is logged in)
    HttpSession session = request.getSession(false);
    return session == null ? "" : (String) session.getAttribute(WebServerConstants.CSRF_TOKEN);
  }

  /**
   * Generates a BASE64 encoded CSRF token from randomly generated 256-bit buffer
   * according to the <a href="https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html">OWASP CSRF Prevention Cheat Sheet</a>
   *
   * @return randomly generated CSRF token.
   */
  public static String generateCsrfToken() {
    byte[] buffer = new byte[32];
    new SecureRandom().nextBytes(buffer);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
  }

  /**
   * Build an URL of a remote Drillbit endpoint.
   *
   * @param work {@link WorkManager} instance needed to retrieve the Drillbit address.
   * @param request {@link HttpServletRequest} instance needed to set the URL schema.
   * @param hostname hostname of the Drillbit.
   * @param path relative path to the endpoint.
   * @return remote Drillbit endpoint URL.
   * @throws RuntimeException if there is no Drillbit at the given hostname.
   */
  static URL getDrillbitURL(WorkManager work, HttpServletRequest request, String hostname, String path) {
    int drillbitPort = work.getContext().getAvailableBits().stream()
        .filter(db -> db.getAddress().equals(hostname))
        .findAny()
        .map(DrillbitEndpoint::getHttpPort)
        .orElseThrow(() -> new RuntimeException("No such drillbit: " + hostname));
    try {
      return new URL(request.getScheme(), hostname, drillbitPort, path);
    } catch (MalformedURLException e) {
      throw new IllegalStateException("Should never occur", e);
    }
  }

  /**
   * Send an HTTP request and return response body as String.
   *
   * @param httpRequest {@link org.apache.http.client.methods.HttpGet} or {@link org.apache.http.client.methods.HttpPost} instance representing an HTTP request.
   * @param drillConfig {@link DrillConfig} instance needed to setup HTTP Client.
   * @return String response body.
   * @throws Exception if unable to create HTTP client or in case of HTTP timeout.
   */
  static String doHTTPRequest(HttpRequestBase httpRequest, DrillConfig drillConfig) throws Exception {
    CloseableHttpAsyncClient httpClient = getHttpClient(drillConfig);
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(
        httpClient.execute(httpRequest, null)
            .get()
            .getEntity()
            .getContent()))) {
      return reader.lines().collect(Collectors.joining("\n"));
    }
  }

  /**
   * Get singleton instance of an HTTP Client.
   *
   * @param drillConfig {@link DrillConfig} instance needed to retrieve Drill SSL parameters.
   * @return HTTP Client instance.
   * @throws Exception if unable to create an HTTP Client.
   */
  private static CloseableHttpAsyncClient getHttpClient(DrillConfig drillConfig) throws Exception {
    CloseableHttpAsyncClient localHttpClient = httpClient;
    if (localHttpClient == null) {
      synchronized (WebUtils.class) {
        localHttpClient = httpClient;
        if (httpClient == null) {
          localHttpClient = createHttpClient(drillConfig);
          localHttpClient.start();
          httpClient = localHttpClient;
        }
      }
    }
    return localHttpClient;
  }

  private static CloseableHttpAsyncClient createHttpClient(DrillConfig drillConfig) throws Exception {
    HttpAsyncClientBuilder clientBuilder = HttpAsyncClients.custom();
    if (drillConfig.getBoolean(ExecConstants.HTTP_ENABLE_SSL)) {
      SSLContext sslContext = SSLContexts.custom()
          .loadTrustMaterial(new TrustSelfSignedStrategy())
          .build();
      SSLIOSessionStrategy sessionStrategy = new SSLIOSessionStrategy(
          sslContext,
          new String[]{drillConfig.getString(ExecConstants.SSL_PROTOCOL)},
          null,
          SSLIOSessionStrategy.getDefaultHostnameVerifier()
      );
      clientBuilder.setSSLStrategy(sessionStrategy);
    }
    RequestConfig requestConfig = RequestConfig.custom()
        .setConnectTimeout(drillConfig.getInt(ExecConstants.HTTP_CLIENT_TIMEOUT))
        .build();
    clientBuilder.setDefaultRequestConfig(requestConfig);
    return clientBuilder.build();
  }
}
