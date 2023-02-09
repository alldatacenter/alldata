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

package com.aliyun.oss;

/**
 * <p>
 * This exception is the one thrown by the client side when accessing OSS.
 * </p>
 * 
 * <p>
 * {@link ClientException} is the class to represent any exception in OSS client
 * side. Generally ClientException occurs either before sending the request or
 * after receving the response from OSS server side. For example, if the network
 * is broken when it tries to send a request, then the SDK will throw a
 * {@link ClientException} instance.
 * </p>
 * 
 * <p>
 * {@link ServiceException} is converted from error code from OSS response. For
 * example, when OSS tries to authenticate a request, if Access ID does not
 * exist, the SDK will throw a {@link ServiceException} or its subclass instance
 * with the specific error code, which the caller could handle that with
 * specific logic.
 * </p>
 * 
 */
public class ClientException extends RuntimeException {

    private static final long serialVersionUID = 1870835486798448798L;

    private String errorMessage;
    private String requestId;
    private String errorCode;

    /**
     * Creates a default instance.
     */
    public ClientException() {
        super();
    }

    /**
     * Creates an instance with error message.
     * 
     * @param errorMessage
     *            Error message.
     */
    public ClientException(String errorMessage) {
        this(errorMessage, null);
    }

    /**
     * Creates an instance with an exception
     * 
     * @param cause
     *            An exception.
     */
    public ClientException(Throwable cause) {
        this(null, cause);
    }

    /**
     * Creates an instance with error message and an exception.
     * 
     * @param errorMessage
     *            Error message.
     * @param cause
     *            An exception.
     */
    public ClientException(String errorMessage, Throwable cause) {
        super(null, cause);
        this.errorMessage = errorMessage;
        this.errorCode = ClientErrorCode.UNKNOWN;
        this.requestId = "Unknown";
    }

    /**
     * Creates an instance with error message, error code, request Id
     * 
     * @param errorMessage
     *            Error message.
     * @param errorCode
     *            Error code, which typically is from a set of predefined
     *            errors. The handler code could do action based on this.
     * @param requestId
     *            Request Id.
     */
    public ClientException(String errorMessage, String errorCode, String requestId) {
        this(errorMessage, errorCode, requestId, null);
    }

    /**
     * Creates an instance with error message, error code, request Id and an
     * exception.
     * 
     * @param errorMessage
     *            Error message.
     * @param errorCode
     *            Error code.
     * @param requestId
     *            Request Id.
     * @param cause
     *            An exception.
     */
    public ClientException(String errorMessage, String errorCode, String requestId, Throwable cause) {
        this(errorMessage, cause);
        this.errorCode = errorCode;
        this.requestId = requestId;
    }

    /**
     * Get error message.
     * 
     * @return Error message in string.
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Get error code.
     * 
     * @return Error code.
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Gets request id.
     * 
     * @return The request Id.
     */
    public String getRequestId() {
        return requestId;
    }

    @Override
    public String getMessage() {
        return getErrorMessage() + "\n[ErrorCode]: " + (errorCode != null ? errorCode
                : "") + "\n[RequestId]: " + (requestId != null ? requestId : "");
    }
}
