/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.internal;

import static com.aliyun.oss.internal.OSSUtils.safeCloseResponse;
import org.apache.http.HttpStatus;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.comm.ResponseHandler;
import com.aliyun.oss.common.comm.ResponseMessage;
import com.aliyun.oss.common.parser.JAXBResponseParser;
import com.aliyun.oss.common.parser.ResponseParseException;
import com.aliyun.oss.common.utils.ExceptionFactory;
import com.aliyun.oss.internal.model.OSSErrorResult;
import com.aliyun.oss.internal.ResponseParsers.ErrorResponseParser;
/**
 * Used to handle error response from oss, when HTTP status code is not 2xx,
 * then throws <code>OSSException</code> with detailed error information(such as
 * request id, error code).
 */
public class OSSErrorResponseHandler implements ResponseHandler {
    private static ErrorResponseParser errorResponseParser = new ErrorResponseParser();

    public void handle(ResponseMessage response) throws OSSException, ClientException {

        if (response.isSuccessful()) {
            return;
        }

        String requestId = response.getRequestId();
        int statusCode = response.getStatusCode();
        if (response.getContent() == null) {
            /**
             * When HTTP response body is null, handle status code 404 Not
             * Found, 304 Not Modified, 412 Precondition Failed especially.
             */
            if (statusCode == HttpStatus.SC_NOT_FOUND) {
                throw ExceptionFactory.createOSSException(requestId, OSSErrorCode.NO_SUCH_KEY, "Not Found");
            } else if (statusCode == HttpStatus.SC_NOT_MODIFIED) {
                throw ExceptionFactory.createOSSException(requestId, OSSErrorCode.NOT_MODIFIED, "Not Modified");
            } else if (statusCode == HttpStatus.SC_PRECONDITION_FAILED) {
                throw ExceptionFactory.createOSSException(requestId, OSSErrorCode.PRECONDITION_FAILED,
                        "Precondition Failed");
            } else if (statusCode == HttpStatus.SC_FORBIDDEN) {
                throw ExceptionFactory.createOSSException(requestId, OSSErrorCode.ACCESS_FORBIDDEN, "AccessForbidden");
            } else {
                throw ExceptionFactory.createUnknownOSSException(requestId, statusCode);
            }
        }

        try {
            OSSErrorResult errorResult = errorResponseParser.parse(response);
            throw ExceptionFactory.createOSSException(errorResult, response.getErrorResponseAsString());
        } catch (ResponseParseException e) {
            throw ExceptionFactory.createInvalidResponseException(requestId, response.getErrorResponseAsString(), e);
        } finally {
            safeCloseResponse(response);
        }
    }

}
