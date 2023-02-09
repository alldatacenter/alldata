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
 * This is the base exception class to represent any expected or unexpected OSS
 * server side errors.
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
 * <p>
 * On the other side, {@link ClientException} is the class to represent any
 * exception in OSS client side. Generally ClientException occurs either before
 * sending the request or after receving the response from OSS server side. For
 * example, if the network is broken when it tries to send a request, then the
 * SDK will throw a {@link ClientException} instance.
 * </p>
 * 
 * <p>
 * So generally speaking, the caller only needs to handle
 * {@link ServiceException} properly as it means the request is processed, but
 * not completely finished due to different errors. The error code in the
 * exception is a good diagnostics information. Sometimes these exceptions are
 * completely expected.
 * </p>
 */
public class ServiceException extends RuntimeException {

    private static final long serialVersionUID = 430933593095358673L;

    private String errorMessage;
    private String errorCode;
    private String requestId;
    private String hostId;

    private String rawResponseError;

    /**
     * Creates a default instance.
     */
    public ServiceException() {
        super();
    }

    /**
     * Creates an instance with the error message.
     * 
     * @param errorMessage
     *            Error message.
     */
    public ServiceException(String errorMessage) {
        super((String) null);
        this.errorMessage = errorMessage;
    }

    /**
     * Creates an instance with a {@link Throwable} instance.
     * 
     * @param cause
     *            A {@link Throwable} instance.
     */
    public ServiceException(Throwable cause) {
        super(null, cause);
    }

    /**
     * Creates an instance with a {@link Throwable} instance and error message.
     * 
     * @param errorMessage
     *            Error message.
     * @param cause
     *            A {@link Throwable} instance.
     */
    public ServiceException(String errorMessage, Throwable cause) {
        super(null, cause);
        this.errorMessage = errorMessage;
    }

    /**
     * Creates an instance with error message, error code, request id, host id.
     * 
     * @param errorMessage
     *            Error message.
     * @param errorCode
     *            Error code.
     * @param requestId
     *            Request Id.
     * @param hostId
     *            Host Id.
     */
    public ServiceException(String errorMessage, String errorCode, String requestId, String hostId) {
        this(errorMessage, errorCode, requestId, hostId, null);
    }

    /**
     * Creates an instance with error message, error code, request id, host id.
     * 
     * @param errorMessage
     *            Error message.
     * @param errorCode
     *            Error code.
     * @param requestId
     *            Request Id.
     * @param hostId
     *            Host Id.
     * @param cause
     *            A {@link Throwable} instance indicates a specific exception.
     */
    public ServiceException(String errorMessage, String errorCode, String requestId, String hostId, Throwable cause) {
        this(errorMessage, errorCode, requestId, hostId, null, cause);
    }

    /**
     * Creates an instance with error message, error code, request id, host id,
     * OSS response error, and a Throwable instance.
     * 
     * @param errorMessage
     *            Error message.
     * @param errorCode
     *            Error code.
     * @param requestId
     *            Request Id.
     * @param hostId
     *            Host Id.
     * @param rawResponseError
     *            OSS error message in response.
     * @param cause
     *            A {@link Throwable} instance indicates a specific exception.
     */
    public ServiceException(String errorMessage, String errorCode, String requestId, String hostId,
            String rawResponseError, Throwable cause) {
        this(errorMessage, cause);
        this.errorCode = errorCode;
        this.requestId = requestId;
        this.hostId = hostId;
        this.rawResponseError = rawResponseError;
    }

    /**
     * Gets error message.
     * 
     * @return Error message.
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Gets the error code.
     * 
     * @return The error code in string.
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Gets the request id.
     * 
     * @return The request Id in string.
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * Gets the host id.
     * 
     * @return The host Id in string.
     */
    public String getHostId() {
        return hostId;
    }

    /**
     * Gets the error message in OSS response.
     * 
     * @return Error response in string.
     */
    public String getRawResponseError() {
        return rawResponseError;
    }

    /**
     * Sets the error response from OSS.
     * 
     * @param rawResponseError
     *            The error response from OSS.
     */
    public void setRawResponseError(String rawResponseError) {
        this.rawResponseError = rawResponseError;
    }

    private String formatRawResponseError() {
        if (rawResponseError == null || rawResponseError.equals("")) {
            return "";
        }
        return String.format("\n[ResponseError]:\n%s", this.rawResponseError);
    }

    @Override
    public String getMessage() {
        return getErrorMessage() + "\n[ErrorCode]: " + getErrorCode() + "\n[RequestId]: " + getRequestId()
                + "\n[HostId]: " + getHostId() + formatRawResponseError();
    }
}
