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

 /**
 *
 */
package org.apache.ranger.security.web.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.ranger.biz.SessionMgr;
import org.apache.ranger.biz.XUserMgr;
import org.apache.ranger.common.GUIDUtil;
import org.apache.ranger.common.HTTPUtil;
import org.apache.ranger.common.PropertiesUtil;
import org.apache.ranger.common.RequestContext;
import org.apache.ranger.common.UserSessionBase;
import org.apache.ranger.entity.XXAuthSession;
import org.apache.ranger.security.context.RangerContextHolder;
import org.apache.ranger.security.context.RangerSecurityContext;
import org.apache.ranger.util.RestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

public class RangerSecurityContextFormationFilter extends GenericFilterBean {

	public static final String AKA_SC_SESSION_KEY = "AKA_SECURITY_CONTEXT";
	public static final String USER_AGENT = "User-Agent";

	@Autowired
	SessionMgr sessionMgr;

	@Autowired
	HTTPUtil httpUtil;

	 @Autowired
    XUserMgr xUserMgr;

	@Autowired
	GUIDUtil guidUtil;
		
	String testIP = null;

	public RangerSecurityContextFormationFilter() {
		testIP = PropertiesUtil.getProperty("xa.env.ip");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
	 * javax.servlet.ServletResponse, javax.servlet.FilterChain)
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

		try {
			Authentication auth = SecurityContextHolder.getContext()
					.getAuthentication();

			if (!(auth instanceof AnonymousAuthenticationToken)) {
				HttpServletRequest httpRequest = (HttpServletRequest) request;
				HttpSession httpSession = httpRequest.getSession(false);

				// [1]get the context from session
				RangerSecurityContext context = null;
				if(httpSession!=null){
					context=(RangerSecurityContext) httpSession.getAttribute(AKA_SC_SESSION_KEY);
				}
				int clientTimeOffset = 0;
				if (context == null) {
					context = new RangerSecurityContext();
					httpSession.setAttribute(AKA_SC_SESSION_KEY, context);
				}
				String userAgent = httpRequest.getHeader(USER_AGENT);
				clientTimeOffset=RestUtil.getTimeOffset(httpRequest);

				// Get the request specific info
				RequestContext requestContext = new RequestContext();
				String reqIP = testIP;
				if (testIP == null) {
					reqIP = httpRequest.getRemoteAddr();
				}
				requestContext.setIpAddress(reqIP);
				requestContext.setUserAgent(userAgent);
				requestContext.setDeviceType(httpUtil
						.getDeviceType(httpRequest));
				requestContext.setServerRequestId(guidUtil.genGUID());
				requestContext.setRequestURL(httpRequest.getRequestURI());

				requestContext.setClientTimeOffsetInMinute(clientTimeOffset);
				context.setRequestContext(requestContext);

				RangerContextHolder.setSecurityContext(context);
				int authType = getAuthType(httpRequest);
				UserSessionBase userSession = sessionMgr.processSuccessLogin(
						authType, userAgent, httpRequest);

				if (userSession != null) {
					if (userSession.getClientTimeOffsetInMinute() == 0) {
						userSession.setClientTimeOffsetInMinute(clientTimeOffset);
					}
				}

				context.setUserSession(userSession);
			}
			HttpServletResponse res = (HttpServletResponse)response;
			res.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
			res.setHeader("X-Frame-Options", "DENY" );
			res.setHeader("X-XSS-Protection", "1; mode=block");
			res.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
			res.setHeader("Content-Security-Policy", "default-src 'none'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; connect-src 'self'; img-src 'self'; style-src 'self' 'unsafe-inline';font-src 'self'");
			res.setHeader("X-Permitted-Cross-Domain-Policies", "none");
			chain.doFilter(request, res);

		} finally {
			// [4]remove context from thread-local
			RangerContextHolder.resetSecurityContext();
			RangerContextHolder.resetOpContext();
		}
	}

	private int getAuthType(HttpServletRequest request) {
		int authType;
		Object ssoEnabledObj = request.getAttribute("ssoEnabled");
		Boolean ssoEnabled = ssoEnabledObj != null ? Boolean.valueOf(String.valueOf(ssoEnabledObj)) : PropertiesUtil.getBooleanProperty("ranger.sso.enabled", false);

		if (ssoEnabled) {
			authType = XXAuthSession.AUTH_TYPE_SSO;
		} else if (request.getAttribute("spnegoEnabled") != null && Boolean.valueOf(String.valueOf(request.getAttribute("spnegoEnabled")))){
			if (request.getAttribute("trustedProxyEnabled") != null && Boolean.valueOf(String.valueOf(request.getAttribute("trustedProxyEnabled")))) {
				if (logger.isDebugEnabled()) {
					logger.debug("Setting auth type as trusted proxy");
				}
				authType = XXAuthSession.AUTH_TYPE_TRUSTED_PROXY;
			} else {
				authType = XXAuthSession.AUTH_TYPE_KERBEROS;
			}
		} else {
			authType = XXAuthSession.AUTH_TYPE_PASSWORD;
		}
		return authType;
	}
}
