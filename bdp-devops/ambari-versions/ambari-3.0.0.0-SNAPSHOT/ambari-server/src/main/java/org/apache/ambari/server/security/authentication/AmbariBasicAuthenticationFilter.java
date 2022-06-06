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

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ambari.server.security.AmbariEntryPoint;
import org.apache.ambari.server.utils.RequestUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.stereotype.Component;

/**
 * AmbariBasicAuthenticationFilter extends a {@link BasicAuthenticationFilter} to allow for auditing
 * of authentication attempts
 * <p>
 * This authentication filter is expected to be used withing an {@link AmbariDelegatingAuthenticationFilter}.
 *
 * @see AmbariDelegatingAuthenticationFilter
 */
@Component
@Order(3)
public class AmbariBasicAuthenticationFilter extends BasicAuthenticationFilter implements AmbariAuthenticationFilter {
  private static final Logger LOG = LoggerFactory.getLogger(AmbariBasicAuthenticationFilter.class);

  private final AmbariAuthenticationEventHandler eventHandler;

  /**
   * Constructor.
   *
   * @param authenticationManager the Spring authentication manager
   * @param ambariEntryPoint      the Spring entry point
   * @param eventHandler          the authentication event handler
   */
  public AmbariBasicAuthenticationFilter(AuthenticationManager authenticationManager,
                                         AmbariEntryPoint ambariEntryPoint,
                                         AmbariAuthenticationEventHandler eventHandler) {
    super(authenticationManager, ambariEntryPoint);

    if (eventHandler == null) {
      throw new IllegalArgumentException("The AmbariAuthenticationEventHandler must not be null");
    }

    this.eventHandler = eventHandler;
  }

  /**
   * Tests to see if this {@link AmbariBasicAuthenticationFilter} should be applied in the authentication
   * filter chain.
   * <p>
   * <code>true</code> will be returned if the HTTP request contains the basic authentication header;
   * otherwise <code>false</code> will be returned.
   * <p>
   * The basic authentication header is named "Authorization" and the value begins with the string
   * "Basic" following by the encoded username and password information.
   * <p>
   * For example:
   * <code>
   * Authorization: Basic YWRtaW46YWRtaW4=
   * </code>
   *
   * @param httpServletRequest the HttpServletRequest the HTTP service request
   * @return <code>true</code> if the HTTP request contains the basic authentication header; otherwise <code>false</code>
   */
  @Override
  public boolean shouldApply(HttpServletRequest httpServletRequest) {

    if (LOG.isDebugEnabled()) {
      RequestUtils.logRequestHeadersAndQueryParams(httpServletRequest, LOG);
    }

    String header = httpServletRequest.getHeader("Authorization");
    if ((header != null) && header.startsWith("Basic ")) {
      // If doAs is sent as a query parameter, ignore the basic auth header.
      // This logic is here to help deal with a potential issue when Knox is the trusted proxy and it
      // forwards the original request's Authorization header (for Basic Auth) when Kerberos authentication
      // is required.
      String doAsQueryParameterValue = RequestUtils.getQueryStringParameterValue(httpServletRequest, "doAs");
      if (StringUtils.isEmpty(doAsQueryParameterValue)) {
        return true;
      } else {
        LOG.warn("The 'doAs' query parameter was provided; however, the BasicAuth header is found. " +
            "Ignoring the BasicAuth header hoping to negotiate Kerberos authentication.");
        return false;
      }
    } else {
      return false;
    }
  }

  @Override
  public boolean shouldIncrementFailureCount() {
    return true;
  }

  /**
   * Checks whether the authentication information is filled. If it is not, then a login failed audit event is logged
   *
   * @param httpServletRequest  the request
   * @param httpServletResponse the response
   * @param filterChain         the Spring filter chain
   * @throws IOException
   * @throws ServletException
   */
  @Override
  public void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws IOException, ServletException {
    if (eventHandler != null) {
      eventHandler.beforeAttemptAuthentication(this, httpServletRequest, httpServletResponse);
    }

    super.doFilterInternal(httpServletRequest, httpServletResponse, filterChain);
  }

  /**
   * If the authentication was successful, then an audit event is logged about the success
   *
   * @param servletRequest  the request
   * @param servletResponse the response
   * @param authResult      the Authentication result
   * @throws IOException
   */
  @Override
  protected void onSuccessfulAuthentication(HttpServletRequest servletRequest,
                                            HttpServletResponse servletResponse,
                                            Authentication authResult) throws IOException {

    if (eventHandler != null) {
      eventHandler.onSuccessfulAuthentication(this, servletRequest, servletResponse, authResult);
    }
  }

  /**
   * In the case of invalid username or password, the authentication fails and it is logged
   *
   * @param servletRequest  the request
   * @param servletResponse the response
   * @param authException   the exception, if any, causing the unsuccessful authentication attempt
   * @throws IOException
   */
  @Override
  protected void onUnsuccessfulAuthentication(HttpServletRequest servletRequest,
                                              HttpServletResponse servletResponse,
                                              AuthenticationException authException) throws IOException {
    if (eventHandler != null) {
      AmbariAuthenticationException cause;
      if (authException instanceof AmbariAuthenticationException) {
        cause = (AmbariAuthenticationException) authException;
      } else {
        String header = servletRequest.getHeader("Authorization");
        String username = null;
        try {
          username = getUsernameFromAuth(header, getCredentialsCharset(servletRequest));
        } catch (Exception e) {
          LOG.warn("Error occurred during decoding authorization header.", e);
        }

        cause = new AmbariAuthenticationException(username, authException.getMessage(), false, authException);
      }

      eventHandler.onUnsuccessfulAuthentication(this, servletRequest, servletResponse, cause);
    }
  }

  /**
   * Helper function to decode Authorization header
   *
   * @param authenticationValue the authentication value to parse
   * @param charSet             the character set of the authentication value
   * @return the username parsed from the authentication header value
   * @throws IOException
   */
  private String getUsernameFromAuth(String authenticationValue, String charSet) throws IOException {
    byte[] base64Token = authenticationValue.substring(6).getBytes("UTF-8");

    byte[] decoded;
    try {
      decoded = Base64.decode(base64Token);
    } catch (IllegalArgumentException ex) {
      throw new BadCredentialsException("Failed to decode basic authentication token");
    }

    String token = new String(decoded, charSet);
    int delimiter = token.indexOf(":");
    if (delimiter == -1) {
      throw new BadCredentialsException("Invalid basic authentication token");
    } else {
      return token.substring(0, delimiter);
    }
  }
}
