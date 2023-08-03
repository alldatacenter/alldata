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

import org.apache.commons.lang3.StringUtils;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import java.io.IOException;

import static org.apache.inlong.common.util.BasicAuth.BASIC_AUTH_TENANT_HEADER;
import static org.apache.inlong.common.util.BasicAuth.DEFAULT_TENANT;

/**
 * Empty tenant filter.
 * Add default tenant to header if the tenant has not been specified.
 */
public class EmptyTenantFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String tenant = request.getHeader(BASIC_AUTH_TENANT_HEADER);
        if (StringUtils.isBlank(tenant)) {
            ((InlongRequestWrapper) servletRequest).addHeader(BASIC_AUTH_TENANT_HEADER, DEFAULT_TENANT);
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
