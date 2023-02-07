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

package org.apache.inlong.manager.web.auth.web;

import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.pojo.user.UserInfo;
import org.apache.inlong.manager.service.user.LoginUserUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
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

/**
 * Filter of web user authentication.
 */
@Slf4j
public class AuthenticationFilter implements Filter {

    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationFilter.class);

    public AuthenticationFilter() {
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;

        Subject subject = SecurityUtils.getSubject();
        if (subject.isAuthenticated()) {
            UserInfo loginUserInfo = (UserInfo) subject.getPrincipal();
            doFilter(servletRequest, servletResponse, filterChain, loginUserInfo);
            return;
        }

        try {
            UsernamePasswordToken token = getPasswordToken(servletRequest);
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
        doFilter(servletRequest, servletResponse, filterChain, (UserInfo) subject.getPrincipal());
    }

    private void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain,
            UserInfo userInfo) throws IOException, ServletException {
        LoginUserUtils.setUserLoginInfo(userInfo);
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            LoginUserUtils.removeUserLoginInfo();
        }
    }

    private UsernamePasswordToken getPasswordToken(ServletRequest servletRequest) {
        String username = servletRequest.getParameter(USERNAME);
        String password = servletRequest.getParameter(PASSWORD);
        Preconditions.checkNotNull(username, "please input username");
        Preconditions.checkNotNull(password, "please input password");
        return new UsernamePasswordToken(username, password);
    }

    @Override
    public void destroy() {

    }
}
