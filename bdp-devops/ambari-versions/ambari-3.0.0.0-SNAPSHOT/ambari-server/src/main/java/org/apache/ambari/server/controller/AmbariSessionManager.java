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

package org.apache.ambari.server.controller;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.eclipse.jetty.server.session.SessionHandler;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Class to expose current session information.
 */
public class AmbariSessionManager {

  /**
   * Session manager.
   */
  @Inject
  SessionHandler sessionHandler;


  // ----- AmbariSessionManager ----------------------------------------------

  /**
   * Get the session id associated with the current thread-bound request.
   *
   * @return the current session id; null if no request is associated with the current thread
   */
  public String getCurrentSessionId() {

    HttpSession session = getHttpSession();

    return session == null ? null : session.getId();
  }

  /**
   * The session cookie id (i.e. AMBARISESSIONID).
   *
   * @return the session cookie
   */
  public String getSessionCookie() {
    return sessionHandler.getSessionCookieConfig().getName();
  }

  /**
   * Set an attribute value on the current session.
   *
   * @param name   the attribute name
   * @param value  the attribute value
   */
  public void setAttribute(String name, Object value) {
    HttpSession session = getHttpSession();
    if (session != null) {
      session.setAttribute(name, value);
    }
  }

  /**
   * Get an attribute value from the current session.
   *
   * @param name  the attribute name
   *
   * @return the attribute value
   */
  public Object getAttribute(String name) {
    HttpSession session = getHttpSession();
    if (session != null) {
      return session.getAttribute(name);
    }
    return null;
  }

  /**
   * Remove the attribute identified by the given name from the current session.
   *
   * @param name  the attribute name
   */
  public void removeAttribute(String name) {
    HttpSession session = getHttpSession();
    if (session != null) {
      session.removeAttribute(name);
    }
  }


  // ----- helper methods ----------------------------------------------------

  /**
   * Get the current session.
   *
   * @return the current session
   */
  protected HttpSession getHttpSession() {

    RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

    if (requestAttributes != null && requestAttributes instanceof ServletRequestAttributes) {

      HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

      return request == null ? null : request.getSession(true);
    }
    return null;
  }
}
