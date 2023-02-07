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

package org.apache.inlong.manager.web.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.exceptions.WorkflowException;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.user.UserInfo;
import org.apache.inlong.manager.service.user.LoginUserUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.UnauthorizedException;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Set;

/**
 * Handler of controller exception.
 */
@Slf4j
@RestControllerAdvice
public class ControllerExceptionHandler {

    private static final String ERROR_MSG = "failed to handle request on path: %s by user: %s";

    @ExceptionHandler(ConstraintViolationException.class)
    public Response<String> handleConstraintViolationException(HttpServletRequest request,
            ConstraintViolationException e) {
        UserInfo userInfo = LoginUserUtils.getLoginUser();
        String username = userInfo != null ? userInfo.getName() : "";
        log.error(String.format(ERROR_MSG, request.getRequestURI(), username), e);

        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        StringBuilder stringBuilder = new StringBuilder(64);
        for (ConstraintViolation<?> violation : violations) {
            stringBuilder.append(violation.getMessage()).append(".");
        }

        return Response.fail(stringBuilder.toString());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Response<String> handleMethodArgumentNotValidException(HttpServletRequest request,
            MethodArgumentNotValidException e) {
        UserInfo userInfo = LoginUserUtils.getLoginUser();
        String username = userInfo != null ? userInfo.getName() : "";
        log.error(String.format(ERROR_MSG, request.getRequestURI(), username), e);

        StringBuilder builder = new StringBuilder();
        BindingResult result = e.getBindingResult();
        result.getFieldErrors().forEach(
                error -> builder.append(error.getField()).append(": ")
                        .append(error.getDefaultMessage()).append(System.lineSeparator()));

        result.getGlobalErrors().forEach(
                error -> builder.append(error.getDefaultMessage()).append(System.lineSeparator()));

        return Response.fail(builder.toString());
    }

    @ExceptionHandler(value = IllegalArgumentException.class)
    public Response<String> handleIllegalArgumentException(HttpServletRequest request, IllegalArgumentException e) {
        UserInfo userInfo = LoginUserUtils.getLoginUser();
        String username = userInfo != null ? userInfo.getName() : "";
        log.error(String.format(ERROR_MSG, request.getRequestURI(), username), e);
        return Response.fail(e.getMessage());
    }

    @ExceptionHandler(value = BindException.class)
    public Response<String> handleBindExceptionHandler(HttpServletRequest request, BindException e) {
        UserInfo userInfo = LoginUserUtils.getLoginUser();
        String username = userInfo != null ? userInfo.getName() : "";
        log.error(String.format(ERROR_MSG, request.getRequestURI(), username), e);

        StringBuilder builder = new StringBuilder();
        e.getBindingResult().getFieldErrors().forEach(
                error -> builder.append(error.getField()).append(": ")
                        .append(error.getDefaultMessage()).append(System.lineSeparator()));
        return Response.fail(builder.toString());
    }

    @ExceptionHandler(value = HttpMessageConversionException.class)
    public Response<String> handleHttpMessageConversionExceptionHandler(HttpServletRequest request,
            HttpMessageConversionException e) {
        UserInfo userInfo = LoginUserUtils.getLoginUser();
        String username = userInfo != null ? userInfo.getName() : "";
        log.error(String.format(ERROR_MSG, request.getRequestURI(), username), e);
        return Response.fail("http message convert exception! pls check params");
    }

    @ExceptionHandler(value = WorkflowException.class)
    public Response<String> handleWorkflowException(HttpServletRequest request, WorkflowException e) {
        UserInfo userInfo = LoginUserUtils.getLoginUser();
        String username = userInfo != null ? userInfo.getName() : "";
        log.error(String.format(ERROR_MSG, request.getRequestURI(), username), e);
        return Response.fail(e.getMessage());
    }

    @ExceptionHandler(value = BusinessException.class)
    public Response<String> handleBusinessExceptionHandler(HttpServletRequest request, BusinessException e) {
        UserInfo userInfo = LoginUserUtils.getLoginUser();
        String username = userInfo != null ? userInfo.getName() : "";
        log.error(String.format(ERROR_MSG, request.getRequestURI(), username), e);
        return Response.fail(e.getMessage());
    }

    @ExceptionHandler(value = AuthenticationException.class)
    public Response<String> handleAuthenticationException(HttpServletRequest request, AuthenticationException e) {
        log.error(String.format(ERROR_MSG, request.getRequestURI(), ""), e);
        return Response.fail("Username or password was incorrect, or the account has expired");
    }

    @ExceptionHandler(value = UnauthorizedException.class)
    public Response<String> handleUnauthorizedException(HttpServletRequest request, AuthorizationException e) {
        UserInfo userInfo = LoginUserUtils.getLoginUser();
        String username = userInfo != null ? userInfo.getName() : "";
        log.error(String.format(ERROR_MSG, request.getRequestURI(), username), e);
        return Response.fail(String.format("Current user [%s] has no permission to access URL",
                (userInfo != null ? userInfo.getName() : "")));
    }

    @ExceptionHandler(Exception.class)
    public Response<String> handle(HttpServletRequest request, Exception e) {
        UserInfo userInfo = LoginUserUtils.getLoginUser();
        String username = userInfo != null ? userInfo.getName() : "";
        log.error(String.format(ERROR_MSG, request.getRequestURI(), username), e);
        return Response.fail("There was an error in the service..."
                + "Please try again later! "
                + "If there are still problems, please contact the administrator");
    }
}
