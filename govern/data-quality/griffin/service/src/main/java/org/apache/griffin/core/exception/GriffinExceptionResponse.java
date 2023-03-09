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

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Date;

import org.springframework.http.HttpStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GriffinExceptionResponse {

    private Date timestamp = new Date();
    private int status;
    private String error;
    private String code;
    private String message;
    private String exception;
    private String path;


    GriffinExceptionResponse(HttpStatus status, GriffinExceptionMessage message,
                             String path) {
        this.status = status.value();
        this.error = status.getReasonPhrase();
        this.code = Integer.toString(message.getCode());
        this.message = message.getMessage();
        this.path = path;
    }

    GriffinExceptionResponse(HttpStatus status, String message, String path,
                             String exception) {
        this.status = status.value();
        this.error = status.getReasonPhrase();
        this.message = message;
        this.path = path;
        this.exception = exception;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public String getException() {
        return exception;
    }
}
