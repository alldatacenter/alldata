/**
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
package org.apache.ambari.server.controller;

import org.apache.ambari.server.configuration.Configuration;
import org.eclipse.jetty.server.session.SessionHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SessionHandlerConfigurer {

  @Inject
  Configuration configuration;

  /**
   * Performs basic configuration of session handler with static values and values from
   * configuration file.
   *
   * @param sessionHandler session handler
   */
  protected void configureSessionHandler(SessionHandler sessionHandler) {
    sessionHandler.getSessionCookieConfig().setPath("/");

    // use AMBARISESSIONID instead of JSESSIONID to avoid conflicts with
    // other services (like HDFS) that run on the same context but a different
    // port
    sessionHandler.setSessionCookie("AMBARISESSIONID");

    sessionHandler.getSessionCookieConfig().setHttpOnly(true);
    if (configuration.getApiSSLAuthentication()) {
      sessionHandler.getSessionCookieConfig().setSecure(true);
    }

    configureMaxInactiveInterval(sessionHandler);
  }

  protected void configureMaxInactiveInterval(SessionHandler sessionHandler) {
    // each request that does not use AMBARISESSIONID will create a new
    // HashedSession in Jetty; these MUST be reaped after inactivity in order
    // to prevent a memory leak

    int sessionInactivityTimeout = configuration.getHttpSessionInactiveTimeout();
    sessionHandler.setMaxInactiveInterval(sessionInactivityTimeout);
  }
}
