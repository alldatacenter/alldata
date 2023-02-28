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

import io.datavines.core.entity.ResultMap;
import io.datavines.core.enums.Status;
import io.datavines.core.utils.TokenManager;
import io.datavines.core.exception.DataVinesServerException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@RestControllerAdvice
public class DataVinesExceptionHandler {

    @Resource
    private TokenManager tokenManager;

    @ExceptionHandler(DataVinesServerException.class)
    public ResponseEntity<ResultMap> dataVinesServerExceptionHandler(DataVinesServerException e, HttpServletRequest request) {
        log.error("DataVinesServerException:", e);
        ResultMap resultMap = new ResultMap(tokenManager);
        Status status = e.getStatus();
        if (Status.INVALID_TOKEN.equals(status)){
            return ResponseEntity.ok(new ResultMap().fail(Status.INVALID_TOKEN.getCode()).message("invalid token！"));
        }
        if (!Objects.isNull(status) ) {
            resultMap.fail(status.getCode());
        }
        resultMap.failAndRefreshToken(request);
        resultMap.message(e.getMessage());
        return ResponseEntity.ok(resultMap);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ResultMap> constraintViolationExceptionHandler(Exception e, HttpServletRequest request) {
        log.error("ConstraintViolationException:", e);
        ResultMap resultMap = new ResultMap(tokenManager);
        resultMap.failAndRefreshToken(request);
        resultMap.message(buildValidFailMessage((ConstraintViolationException) e));
        return ResponseEntity.ok(resultMap);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResultMap> methodArgumentNotValidExceptionHandler(Exception e, HttpServletRequest request) {
        log.error("MethodArgumentNotValidException:", e);
        ResultMap resultMap = new ResultMap(tokenManager);
        resultMap.failAndRefreshToken(request);
        String message = buildValidFailMessage((MethodArgumentNotValidException) e);
        resultMap.message(message);
        return ResponseEntity.ok(resultMap);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResultMap> commonExceptionHandler(Exception e, HttpServletRequest request) {
        log.error("Exception:", e);
        ResultMap resultMap = new ResultMap(tokenManager);
        resultMap.failAndRefreshToken(request);
        resultMap.message(e.getMessage());
        return ResponseEntity.ok(resultMap);
    }

    public String buildValidFailMessage(ConstraintViolationException violationException) {
        Set<ConstraintViolation<?>> constraintViolationSet = violationException.getConstraintViolations();
        StringBuilder messageBuilder = new StringBuilder();
        if(CollectionUtils.isEmpty(constraintViolationSet)){
            messageBuilder.append("invalid param");
        }
        Iterator<ConstraintViolation<?>> iterator = constraintViolationSet.iterator();
        while (iterator.hasNext()) {
            ConstraintViolation<?> next = iterator.next();
            messageBuilder.append(next.getMessage());
            if (iterator.hasNext()) {
                messageBuilder.append(System.lineSeparator());
            }
        }
        return messageBuilder.toString();
    }

    private String buildValidFailMessage(MethodArgumentNotValidException notValidException) {
        BindingResult bindingResult = notValidException.getBindingResult();
        List<ObjectError> errorList = bindingResult.getAllErrors();

        StringBuilder messageBuilder = new StringBuilder();
        if(CollectionUtils.isEmpty(errorList)){
            messageBuilder.append("invalid param");
        }
        Iterator<ObjectError> iterator = errorList.iterator();
        while (iterator.hasNext()) {
            ObjectError next = iterator.next();
            messageBuilder.append(next.getDefaultMessage());
            if (iterator.hasNext()) {
                messageBuilder.append(System.lineSeparator());
            }
        }
        String message = messageBuilder.toString();
        return message;
    }
}
