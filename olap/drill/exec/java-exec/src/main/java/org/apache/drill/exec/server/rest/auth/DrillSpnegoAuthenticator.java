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
package org.apache.drill.exec.server.rest.auth;


import org.apache.drill.exec.server.rest.WebServerConstants;
import org.apache.parquet.Strings;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.security.authentication.DeferredAuthentication;
import org.eclipse.jetty.security.authentication.SessionAuthentication;
import org.eclipse.jetty.security.authentication.SpnegoAuthenticator;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.UserIdentity;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Custom SpnegoAuthenticator for Drill
 */
public class DrillSpnegoAuthenticator extends SpnegoAuthenticator {

  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DrillSpnegoAuthenticator.class);

  public DrillSpnegoAuthenticator(String authMethod) {
    super(authMethod);
  }

  /**
   * Updated logic as compared to default implementation in
   * {@link SpnegoAuthenticator#validateRequest(ServletRequest, ServletResponse, boolean)} to handle below cases:
   * 1) Perform SPNEGO authentication only when spnegoLogin resource is requested. This helps to avoid authentication
   *    for each and every resource which the JETTY provided authenticator does.
   * 2) Helps to redirect to the target URL after authentication is done successfully.
   * 3) Clear-Up in memory session information once LogOut is triggered such that any future request also triggers SPNEGO
   *    authentication.
   * @param request
   * @param response
   * @param mandatoryAuth
   * @return
   * @throws ServerAuthException
   */
  @Override
  public Authentication validateRequest(ServletRequest request, ServletResponse response, boolean mandatoryAuth)
      throws ServerAuthException {

    final HttpServletRequest req = (HttpServletRequest) request;
    final HttpSession session = req.getSession(true);
    final Authentication authentication = (Authentication) session.getAttribute(SessionAuthentication.__J_AUTHENTICATED);
    final String uri = req.getRequestURI();

    // If the Request URI is for /spnegoLogin then perform login
    final boolean mandatory = mandatoryAuth || uri.equals(WebServerConstants.SPENGO_LOGIN_RESOURCE_PATH);

    // For logout the attribute from the session that holds UserIdentity will be removed when session is getting
    // invalidated
    if (authentication != null) {
      if (uri.equals(WebServerConstants.LOGOUT_RESOURCE_PATH)) {
        return null;
      }

      // Already logged in so just return the session attribute.
      return authentication;
    }

    // Try to authenticate an unauthenticated session.
    return authenticateSession(request, response, mandatory);
  }

  /**
   * Method to authenticate a user session using the SPNEGO token passed in AUTHORIZATION header of request.
   * @param request
   * @param response
   * @param mandatory
   * @return
   * @throws ServerAuthException
   */
  private Authentication authenticateSession(ServletRequest request, ServletResponse response, boolean mandatory)
      throws ServerAuthException {

    final HttpServletRequest req = (HttpServletRequest) request;
    final HttpServletResponse res = (HttpServletResponse) response;
    final HttpSession session = req.getSession(true);

    // Defer the authentication if not mandatory.
    if (!mandatory) {
      return new DeferredAuthentication(this);
    }

    // Authentication is mandatory, get the Authorization header
    final String header = req.getHeader(HttpHeader.AUTHORIZATION.asString());

    // Authorization header is null, so send the 401 error code to client along with negotiate header
    if (header == null) {
      try {
        if (DeferredAuthentication.isDeferred(res)) {
          return Authentication.UNAUTHENTICATED;
        } else {
          res.setHeader(HttpHeader.WWW_AUTHENTICATE.asString(), HttpHeader.NEGOTIATE.asString());
          res.sendError(401);
          logger.debug("DrillSpnegoAuthenticator: Sending challenge to client {}", req.getRemoteAddr());
          return Authentication.SEND_CONTINUE;
        }
      } catch (IOException e) {
        logger.error("DrillSpnegoAuthenticator: Failed while sending challenge to client {}", req.getRemoteAddr(), e);
        throw new ServerAuthException(e);
      }
    }

    // Valid Authorization header received. Get the SPNEGO token sent by client and try to authenticate
    logger.debug("DrillSpnegoAuthenticator: Received NEGOTIATE Response back from client {}", req.getRemoteAddr());
    final String negotiateString = HttpHeader.NEGOTIATE.asString();

    if (header.startsWith(negotiateString)) {
      final String spnegoToken = header.substring(negotiateString.length() + 1);
      final UserIdentity user = this.login(null, spnegoToken, request);

      //redirect the request to the desired page after successful login
      if (user != null) {
        String newUri = (String) session.getAttribute("org.eclipse.jetty.security.form_URI");
        if (Strings.isNullOrEmpty(newUri)) {
          newUri = req.getContextPath();
          if (Strings.isNullOrEmpty(newUri)) {
            newUri = WebServerConstants.WEBSERVER_ROOT_PATH;
          }
        }
        response.setContentLength(0);
        Request baseRequest = Request.getBaseRequest(req);
        int redirectCode =
            baseRequest.getHttpVersion().getVersion() < HttpVersion.HTTP_1_1.getVersion() ? 302 : 303;
        try {
          baseRequest.getResponse().sendRedirect(redirectCode, res.encodeRedirectURL(newUri));
        } catch (IOException e) {
          logger.error("DrillSpnegoAuthenticator: Failed while using the redirect URL {} from client {}", newUri,
              req.getRemoteAddr(), e);
          throw new ServerAuthException(e);
        }

        logger.debug("DrillSpnegoAuthenticator: Successfully authenticated this client session: {}",
            user.getUserPrincipal().getName());
        return new UserAuthentication(this.getAuthMethod(), user);
      }
    }

    logger.debug("DrillSpnegoAuthenticator: Authentication failed for client session: {}", req.getRemoteAddr());
    return Authentication.UNAUTHENTICATED;

  }

  public UserIdentity login(String username, Object password, ServletRequest request) {
    final UserIdentity user = super.login(username, password, request);

    if (user != null) {
      final HttpSession session = ((HttpServletRequest) request).getSession(true);
      final Authentication cached = new SessionAuthentication(this.getAuthMethod(), user, password);
      session.setAttribute(SessionAuthentication.__J_AUTHENTICATED, cached);
    }

    return user;
  }
}