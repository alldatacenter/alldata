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
package org.apache.ambari.server.api;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.ambari.server.security.authorization.AuthorizationHelper;

/**
 * This filter overrides usernames found in request url.
 */
public class UserNameOverrideFilter implements Filter {

  // Regex for extracting user name component from the user related api request Uris
  private final static Pattern USER_NAME_IN_URI_REGEXP = Pattern.compile("(?<pre>.*/users/)(?<username>[^/]+)(?<post>(/.*)?)");

  /**
   * Called by the web container to indicate to a filter that it is
   * being placed into service.
   *
   * <p>The servlet container calls the init
   * method exactly once after instantiating the filter. The init
   * method must complete successfully before the filter is asked to do any
   * filtering work.
   *
   * <p>The web container cannot place the filter into service if the init
   * method either
   * <ol>
   * <li>Throws a ServletException
   * <li>Does not return within a time period defined by the web container
   * </ol>
   *
   * @param filterConfig
   */
  @Override
  public void init(FilterConfig filterConfig) throws ServletException {

  }

  /**
   * The <code>doFilter</code> method of the Filter is called by the
   * container each time a request/response pair is passed through the
   * chain due to a client request for a resource at the end of the chain.
   * The FilterChain passed in to this method allows the Filter to pass
   * on the request and response to the next entity in the chain.
   *
   * Verify if this a user related request by checking that the Uri of the request contains
   * username and resolves the username to actual ambari user name if username
   * is a login alias.
   *
   * @param request
   * @param response
   * @param chain
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    if (request instanceof HttpServletRequest) {
      final HttpServletRequest httpServletRequest = (HttpServletRequest) request;
      Matcher userNameMatcher = getUserNameMatcher(httpServletRequest.getRequestURI());

      if (userNameMatcher.find()) {
        String userNameFromUri = URLDecoder.decode(userNameMatcher.group("username"), "UTF-8");
        final String userName = AuthorizationHelper.resolveLoginAliasToUserName(userNameFromUri);

        if (!userNameFromUri.equals(userName)) {
          final String requestUriOverride = String.format("%s%s%s", userNameMatcher.group("pre"), userName, userNameMatcher.group("post"));

          request = new HttpServletRequestWrapper(httpServletRequest) {
            @Override
            public String getRequestURI() {
              return requestUriOverride;
            }
          };

        }
      }
    }

    chain.doFilter(request, response);
  }

  /**
   * Returns a {@link Matcher} created from {@link #USER_NAME_IN_URI_REGEXP} for the
   * provided requestUri.
   * @param requestUri the Uri the Matcher is created for.
   * @return the matcher
   */
  protected Matcher getUserNameMatcher(String requestUri) {
    return USER_NAME_IN_URI_REGEXP.matcher(requestUri);
  }

  /**
   * Called by the web container to indicate to a filter that it is being
   * taken out of service.
   *
   * <p>This method is only called once all threads within the filter's
   * doFilter method have exited or after a timeout period has passed.
   * After the web container calls this method, it will not call the
   * doFilter method again on this instance of the filter.
   *
   * <p>This method gives the filter an opportunity to clean up any
   * resources that are being held (for example, memory, file handles,
   * threads) and make sure that any persistent state is synchronized
   * with the filter's current state in memory.
   */
  @Override
  public void destroy() {

  }
}
