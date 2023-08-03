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
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.apache.ranger.common.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * RangerMDCFilter filter that captures the HTTP request and insert request-id
 * as part of MDC context which will be captured in the log file for every request
 */

public class RangerMDCFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(RangerMDCFilter.class);

    public  static final String  DEFAULT_MDC_KEY                        = "REQUEST_ID";
    public  static final String  DEFAULT_REQUEST_ID_HEADER_NAME         = "request-id";
    private static final boolean DEFAULT_MDC_FILTER_ENABLED             = false;
    private static final String  PROP_MDC_FILTER_MDC_KEY                = "ranger.admin.mdc-filter.mdcKey";
    private static final String  PROP_MDC_FILTER_REQUEST_ID_HEADER_NAME = "ranger.admin.mdc-filter.requestHeader.name";
    private static final String  PROP_MDC_FILTER_ENABLED                = "ranger.admin.mdc-filter.enabled";

    private String  mdcKey            = DEFAULT_MDC_KEY;
    private String  requestHeaderName = DEFAULT_REQUEST_ID_HEADER_NAME;
    private boolean mdcFilterEnabled  = DEFAULT_MDC_FILTER_ENABLED;


    @Override
    public void init(FilterConfig config) throws ServletException {
        if (log.isDebugEnabled()) {
            log.debug("==> RangerMDCFilter.initialize()");
        }

        mdcFilterEnabled = PropertiesUtil.getBooleanProperty(PROP_MDC_FILTER_ENABLED, DEFAULT_MDC_FILTER_ENABLED);

        if (mdcFilterEnabled) {
            requestHeaderName = PropertiesUtil.getProperty(PROP_MDC_FILTER_REQUEST_ID_HEADER_NAME, DEFAULT_REQUEST_ID_HEADER_NAME);
            mdcKey            = PropertiesUtil.getProperty(PROP_MDC_FILTER_MDC_KEY, DEFAULT_MDC_KEY);

            log.info(PROP_MDC_FILTER_REQUEST_ID_HEADER_NAME + "=" + requestHeaderName);
            log.info(PROP_MDC_FILTER_MDC_KEY + "=" + mdcKey);
        }

        log.info(PROP_MDC_FILTER_ENABLED + "=" + mdcFilterEnabled);

        if (log.isDebugEnabled()) {
            log.debug("<== RangerMDCFilter.initialize()");
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (log.isDebugEnabled()) {
            log.debug("==> RangerMDCFilter.doFilter()");
        }

        if (mdcFilterEnabled) {
            HttpServletRequest httpRequest = (HttpServletRequest)request;
            String             requestId   = httpRequest.getHeader(requestHeaderName);

            if (requestId != null) {
                MDC.put(mdcKey, requestId);
            }

            try {
                chain.doFilter(request, response);
            } finally {
                MDC.clear();
            }
        } else {
            chain.doFilter(request, response);
        }

        if (log.isDebugEnabled()) {
            log.debug("<== RangerMDCFilter.doFilter()");
        }
    }

    @Override
    public void destroy() {
    }
}
