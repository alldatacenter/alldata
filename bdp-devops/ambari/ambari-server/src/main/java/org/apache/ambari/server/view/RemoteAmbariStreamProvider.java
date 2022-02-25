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

import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.controller.internal.URLStreamProvider;
import org.apache.ambari.server.proxy.ProxyService;
import org.apache.ambari.view.AmbariHttpException;
import org.apache.ambari.view.AmbariStreamProvider;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;

/**
 * Provider of an input stream for a request to the Remote Ambari server.
 */
public class RemoteAmbariStreamProvider implements AmbariStreamProvider {

  private String baseUrl;

  private String username;

  private String password;

  private URLStreamProvider urlStreamProvider;

  public RemoteAmbariStreamProvider(String baseUrl, String username, String password, int connectTimeout, int readTimeout) {
    this.baseUrl = baseUrl;
    this.username = username;
    this.password = password;

    ComponentSSLConfiguration sslConfiguration = ComponentSSLConfiguration.instance();
    this.urlStreamProvider =
      new URLStreamProvider(
        connectTimeout,
        readTimeout,
        sslConfiguration.getTruststorePath(),
        sslConfiguration.getTruststorePassword(),
        sslConfiguration.getTruststoreType());
  }

  @Override
  public InputStream readFrom(String path, String requestMethod, String body, Map<String, String> headers) throws IOException, AmbariHttpException {
    return getInputStream(urlStreamProvider.processURL(getUrl(path), requestMethod, body, addHeaders(headers)));
  }

  @Override
  public InputStream readFrom(String path, String requestMethod, InputStream body, Map<String, String> headers) throws IOException, AmbariHttpException {
    return getInputStream(urlStreamProvider.processURL(getUrl(path), requestMethod, body, addHeaders(headers)));
  }

  private InputStream getInputStream(HttpURLConnection connection) throws IOException, AmbariHttpException {
    int responseCode = connection.getResponseCode();
    if(responseCode >= ProxyService.HTTP_ERROR_RANGE_START){
      throw new AmbariHttpException(IOUtils.toString(connection.getErrorStream()),responseCode);
    }
    return connection.getInputStream();
  }

  private String getUrl(String path){
    String basePath = baseUrl;
    return path.startsWith("/") ? basePath + path : basePath + "/" + path;
  }

  private void addRequestedByHeaders(HashMap<String, String> newHeaders) {
    newHeaders.put("X-Requested-By", "AMBARI");
  }

  private Map<String,List<String>> modifyHeaders(Map<String,String> headers){
    Map<String, List<String>> headerMap = new HashMap<>();
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      headerMap.put(entry.getKey(), Collections.singletonList(entry.getValue()));
    }
    return headerMap;
  }

  private Map<String, List<String>> addHeaders(Map<String, String> customHeaders) {
    HashMap<String, String> newHeaders = new HashMap<>();
    if (customHeaders != null)
      newHeaders.putAll(customHeaders);

    addBasicAuthHeaders(newHeaders);
    addRequestedByHeaders(newHeaders);
    return modifyHeaders(newHeaders);
  }

  private void addBasicAuthHeaders(HashMap<String, String> headers) {
    String authString = username + ":" + password;
    byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
    String authStringEnc = new String(authEncBytes);

    headers.put("Authorization", "Basic " + authStringEnc);
  }

}
