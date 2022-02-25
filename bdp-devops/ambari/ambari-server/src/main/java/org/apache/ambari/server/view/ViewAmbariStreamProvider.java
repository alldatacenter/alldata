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

package org.apache.ambari.server.view;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariSessionManager;
import org.apache.ambari.server.controller.internal.URLStreamProvider;
import org.apache.ambari.server.proxy.ProxyService;
import org.apache.ambari.view.AmbariHttpException;
import org.apache.ambari.view.AmbariStreamProvider;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider of an input stream for a request to the Ambari server.
 */
public class ViewAmbariStreamProvider implements AmbariStreamProvider {
  /**
   * Internal stream provider.
   */
  private final URLStreamProvider streamProvider;

  /**
   * The Ambari session manager.
   */
  private final AmbariSessionManager ambariSessionManager;

  /**
   * The Ambari management controller.
   */
  private final AmbariManagementController controller;

  private static final Logger LOG = LoggerFactory.getLogger(ViewAmbariStreamProvider.class);


  // ----- Constructor -----------------------------------------------------

  /**
   * Construct a view Ambari stream provider.
   *
   * @param streamProvider        the underlying stream provider
   * @param ambariSessionManager  the Ambari session manager
   * @param controller         the Ambari configuration
   *
   * @throws IllegalStateException if the Ambari stream provider can not be created
   */
  protected ViewAmbariStreamProvider(URLStreamProvider streamProvider, AmbariSessionManager ambariSessionManager,
                                     AmbariManagementController controller) {
    this.streamProvider       = streamProvider;
    this.ambariSessionManager = ambariSessionManager;
    this.controller           = controller;
  }


  // ----- AmbariStreamProvider -----------------------------------------------

  @Override
  public InputStream readFrom(String path, String requestMethod, String body, Map<String, String> headers
                              ) throws IOException, AmbariHttpException {
    return getInputStream(path, requestMethod, headers, body == null ? null : body.getBytes());
  }

  @Override
  public InputStream readFrom(String path, String requestMethod, InputStream body, Map<String, String> headers
                              ) throws IOException, AmbariHttpException {

    return getInputStream(path, requestMethod, headers, body == null ? null : IOUtils.toByteArray(body));
  }


  // ----- helper methods ----------------------------------------------------

  private InputStream getInputStream(String path, String requestMethod, Map<String, String> headers
                                     , byte[] body) throws IOException, AmbariHttpException {
    // add the Ambari session cookie to the given headers
    String sessionId = ambariSessionManager.getCurrentSessionId();
    if (sessionId != null) {

      String ambariSessionCookie = ambariSessionManager.getSessionCookie() + "=" + sessionId;

      if (headers == null || headers.isEmpty()) {
        headers = Collections.singletonMap(URLStreamProvider.COOKIE, ambariSessionCookie);
      } else {
        headers = new HashMap<>(headers);

        String cookies = headers.get(URLStreamProvider.COOKIE);

        headers.put(URLStreamProvider.COOKIE, URLStreamProvider.appendCookie(cookies, ambariSessionCookie));
      }
    }

    // adapt the headers for the internal URLStreamProvider signature
    Map<String, List<String>> headerMap = new HashMap<>();
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      headerMap.put(entry.getKey(), Collections.singletonList(entry.getValue()));
    }

    return getInputStream(streamProvider.processURL(controller.getAmbariServerURI(path.startsWith("/") ? path : "/" + path),
        requestMethod, body, headerMap));
  }

  private InputStream getInputStream(HttpURLConnection connection) throws IOException, AmbariHttpException {
    int responseCode = connection.getResponseCode();
    if (responseCode >= ProxyService.HTTP_ERROR_RANGE_START) {
      String message = connection.getResponseMessage();
      if (connection.getErrorStream() != null) {
        message = IOUtils.toString(connection.getErrorStream());
      }
      LOG.error("Got error response for url {}. Response code:{}. {}", connection.getURL(), responseCode, message);
      throw new AmbariHttpException(message, responseCode);
    }
    return connection.getInputStream();
  }
}

