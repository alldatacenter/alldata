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

package org.apache.inlong.manager.web.auth.tenant;

import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.pojo.user.UserInfo;

import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

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
 * Shiro filter to check if the request user has the permission to target tenant.
 */
@Slf4j
public class TenantAuthenticatingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        Subject subject = SecurityUtils.getSubject();

        // pre-check
        if (!subject.isAuthenticated()) {
            log.error("The request should be authed before tenant authentication, user:{}, path:{}",
                    subject.getPrincipal(),
                    httpServletRequest.getServletPath());
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        // tenant auth
        UserInfo loginUserInfo = (UserInfo) subject.getPrincipal();
        String tenant = httpServletRequest.getHeader(BASIC_AUTH_TENANT_HEADER);
        try {
            Preconditions.expectNotBlank(tenant, "tenant should not be null or blank");
            subject.login(new TenantToken(loginUserInfo.getName(), tenant));
        } catch (Exception ex) {
            log.error("tenant auth error: {}", ex.getMessage());
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN, ex.getMessage());
            return;
        }

        // check tenant auth result
        if (!subject.isAuthenticated()) {
            log.error("Access denied for user:{}, tenant:{}, path:{} ", subject.getPrincipal(), tenant,
                    httpServletRequest.getServletPath());
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // next filter
        chain.doFilter(request, response);
    }
}
