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

package org.apache.atlas.web.filters;

import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasException;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AtlasCSRFPreventionFilter implements Filter {
	private static final Logger LOG = LoggerFactory.getLogger(AtlasCSRFPreventionFilter.class);
	private static Configuration configuration;
	
	static {
	    try {
            configuration  = ApplicationProperties.get();
            LOG.info("Configuration obtained :: "+configuration);
        } catch (AtlasException e) {
            LOG.error(e.getMessage(), e);
        }
	}
	
	public static final boolean isCSRF_ENABLED = configuration.getBoolean("atlas.rest-csrf.enabled", true);
	public static final String BROWSER_USER_AGENT_PARAM = "atlas.rest-csrf.browser-useragents-regex";
	public static final String BROWSER_USER_AGENTS_DEFAULT = "^Mozilla.*,^Opera.*,^Chrome";
	public static final String CUSTOM_METHODS_TO_IGNORE_PARAM = "atlas.rest-csrf.methods-to-ignore";
	public static final String METHODS_TO_IGNORE_DEFAULT = "GET,OPTIONS,HEAD,TRACE";
	public static final String CUSTOM_HEADER_PARAM = "atlas.rest-csrf.custom-header";
	public static final String HEADER_DEFAULT = "X-XSRF-HEADER";
	public static final String HEADER_USER_AGENT = "User-Agent";
	public static final String CSRF_TOKEN = "_csrfToken";


	private String  headerName = HEADER_DEFAULT;
	private Set<String> methodsToIgnore = null;
	private Set<Pattern> browserUserAgents;

	public AtlasCSRFPreventionFilter() {
		try {
			if (isCSRF_ENABLED){
				init(null);
			}
		} catch (Exception e) {
			LOG.error("Error while initializing Filter ", e);
		}
	}
	
	public void init(FilterConfig filterConfig) throws ServletException {
		String customHeader = configuration.getString(CUSTOM_HEADER_PARAM, HEADER_DEFAULT);
	    if (customHeader != null) {
	      headerName = customHeader;
	    }
	    
	    String customMethodsToIgnore = configuration.getString(CUSTOM_METHODS_TO_IGNORE_PARAM, METHODS_TO_IGNORE_DEFAULT);
        if (customMethodsToIgnore != null) {
          parseMethodsToIgnore(customMethodsToIgnore);
        } else {
          parseMethodsToIgnore(METHODS_TO_IGNORE_DEFAULT);
        }
        String agents = configuration.getString(BROWSER_USER_AGENT_PARAM, BROWSER_USER_AGENTS_DEFAULT);
        if (agents == null) {
          agents = BROWSER_USER_AGENTS_DEFAULT;
        }
        parseBrowserUserAgents(agents);
        LOG.info("Adding cross-site request forgery (CSRF) protection");
	}
	
	void parseMethodsToIgnore(String mti) {
        String[] methods = mti.split(",");
        methodsToIgnore = new HashSet<>();
		Collections.addAll(methodsToIgnore, methods);
	}
	
	void parseBrowserUserAgents(String userAgents) {
		String[] agentsArray = userAgents.split(",");
		browserUserAgents = new HashSet<>();
		for (String patternString : agentsArray) {
			browserUserAgents.add(Pattern.compile(patternString));
		}
	}
	
	protected boolean isBrowser(String userAgent) {
		if (userAgent == null) {
			return false;
		}
		if (browserUserAgents != null){
			for (Pattern pattern : browserUserAgents) {
				Matcher matcher = pattern.matcher(userAgent);
				if (matcher.matches()) {
					return true;
				}
			}
		}
		return false;
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

	public void handleHttpInteraction(HttpInteraction httpInteraction) throws IOException, ServletException {
		HttpSession session   = ((ServletFilterHttpInteraction) httpInteraction).getSession();
		String      csrfToken = StringUtils.EMPTY;

		if (session != null) {
			csrfToken = (String) session.getAttribute(CSRF_TOKEN);
		} else {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Session is null");
			}
		}

		String clientCsrfToken = httpInteraction.getHeader(headerName);

		if (!isBrowser(httpInteraction.getHeader(HEADER_USER_AGENT)) || methodsToIgnore.contains(httpInteraction.getMethod())
				|| (clientCsrfToken != null && clientCsrfToken.equals(csrfToken))) {
			httpInteraction.proceed();
		} else {
			httpInteraction.sendError(HttpServletResponse.SC_BAD_REQUEST,"Missing header or invalid Header value for CSRF Vulnerability Protection");
		}
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final HttpServletResponse httpResponse = (HttpServletResponse) response;
        AtlasResponseRequestWrapper responseWrapper = new AtlasResponseRequestWrapper(httpResponse);
		HeadersUtil.setHeaderMapAttributes(responseWrapper, HeadersUtil.X_FRAME_OPTIONS_KEY);

        if (isCSRF_ENABLED){
            handleHttpInteraction(new ServletFilterHttpInteraction(httpRequest, httpResponse, chain));
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
			JSONObject json = new JSONObject();
            json.put("msgDesc", message);
            httpResponse.setContentType("application/json");
            httpResponse.setStatus(code);
            httpResponse.setCharacterEncoding("UTF-8");
            httpResponse.getWriter().write(json.toJSONString());
		}
	}
}
