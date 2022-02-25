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

package org.apache.ambari.server.security.authentication;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;

/**
 * AmbariAuthenticationEventHandler is an interface to be implemented by classes used to track Ambari
 * user authentication attempts.
 */
public interface AmbariAuthenticationEventHandler {
  /**
   * The event callback called when a successful authentication attempt has occurred.
   *
   * @param filter          the Authentication filer used for authentication
   * @param servletRequest  the request
   * @param servletResponse the response
   * @param result          the authentication result
   */
  void onSuccessfulAuthentication(AmbariAuthenticationFilter filter, HttpServletRequest servletRequest,
                                  HttpServletResponse servletResponse, Authentication result);

  /**
   * The event callback called when a failed authentication attempt has occurred.
   *
   * @param filter          the Authentication filer used for authentication
   * @param servletRequest  the request
   * @param servletResponse the response
   * @param cause           the exception used to declare the cause for the failure
   */
  void onUnsuccessfulAuthentication(AmbariAuthenticationFilter filter, HttpServletRequest servletRequest,
                                    HttpServletResponse servletResponse, AmbariAuthenticationException cause);

  /**
   * The event callback called just before an authentication attempt.
   *
   * @param filter          the Authentication filer used for authentication
   * @param servletRequest  the request
   * @param servletResponse the response
   */
  void beforeAttemptAuthentication(AmbariAuthenticationFilter filter, ServletRequest servletRequest,
                                   ServletResponse servletResponse);
}
