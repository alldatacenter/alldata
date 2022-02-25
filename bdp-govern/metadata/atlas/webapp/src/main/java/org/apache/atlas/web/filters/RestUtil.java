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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

public class RestUtil {

    private static final Logger LOG = LoggerFactory.getLogger(RestUtil.class);
    public static final String TIMEOUT_ACTION = "timeout";
    public static final String LOGOUT_URL = "/logout.html";
    private static final String PROXY_ATLAS_URL_PATH = "/atlas";
    private static final String X_FORWARDED_PROTO = "x-forwarded-proto";
    private static final String X_FORWARDED_HOST = "x-forwarded-host";
    private static final String X_FORWARDED_CONTEXT = "x-forwarded-context";
    public static final String DELIMITTER = "://";

    public static String constructForwardableURL(HttpServletRequest httpRequest) {
        String xForwardedProto = "";
        String xForwardedHost = "";
        String xForwardedContext = "";
        Enumeration<?> headerNames = httpRequest.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = (String) headerNames.nextElement();
            Enumeration<?> values = httpRequest.getHeaders(name);
            String value = "";
            if (values != null) {
                while (values.hasMoreElements()) {
                    value = (String) values.nextElement();
                }
            }
            if (StringUtils.trimToNull(name) != null && StringUtils.trimToNull(value) != null) {
                if (name.equalsIgnoreCase(X_FORWARDED_PROTO)) {
                    xForwardedProto = value;
                } else if (name.equalsIgnoreCase(X_FORWARDED_HOST)) {
                    xForwardedHost = value;
                } else if (name.equalsIgnoreCase(X_FORWARDED_CONTEXT)) {
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
                xForwardedURL = xForwardedProto + DELIMITTER + xForwardedHost + xForwardedContext + PROXY_ATLAS_URL_PATH + httpRequest.getRequestURI();
            } else if (StringUtils.trimToNull(xForwardedHost) != null) {
                //if header contains x-forwarded-host and does not contains x-forwarded-context
                xForwardedURL = xForwardedProto + DELIMITTER + xForwardedHost + httpRequest.getRequestURI();
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
            loginURL += xForwardedURL;
        } else {
            loginURL += request.getRequestURL().append(getOriginalQueryString(request));
        }
        return loginURL;
    }

    private static String getOriginalQueryString(HttpServletRequest request) {
        String originalQueryString = request.getQueryString();
        return (originalQueryString == null) ? "" : "?" + originalQueryString;
    }
}
