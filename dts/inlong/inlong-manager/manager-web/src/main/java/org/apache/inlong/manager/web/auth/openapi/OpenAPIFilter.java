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

package org.apache.inlong.manager.web.auth.openapi;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.inlong.common.util.BasicAuth;
import org.apache.inlong.manager.pojo.user.UserInfo;
import org.apache.inlong.manager.service.user.LoginUserUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;

/**
 * Filter of open api authentication.
 */
@Slf4j
public class OpenAPIFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAPIFilter.class);

    public OpenAPIFilter() {
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @SneakyThrows
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        Subject subject = SecurityUtils.getSubject();
        try {
            SecretToken token = parseBasicAuth(httpServletRequest);
            subject.login(token);
        } catch (Exception ex) {
            LOGGER.error("login error: {}", ex.getMessage());
            ((HttpServletResponse) servletResponse).sendError(HttpServletResponse.SC_FORBIDDEN, ex.getMessage());
            return;
        }

        if (!subject.isAuthenticated()) {
            log.error("Access denied for anonymous user:{}, path:{} ", subject.getPrincipal(),
                    httpServletRequest.getServletPath());
            ((HttpServletResponse) servletResponse).sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        LoginUserUtils.setUserLoginInfo((UserInfo) subject.getPrincipal());
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            LoginUserUtils.removeUserLoginInfo();
        }
    }

    private SecretToken parseBasicAuth(HttpServletRequest servletRequest) {
        String basicAuth = servletRequest.getHeader(BasicAuth.BASIC_AUTH_HEADER);
        if (StringUtils.isBlank(basicAuth)) {
            log.error("basic auth header is empty");
            return null;
        }

        // Basic auth string must be "Basic Base64(ID:Secret)"
        String[] parts = basicAuth.split(BasicAuth.BASIC_AUTH_SEPARATOR);
        if (parts.length != 2) {
            log.error("the length parts size error: {}", parts.length);
            return null;
        }
        if (!parts[0].equals(BasicAuth.BASIC_AUTH_PREFIX)) {
            log.error("prefix error: {}", parts[0]);
            return null;
        }

        String joinedPair = new String(Base64.getDecoder().decode(parts[1]));
        String[] pair = joinedPair.split(BasicAuth.BASIC_AUTH_JOINER);
        if (pair.length != 2) {
            log.error("pair format error: {}", pair.length);
            return null;
        }

        String secretId = pair[0];
        String secretKey = pair[1];
        if (StringUtils.isBlank(secretId) || StringUtils.isBlank(secretKey)) {
            log.error("invalid id = {} or key = {}", secretId, secretKey);
            return null;
        }

        return new SecretToken(secretId, secretKey);
    }

    @Override
    public void destroy() {

    }
}
