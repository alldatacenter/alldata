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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * AbstractSecurityHeaderFilter is an abstract class used to help add security-related headers to
 * HTTP responses.
 * <p/>
 * This class is to be implemented to set the values for the following headers:
 * <ol>
 * <li>Strict-Transport-Security</li>
 * <li>X-Frame-Options</li>
 * <li>X-XSS-Protection</li>
 * </ol>
 * <p/>
 * If the value for a particular header item is empty (or null) that header will not be added to the
 * set of response headers.
 */
public abstract class AbstractSecurityHeaderFilter implements Filter {
  protected final static String STRICT_TRANSPORT_HEADER = "Strict-Transport-Security";
  protected final static String X_FRAME_OPTIONS_HEADER = "X-Frame-Options";
  protected final static String X_XSS_PROTECTION_HEADER = "X-XSS-Protection";
  protected final static String X_CONTENT_TYPE_HEADER = "X-Content-Type-Options";
  protected final static String CACHE_CONTROL_HEADER = "Cache-Control";
  protected final static String PRAGMA_HEADER = "Pragma";

  /**
   * The logger.
   */
  private final static Logger LOG = LoggerFactory.getLogger(AbstractSecurityHeaderFilter.class);

  /**
   * Signals whether subsequent filters are allowed to override security headers
   */
  protected final static String DENY_HEADER_OVERRIDES_FLAG = "deny.header.overrides.flag";
  /**
   * The Configuration object used to determine how Ambari is configured
   */
  @Inject
  private Configuration configuration;

  /**
   * Indicates whether Ambari is configured for SSL (true) or not (false).  By default true is assumed
   * since preparing for more security will not hurt and is better than not assuming SSL is enabled
   * when it is.
   */
  private boolean sslEnabled = true;
  /**
   * The value for the Strict-Transport-Security HTTP response header.
   */
  private String strictTransportSecurity = Configuration.HTTP_STRICT_TRANSPORT_HEADER_VALUE.getDefaultValue();
  /**
   * The value for the X-Frame-Options HTTP response header.
   */
  private String xFrameOptionsHeader = Configuration.HTTP_X_FRAME_OPTIONS_HEADER_VALUE.getDefaultValue();
  /**
   * The value for the X-XSS-Protection HTTP response header.
   */
  private String xXSSProtectionHeader = Configuration.HTTP_X_XSS_PROTECTION_HEADER_VALUE.getDefaultValue();
  /**
   * The value for the Content-Type HTTP response header.
   */
  private String xContentTypeHeader = Configuration.HTTP_X_CONTENT_TYPE_HEADER_VALUE.getDefaultValue();
  /**
   * The value for the Cache-control HTTP response header.
   */
  private String cacheControlHeader = Configuration.HTTP_CACHE_CONTROL_HEADER_VALUE.getDefaultValue();
  /**
   * The value for the Pragma HTTP response header.
   */
  private String pragmaHeader = Configuration.HTTP_PRAGMA_HEADER_VALUE.getDefaultValue();

  /**
   * The value for the Charset HTTP response header.
   */
  private String charset = Configuration.HTTP_CHARSET.getDefaultValue();


  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    LOG.debug("Initializing {}", this.getClass().getName());

    if (configuration == null) {
      LOG.warn("The Ambari configuration object is not available, all default options will be assumed.");
    } else {
      processConfig(configuration);
    }
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

    if (checkPrerequisites(servletRequest)) {
      doFilterInternal(servletRequest, servletResponse);
    }

    filterChain.doFilter(servletRequest, servletResponse);
  }

  /**
   * Checks whether the security headers need to be set, if so signals it in a request parameter.
   *
   * @param servletRequest the incoming request
   * @return true if headers need to be set, false otherwise
   */
  protected abstract boolean checkPrerequisites(ServletRequest servletRequest);

  @Override
  public void destroy() {
    LOG.debug("Destroying {}", this.getClass().getName());
  }

  protected abstract void processConfig(Configuration configuration);


  protected void setSslEnabled(boolean sslEnabled) {
    this.sslEnabled = sslEnabled;
  }

  protected void setStrictTransportSecurity(String strictTransportSecurity) {
    this.strictTransportSecurity = strictTransportSecurity;
  }

  protected void setxFrameOptionsHeader(String xFrameOptionsHeader) {
    this.xFrameOptionsHeader = xFrameOptionsHeader;
  }

  protected void setxXSSProtectionHeader(String xXSSProtectionHeader) {
    this.xXSSProtectionHeader = xXSSProtectionHeader;
  }

  protected void setXContentTypeHeader(String xContentTypeHeader) {
    this.xContentTypeHeader = xContentTypeHeader;
  }

  protected void setCacheControlHeader(String cacheControlHeader) {
    this.cacheControlHeader = cacheControlHeader;
  }

  protected void setPragmaHeader(String pragmaHeader) {
    this.pragmaHeader = pragmaHeader;
  }

  protected void setCharset(String charset) {
    this.charset = charset;
  }

  private void doFilterInternal(ServletRequest servletRequest, ServletResponse servletResponse) {
    if (servletResponse instanceof HttpServletResponse) {
      HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
      // Conditionally set the Strict-Transport-Security HTTP response header if SSL is enabled and
      // a value is supplied
      if (sslEnabled && !StringUtils.isEmpty(strictTransportSecurity)) {
        httpServletResponse.setHeader(STRICT_TRANSPORT_HEADER, strictTransportSecurity);
      }

      if (!StringUtils.isEmpty(xFrameOptionsHeader)) {
        // perform filter specific logic related to the X-Frame-Options HTTP response header
        httpServletResponse.setHeader(X_FRAME_OPTIONS_HEADER, xFrameOptionsHeader);
      }

      // Conditionally set the X-XSS-Protection HTTP response header if a value is supplied
      if (!StringUtils.isEmpty(xXSSProtectionHeader)) {
        httpServletResponse.setHeader(X_XSS_PROTECTION_HEADER, xXSSProtectionHeader);
      }

      // Conditionally set the X-Content-Type HTTP response header if a value is supplied
      if (!StringUtils.isEmpty(xContentTypeHeader)) {
        httpServletResponse.setHeader(X_CONTENT_TYPE_HEADER, xContentTypeHeader);
      }

      // Conditionally set the X-Cache-Control HTTP response header if a value is supplied
      if (!StringUtils.isEmpty(cacheControlHeader)) {
        httpServletResponse.setHeader(CACHE_CONTROL_HEADER, cacheControlHeader);
      }

      // Conditionally set the X-Pragma HTTP response header if a value is supplied
      if (!StringUtils.isEmpty(pragmaHeader)) {
        httpServletResponse.setHeader(PRAGMA_HEADER, pragmaHeader);
      }

      // Conditionally set the Charset HTTP response header if a value is supplied
      if (!StringUtils.isEmpty(charset)) {
        httpServletResponse.setCharacterEncoding(charset);
      }
    }
  }

}
