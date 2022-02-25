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
package org.apache.ambari.server.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.security.authentication.kerberos.AmbariKerberosAuthenticationProperties;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class AmbariEntryPoint implements AuthenticationEntryPoint {

  /**
   * A Boolean value declaring whether Kerberos authentication has been enabled (<code>true</code>)
   * or not (<code>false</code>).
   * <p>
   * This value determines the behavior this entry point when authentication fails.
   */
  private final boolean kerberosAuthenticationEnabled;

  public AmbariEntryPoint(Configuration configuration) {
    AmbariKerberosAuthenticationProperties kerberosAuthenticationProperties = (configuration == null)
        ? null
        : configuration.getKerberosAuthenticationProperties();

    kerberosAuthenticationEnabled = (kerberosAuthenticationProperties != null) && kerberosAuthenticationProperties.isKerberosAuthenticationEnabled();
  }

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
    /* *****************************************************************************************
     * If Kerberos authentication is enabled (authentication.kerberos.enabled = true), respond such
     * that the client is challenged to Negotiate and reissue the request with a Kerberos token.
     * This response is an HTTP 401 status with the "WWW-Authenticate: Negotiate" header.
     *
     * If Kerberos authentication is not enabled, return an HTTP 403 status.
     * ****************************************************************************************** */
    if (kerberosAuthenticationEnabled) {
      response.setHeader("WWW-Authenticate", "Negotiate");
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication requested");
    } else {
      response.sendError(HttpServletResponse.SC_FORBIDDEN, authException.getMessage());
    }
  }
}
