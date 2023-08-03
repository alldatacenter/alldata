/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.web.filter;

import org.apache.inlong.manager.web.utils.InlongRequestWrapper;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.web.servlet.ShiroHttpServletRequest;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import static org.apache.inlong.common.util.BasicAuth.BASIC_AUTH_TENANT_HEADER;

/**
 * Tenant info insertion filter.
 * Add tenant into param and body of each request
 */
@Slf4j
public class TenantInsertionFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String tenant = request.getHeader(BASIC_AUTH_TENANT_HEADER);
        if (StringUtils.isBlank(tenant)) {
            String errMsg = "tenant should not be blank";
            log.error(errMsg);
            ((HttpServletResponse) servletResponse).sendError(HttpServletResponse.SC_BAD_REQUEST, errMsg);
            return;
        }

        InlongRequestWrapper inlongWrapper;
        if (request instanceof ShiroHttpServletRequest) {
            ShiroHttpServletRequest shiroWrapper = (ShiroHttpServletRequest) request;
            inlongWrapper = (InlongRequestWrapper) shiroWrapper.getRequest();
        } else if (request instanceof InlongRequestWrapper) {
            inlongWrapper = (InlongRequestWrapper) request;
        } else {
            String errMsg = "request should be one of ShiroHttpServletRequest or InlongRequestWrapper type";
            log.error(errMsg);
            ((HttpServletResponse) servletResponse).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errMsg);
            return;
        }

        inlongWrapper.addParameter(BASIC_AUTH_TENANT_HEADER, tenant);
        filterChain.doFilter(inlongWrapper, servletResponse);

    }
}
