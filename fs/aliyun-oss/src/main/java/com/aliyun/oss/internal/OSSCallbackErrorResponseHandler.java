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
import com.aliyun.oss.ServiceException;
import com.aliyun.oss.common.comm.ResponseHandler;
import com.aliyun.oss.common.comm.ResponseMessage;
import com.aliyun.oss.common.parser.JAXBResponseParser;
import com.aliyun.oss.common.parser.ResponseParseException;
import com.aliyun.oss.common.utils.ExceptionFactory;
import com.aliyun.oss.internal.model.OSSErrorResult;
import com.aliyun.oss.internal.ResponseParsers.ErrorResponseParser;

public class OSSCallbackErrorResponseHandler implements ResponseHandler {
    private static ErrorResponseParser errorResponseParser = new ErrorResponseParser();

    @Override
    public void handle(ResponseMessage response) throws ServiceException, ClientException {
        if (response.getStatusCode() == HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION) {
            try {
                OSSErrorResult errorResult = errorResponseParser.parse(response);
                throw ExceptionFactory.createOSSException(errorResult, response.getErrorResponseAsString());
            } catch (ResponseParseException e) {
                throw ExceptionFactory.createInvalidResponseException(response.getRequestId(),
                        response.getErrorResponseAsString(), e);
            } finally {
                safeCloseResponse(response);
            }
        }
    }

}
