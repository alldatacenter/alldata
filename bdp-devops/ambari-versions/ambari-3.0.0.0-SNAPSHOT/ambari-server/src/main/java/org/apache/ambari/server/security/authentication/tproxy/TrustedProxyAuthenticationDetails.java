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

package org.apache.ambari.server.security.authentication.tproxy;

import javax.servlet.http.HttpServletRequest;

import org.apache.ambari.server.utils.RequestUtils;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

/**
 * TrustedProxyAuthenticationDetails is a {@link WebAuthenticationDetails} implementation that
 * contains information from an {@link HttpServletRequest} that can be used for handling trusted
 * proxy authentication requests.
 * <p>
 * If the request is not from a trusted proxy, the internal valiues will be null.
 */
public class TrustedProxyAuthenticationDetails extends WebAuthenticationDetails {
  private final String doAs;
  private final String xForwardedContext;
  private final String xForwardedProto;
  private final String xForwardedHost;
  private final String xForwardedFor;
  private final String xForwardedPort;
  private final String xForwardedServer;

  /**
   * Constructor.
   * <p>
   * Given an {@link HttpServletRequest}, attempts to retrieve the following data:
   * <ul>
   * <li>The proxied user from the <code>doAs</code> query parameter</li>
   * <li>
   * The following <code>X-Forwarded-*</code> values from the request header information
   * <ul>
   * <li>X-Forwarded-Context</li>
   * <li>X-Forwarded-Proto</li>
   * <li>X-Forwarded-Host</li>
   * <li>X-Forwarded-For</li>
   * <li>X-Forwarded-Port</li>
   * <li>X-Forwarded-Server</li>
   * </ul>
   * </li>
   * </ul>
   *
   * @param context the {@link HttpServletRequest} to query
   */
  TrustedProxyAuthenticationDetails(HttpServletRequest context) {
    super(context);

    this.doAs = RequestUtils.getQueryStringParameterValue(context, "doAs");
    this.xForwardedContext = context.getHeader("X-Forwarded-Context");
    this.xForwardedProto = context.getHeader("X-Forwarded-Proto");
    this.xForwardedHost = context.getHeader("X-Forwarded-Host");
    this.xForwardedFor = context.getHeader("X-Forwarded-For");
    this.xForwardedPort = context.getHeader("X-Forwarded-Port");
    this.xForwardedServer = context.getHeader("X-Forwarded-Server");
  }

  public String getDoAs() {
    return doAs;
  }

  public String getXForwardedContext() {
    return xForwardedContext;
  }

  public String getXForwardedProto() {
    return xForwardedProto;
  }

  public String getXForwardedHost() {
    return xForwardedHost;
  }

  public String getXForwardedFor() {
    return xForwardedFor;
  }

  public String getXForwardedPort() {
    return xForwardedPort;
  }

  public String getXForwardedServer() {
    return xForwardedServer;
  }
}
