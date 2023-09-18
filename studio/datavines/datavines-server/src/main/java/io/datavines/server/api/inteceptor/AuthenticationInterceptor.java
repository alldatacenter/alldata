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
package io.datavines.server.api.inteceptor;

import io.datavines.core.constant.DataVinesConstants;
import io.datavines.server.api.annotation.AuthIgnore;
import io.datavines.core.enums.Status;
import io.datavines.server.repository.entity.User;
import io.datavines.server.repository.service.UserService;
import io.datavines.core.exception.DataVinesServerException;
import io.datavines.server.utils.ContextHolder;
import io.datavines.core.utils.TokenManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Objects;

@Slf4j
public class AuthenticationInterceptor implements HandlerInterceptor {

    @Resource
    private TokenManager tokeManager;

    @Resource
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        HandlerMethod handlerMethod = null;
        try {
            handlerMethod = (HandlerMethod) handler;
        } catch (Exception e) {
            response.setStatus(Status.REQUEST_ERROR.getCode());
            return false;
        }
        
        Method method = handlerMethod.getMethod();

        AuthIgnore ignoreAuthMethod = method.getAnnotation(AuthIgnore.class);

        if (null != ignoreAuthMethod) {
            return true;
        }

        String token = request.getHeader(DataVinesConstants.TOKEN_HEADER_STRING);
        if (Objects.isNull(token)){
            throw new DataVinesServerException(Status.TOKEN_IS_NULL_ERROR);
        }
        String username = tokeManager.getUsername(token);
        User user = userService.getByUsername(username);
        if (null == user) {
            throw new DataVinesServerException(Status.INVALID_TOKEN, token);
        }
        request.setAttribute(DataVinesConstants.LOGIN_USER, user);

        if (!tokeManager.validateToken(token, username, tokeManager.getPassword(token))) {
            throw new DataVinesServerException(Status.INVALID_TOKEN, token);
        }

        ContextHolder.setParam(DataVinesConstants.LOGIN_USER, user);

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        ContextHolder.removeAll();
    }
}
