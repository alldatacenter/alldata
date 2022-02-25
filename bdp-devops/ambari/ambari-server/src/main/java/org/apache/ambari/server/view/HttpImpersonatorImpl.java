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


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.controller.internal.URLStreamProvider;
import org.apache.ambari.server.proxy.ProxyService;
import org.apache.ambari.view.HttpImpersonator;
import org.apache.ambari.view.ImpersonatorSetting;
import org.apache.ambari.view.ViewContext;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Class for Ambari to impersonate users over HTTP request.
 * This is handy for Views like Jobs that needs to query ATS via HTTP Get requests and
 * impersonate the currently logged on user.
 * Or a file browser view that needs to use WebHDFS with the credentials of the current user.
 */
public class HttpImpersonatorImpl implements HttpImpersonator {
  private ViewContext context;
  private final URLStreamProvider urlStreamProvider;

  private static final Logger LOG = LoggerFactory.getLogger(HttpImpersonatorImpl.class);

  public HttpImpersonatorImpl(ViewContext c) {
    this.context = c;

    ComponentSSLConfiguration configuration = ComponentSSLConfiguration.instance();
    this.urlStreamProvider = new URLStreamProvider(ProxyService.URL_CONNECT_TIMEOUT,
        ProxyService.URL_READ_TIMEOUT, configuration.getTruststorePath(),
        configuration.getTruststorePassword(), configuration.getTruststoreType());
  }

  public HttpImpersonatorImpl(ViewContext c, URLStreamProvider urlStreamProvider) {
    this.context = c;
    this.urlStreamProvider = urlStreamProvider;
  }

  public ViewContext getContext() {
    return this.context;
  }

  public String getUsername() {
    return getContext().getUsername();
  }

  /**
   * @param conn HTTP connection that will be modified and returned
   * @param type HTTP Request type: GET, PUT, POST, DELETE, etc.
   * @return HTTP Connection object with the "doAs" query param set to the currently logged on user.
   */
  @Override
  public HttpURLConnection doAs(HttpURLConnection conn, String type)  {
    String username = getUsername();
    return doAs(conn, type, username, ImpersonatorSettingImpl.DEFAULT_DO_AS_PARAM);
  }

  /**
   * @param conn HTTP connection that will be modified and returned
   * @param type HTTP Request type: GET, PUT, POST, DELETE, etc.
   * @param username Username to impersonate
   * @param doAsParamName Query param, typically "doAs"
   * @return HTTP Connection object with the doAs query param set to the provider username.
   */
  @Override
  public HttpURLConnection doAs(HttpURLConnection conn, String type, String username, String doAsParamName) {
    String url = conn.getURL().toString();
    if (url.toLowerCase().contains(doAsParamName.toLowerCase())) {
      throw new IllegalArgumentException("URL cannot contain \"" + doAsParamName + "\" parameter");
    }

    try {
      conn.setRequestMethod(type);
    } catch (IOException e) {
      return null;
    }

    conn.setRequestProperty(doAsParamName, username);
    return conn;
  }

  /**
   * Returns the result of the HTTP request by setting the "doAs" impersonation for the query param and username
   * in @param impersonatorSetting.
   * @param url URL to request
   * @param requestType HTTP Request type: GET, PUT, POST, DELETE, etc.
   * @param impersonatorSetting Setting class with default values for username and doAs param name.
   *                           To use different values, call the setters of the object.
   * @return Return a response as a String
   */
  @Override
  public String requestURL(String url, String requestType, final ImpersonatorSetting impersonatorSetting) {
    String result = "";
    BufferedReader rd;
    String line;

    if (url.toLowerCase().contains(impersonatorSetting.getDoAsParamName().toLowerCase())) {
      throw new IllegalArgumentException("URL cannot contain \"" + impersonatorSetting.getDoAsParamName() + "\" parameter");
    }

    try {
      String username = impersonatorSetting.getUsername();
      if (username != null) {
        URIBuilder builder = new URIBuilder(url);
        builder.addParameter(impersonatorSetting.getDoAsParamName(), username);
        url = builder.build().toString();
      }

      HttpURLConnection connection = urlStreamProvider.processURL(url, requestType, (String) null, null);

      int responseCode = connection.getResponseCode();
      InputStream resultInputStream;
      if (responseCode >= ProxyService.HTTP_ERROR_RANGE_START) {
        resultInputStream = connection.getErrorStream();
      } else {
        resultInputStream = connection.getInputStream();
      }

      rd = new BufferedReader(new InputStreamReader(resultInputStream));

      line = rd.readLine();
      while (line != null) {
        result += line;
        line = rd.readLine();
      }
      rd.close();
    } catch (Exception e) {
      LOG.error("Exception caught processing impersonator request.", e);
    }
    return result;
  }
}
