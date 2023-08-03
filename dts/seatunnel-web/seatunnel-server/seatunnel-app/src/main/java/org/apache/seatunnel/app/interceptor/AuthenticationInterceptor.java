/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seatunnel.app.interceptor;

import org.apache.seatunnel.app.common.Constants;
import org.apache.seatunnel.app.dal.dao.IUserDao;
import org.apache.seatunnel.app.dal.entity.User;
import org.apache.seatunnel.app.dal.entity.UserLoginLog;
import org.apache.seatunnel.app.security.JwtUtils;

import org.apache.commons.lang3.StringUtils;

import org.eclipse.jetty.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Map;
import java.util.Objects;

import static io.jsonwebtoken.Claims.EXPIRATION;
import static org.apache.seatunnel.server.common.Constants.OPTIONS;
import static org.apache.seatunnel.server.common.Constants.TOKEN;
import static org.apache.seatunnel.server.common.Constants.USER_ID;

@Slf4j
public class AuthenticationInterceptor implements HandlerInterceptor {

    @Resource private IUserDao userDaoImpl;

    @Resource private JwtUtils jwtUtils;

    @Override
    @SuppressWarnings("MagicNumber")
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (request.getMethod().equals(OPTIONS)) {
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Headers", "*");
            response.setHeader("Access-Control-Allow-Methods", "*");
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Max-Age", "3600");
            return true;
        }

        long currentTimestamp = System.currentTimeMillis();
        final String token = request.getHeader(TOKEN);
        if (StringUtils.isBlank(token)) {
            log.info("user does not exist");
            response.setStatus(HttpStatus.UNAUTHORIZED_401);
            return false;
        }
        final Map<String, Object> map = jwtUtils.parseToken(token);
        final Integer userId = (Integer) map.get(USER_ID);
        if (Objects.isNull(userId)) {
            log.info("userId does not exist");
            response.setStatus(HttpStatus.UNAUTHORIZED_401);
            return false;
        }
        final UserLoginLog userLoginLog = userDaoImpl.getLastLoginLog(userId);
        if (Objects.isNull(userLoginLog) || !userLoginLog.getTokenStatus()) {
            log.info("userLoginLog does not exist");
            response.setStatus(HttpStatus.UNAUTHORIZED_401);
            return false;
        }

        final Integer expireDate = (Integer) map.get(EXPIRATION);
        if (Objects.isNull(expireDate) || currentTimestamp - (long) expireDate * 1000 > 0) {
            log.info("user token has expired");
            response.setStatus(HttpStatus.UNAUTHORIZED_401);
            return false;
        }

        map.forEach(request::setAttribute);
        User user = new User();
        user.setUsername((String) map.get("name"));
        user.setId((Integer) map.get("id"));
        //        user.setStatus((Byte) map.get("status"));
        //        user.setType((Byte) map.get("type"));
        request.setAttribute(Constants.SESSION_USER, user);
        request.setAttribute("userId", userId);
        return true;
    }

    @Override
    public void postHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            ModelAndView modelAndView)
            throws Exception {
        // do nothing
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        // do nothing
    }
}
