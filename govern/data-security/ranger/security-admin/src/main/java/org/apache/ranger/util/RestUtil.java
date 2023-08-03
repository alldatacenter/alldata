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

 package org.apache.ranger.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.ranger.security.context.RangerContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Enumeration;

@Component
public class RestUtil {

	private static final Logger LOG = LoggerFactory.getLogger(RestUtil.class);
	public static final String timeOffsetCookieName = "clientTimeOffset";
	public static final String TIMEOUT_ACTION = "timeout";
	private static final String PROXY_RANGER_URL_PATH = "/ranger";
	public static final String LOCAL_LOGIN_URL = "locallogin";

	public static Integer getTimeOffset(HttpServletRequest request) {
		Integer cookieVal = 0;
		try{		
			Cookie[] cookies = request.getCookies();
			String timeOffset = null;
			
			if (cookies != null) {
				for (Cookie cookie : cookies) {
					try {
						if (timeOffsetCookieName.equals(cookie.getName())) {
							timeOffset = cookie.getValue();
							if (timeOffset != null) {
								cookieVal = Integer.parseInt(timeOffset);
							}
							break;
						}
					} catch (Exception ex) {
						cookieVal = 0;
					}
				}
			}
		}catch(Exception ex){
			
		}
		return cookieVal;
	}
	
	public static int getClientTimeOffset(){
		int clientTimeOffsetInMinute = 0;
		try{
			clientTimeOffsetInMinute= RangerContextHolder.getSecurityContext().getRequestContext().getClientTimeOffsetInMinute();
		}catch(Exception ex){
			
		}
		if(clientTimeOffsetInMinute==0){
			try{
				clientTimeOffsetInMinute= RangerContextHolder.getSecurityContext().getUserSession().getClientTimeOffsetInMinute();
			}catch(Exception ex){
				
			}
		}
		return clientTimeOffsetInMinute;
	}

	public static String constructForwardableURL(HttpServletRequest httpRequest) {
		String xForwardedProto = "";
		String xForwardedHost = "";
		String xForwardedContext = "";
		Enumeration<?> names = httpRequest.getHeaderNames();
		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			Enumeration<?> values = httpRequest.getHeaders(name);
			String value = "";
			if (values != null) {
				while (values.hasMoreElements()) {
					value = (String) values.nextElement();
				}
			}
			if (StringUtils.trimToNull(name) != null && StringUtils.trimToNull(value) != null) {
				if (name.equalsIgnoreCase("x-forwarded-proto")) {
					xForwardedProto = value;
				} else if (name.equalsIgnoreCase("x-forwarded-host")) {
					xForwardedHost = value;
				} else if (name.equalsIgnoreCase("x-forwarded-context")) {
					xForwardedContext = value;
				}
			}
		}
		if (xForwardedHost.contains(",")) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("xForwardedHost value is " + xForwardedHost + " it contains multiple hosts, selecting the first host.");
			}
			xForwardedHost = xForwardedHost.split(",")[0].trim();
		}
		String xForwardedURL = "";
		if (StringUtils.trimToNull(xForwardedProto) != null) {
			//if header contains x-forwarded-host and x-forwarded-context
			if (StringUtils.trimToNull(xForwardedHost) != null && StringUtils.trimToNull(xForwardedContext) != null) {
				xForwardedURL = xForwardedProto + "://" + xForwardedHost + xForwardedContext + PROXY_RANGER_URL_PATH + httpRequest.getRequestURI();
			} else if (StringUtils.trimToNull(xForwardedHost) != null) {
				//if header contains x-forwarded-host and does not contains x-forwarded-context
				xForwardedURL = xForwardedProto + "://" + xForwardedHost + httpRequest.getRequestURI();
			} else {
				//if header does not contains x-forwarded-host and x-forwarded-context
				//preserve the x-forwarded-proto value coming from the request.
				String requestURL = httpRequest.getRequestURL().toString();
				if (StringUtils.trimToNull(requestURL) != null && requestURL.startsWith("http:")) {
					requestURL = requestURL.replaceFirst("http", xForwardedProto);
				}
				xForwardedURL = requestURL;
			}
		}
		return xForwardedURL;
	}

	public static String constructRedirectURL(HttpServletRequest request, String redirectUrl, String xForwardedURL, String originalUrlQueryParam) {
		String delimiter = "?";
		if (redirectUrl.contains("?")) {
			delimiter = "&";
		}
		String loginURL = redirectUrl + delimiter + originalUrlQueryParam + "=";
		if (StringUtils.trimToNull(xForwardedURL) != null) {
			loginURL += xForwardedURL + getOriginalQueryString(request);
		} else {
			loginURL += request.getRequestURL().append(getOriginalQueryString(request));
		}
		return loginURL;
	}

	private static String getOriginalQueryString(HttpServletRequest request) {
		String originalQueryString = request.getQueryString();
		if (LOG.isDebugEnabled()) {
			LOG.debug("originalQueryString = " + originalQueryString);
		}
		if (originalQueryString == null || originalQueryString.contains("action")) {
			return "";
		} else {
			return "?" + originalQueryString;
		}
	}
}