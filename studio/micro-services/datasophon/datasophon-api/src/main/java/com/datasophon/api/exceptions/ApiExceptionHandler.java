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
package com.datasophon.api.exceptions;

import com.datasophon.api.enums.Status;
import com.datasophon.common.utils.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Exception Handler
 */
@ControllerAdvice
@ResponseBody
public class ApiExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public Result exceptionHandler(Exception e, HandlerMethod hm) {
        ApiException ce = hm.getMethodAnnotation(ApiException.class);
        if (ce == null) {
            logger.error(e.getMessage(), e);
            return Result.error(Status.INTERNAL_SERVER_ERROR_ARGS.getCode(), e.getMessage());
        }
        Status st = ce.value();
        logger.error(st.getMsg(), e);
        return Result.error(st.getCode(),st.getMsg());
    }

    /**
     * 请求参数验证异常处理
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result constraintViolationException(ConstraintViolationException e) {
        Set<String> set = e.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessageTemplate)
                .collect(Collectors.toSet());
        return Result.error(String.join(",", set));
    }

    /**
     * 请求参数转换异常处理
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result exceptionHandler(MethodArgumentTypeMismatchException e) {
        return Result.error("参数类型错不匹配：" + e.getMessage());
    }

}
