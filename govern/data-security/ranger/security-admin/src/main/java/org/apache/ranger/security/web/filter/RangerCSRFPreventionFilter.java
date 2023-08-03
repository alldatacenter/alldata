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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.security.web.filter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.ranger.common.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RangerCSRFPreventionFilter implements Filter {
	
	private static final Logger LOG = LoggerFactory.getLogger(RangerCSRFPreventionFilter.class);
		
	public static final String BROWSER_USER_AGENT_PARAM = "ranger.rest-csrf.browser-useragents-regex";
	public static final String BROWSER_USER_AGENTS_DEFAULT = "Mozilla,Opera,Chrome";
	public static final String CUSTOM_METHODS_TO_IGNORE_PARAM = "ranger.rest-csrf.methods-to-ignore";
	public static final String METHODS_TO_IGNORE_DEFAULT = "GET,OPTIONS,HEAD,TRACE";
	public static final String CUSTOM_HEADER_PARAM = "ranger.rest-csrf.custom-header";
	public static final String HEADER_DEFAULT = "X-XSRF-HEADER";
	public static final String HEADER_USER_AGENT = "User-Agent";
	public static final String CSRF_TOKEN = "_csrfToken";
	private static final boolean IS_CSRF_ENABLED = PropertiesUtil.getBooleanProperty("ranger.rest-csrf.enabled", true);

	private String  headerName = HEADER_DEFAULT;
	private Set<String> methodsToIgnore = null;
	private String[] browserUserAgents;
	
	public RangerCSRFPreventionFilter() {
		try {
			if (IS_CSRF_ENABLED){
				init(null);
			}
		} catch (Exception e) {
			LOG.error("Error while initializing Filter : "+e.getMessage());
		}
	}
	
	public void init(FilterConfig filterConfig) throws ServletException {
		String customHeader = PropertiesUtil.getProperty(CUSTOM_HEADER_PARAM, HEADER_DEFAULT);
	    if (customHeader != null) {
	      headerName = customHeader;
	    }
	
	    String customMethodsToIgnore = PropertiesUtil.getProperty(CUSTOM_METHODS_TO_IGNORE_PARAM, METHODS_TO_IGNORE_DEFAULT);
        if (customMethodsToIgnore != null) {
          parseMethodsToIgnore(customMethodsToIgnore);
        } else {
          parseMethodsToIgnore(METHODS_TO_IGNORE_DEFAULT);
        }
        String agents = PropertiesUtil.getProperty(BROWSER_USER_AGENT_PARAM, BROWSER_USER_AGENTS_DEFAULT);
        if (agents == null) {
          agents = BROWSER_USER_AGENTS_DEFAULT;
        }
        parseBrowserUserAgents(agents);
        LOG.info("Adding cross-site request forgery (CSRF) protection");
	}
	
	void parseMethodsToIgnore(String mti) {
        String[] methods = mti.split(",");
        methodsToIgnore = new HashSet<String>();
        Collections.addAll(methodsToIgnore, methods);
	}
	
	void parseBrowserUserAgents(String userAgents) {
		browserUserAgents = userAgents.split(",");
	}
	
	protected boolean isBrowser(String userAgent) {
		boolean isWeb = false;
		if (browserUserAgents != null && browserUserAgents.length > 0 && userAgent != null) {
			for (String ua : browserUserAgents) {
				if (userAgent.toLowerCase().startsWith(ua.toLowerCase())) {
					isWeb = true;
					break;
				}
			}
		}
		return isWeb;
	}
	
	public interface HttpInteraction {
		/**
		 * Returns the value of a header.
		 *
		 * @param header
		 *            name of header
		 * @return value of header
		 */
		String getHeader(String header);

		/**
		 * Returns the method.
		 *
		 * @return method
		 */
		String getMethod();

		/**
		 * Called by the filter after it decides that the request may proceed.
		 *
		 * @throws IOException
		 *             if there is an I/O error
		 * @throws ServletException
		 *             if the implementation relies on the servlet API and a
		 *             servlet API call has failed
		 */
		void proceed() throws IOException, ServletException;

		/**
		 * Called by the filter after it decides that the request is a potential
		 * CSRF attack and therefore must be rejected.
		 *
		 * @param code
		 *            status code to send
		 * @param message
		 *            response message
		 * @throws IOException
		 *             if there is an I/O error
		 */
		void sendError(int code, String message) throws IOException;
	}	
	
	public void handleHttpInteraction(HttpInteraction httpInteraction, boolean spnegoEnabled, boolean trustedProxyEnabled)
			throws IOException, ServletException {

		HttpSession session   = ((ServletFilterHttpInteraction) httpInteraction).getSession();
		String clientCsrfToken = httpInteraction.getHeader(headerName);
		String actualCsrfToken = StringUtils.EMPTY;

		if (session != null) {
			actualCsrfToken = (String) session.getAttribute(CSRF_TOKEN);
		} else {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Session is null");
			}
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("actualCsrfToken = " + actualCsrfToken + " clientCsrfToken = " + clientCsrfToken +
					"trustedProxy = " + trustedProxyEnabled + " for " + ((ServletFilterHttpInteraction) httpInteraction).httpRequest.getRequestURI());
		}
		/* When the request is from Knox, then spnegoEnabled and trustedProxyEnabled are true.
		 * In this case Knox inserts XSRF header with proper value for POST & PUT requests and hence proceed with authentication filter
		 */
		if ((spnegoEnabled && trustedProxyEnabled) || clientCsrfToken != null && clientCsrfToken.equals(actualCsrfToken)
				|| !isBrowser(httpInteraction.getHeader(HEADER_USER_AGENT))
				|| methodsToIgnore.contains(httpInteraction.getMethod())) {
			httpInteraction.proceed();
		}else {
			LOG.error("Missing header or invalid Header value for CSRF Vulnerability Protection");
			httpInteraction.sendError(HttpServletResponse.SC_BAD_REQUEST,"Missing header or invalid Header value for CSRF Vulnerability Protection");
		}
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (IS_CSRF_ENABLED) {
			final HttpServletRequest httpRequest = (HttpServletRequest)request;
		    final HttpServletResponse httpResponse = (HttpServletResponse)response;
		    Boolean spnegoEnabled = httpRequest.getAttribute("spnegoEnabled") != null && Boolean.valueOf(String.valueOf(httpRequest.getAttribute("spnegoEnabled")));
		    Boolean trustedProxyEnabled = httpRequest.getAttribute("trustedProxyEnabled") != null && Boolean.valueOf(String.valueOf(httpRequest.getAttribute("trustedProxyEnabled")));
		    handleHttpInteraction(new ServletFilterHttpInteraction(httpRequest, httpResponse, chain), spnegoEnabled, trustedProxyEnabled);
		}else{
			chain.doFilter(request, response);
		}
	}

	public void destroy() {
	}
	
	private static final class ServletFilterHttpInteraction implements
			HttpInteraction {

		private final FilterChain chain;
		private final HttpServletRequest httpRequest;
		private final HttpServletResponse httpResponse;

		/**
		 * Creates a new ServletFilterHttpInteraction.
		 *
		 * @param httpRequest
		 *            request to process
		 * @param httpResponse
		 *            response to process
		 * @param chain
		 *            filter chain to forward to if HTTP interaction is allowed
		 */
		public ServletFilterHttpInteraction(HttpServletRequest httpRequest,
				HttpServletResponse httpResponse, FilterChain chain) {
			this.httpRequest = httpRequest;
			this.httpResponse = httpResponse;
			this.chain = chain;
		}

		@Override
		public String getHeader(String header) {
			return httpRequest.getHeader(header);
		}

		@Override
		public String getMethod() {
			return httpRequest.getMethod();
		}

		@Override
		public void proceed() throws IOException, ServletException {
			chain.doFilter(httpRequest, httpResponse);
		}

		public HttpSession getSession() {
			return httpRequest.getSession();
		}

		@Override
		public void sendError(int code, String message) throws IOException {
			httpResponse.sendError(code, message);
		}
	}
}
