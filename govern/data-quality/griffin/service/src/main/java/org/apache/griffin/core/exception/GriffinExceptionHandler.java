/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package org.apache.griffin.core.exception;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GriffinExceptionHandler {

    @SuppressWarnings("rawtypes")
    @ExceptionHandler(GriffinException.ServiceException.class)
    public ResponseEntity handleGriffinExceptionOfServer(
        HttpServletRequest request,
        GriffinException.ServiceException e) {
        String message = e.getMessage();
        Throwable cause = e.getCause();
        GriffinExceptionResponse body = new GriffinExceptionResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            message, request.getRequestURI(), cause.getClass().getName());
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @SuppressWarnings("rawtypes")
    @ExceptionHandler(GriffinException.class)
    public ResponseEntity handleGriffinExceptionOfClient(
        HttpServletRequest request, GriffinException e) {
        ResponseStatus responseStatus = AnnotationUtils.findAnnotation(
            e.getClass(), ResponseStatus.class);
        HttpStatus status = responseStatus.code();
        String code = e.getMessage();
        GriffinExceptionMessage message = GriffinExceptionMessage
            .valueOf(Integer.valueOf(code));
        GriffinExceptionResponse body = new GriffinExceptionResponse(
            status, message, request.getRequestURI());
        return new ResponseEntity<>(body, status);
    }
}
