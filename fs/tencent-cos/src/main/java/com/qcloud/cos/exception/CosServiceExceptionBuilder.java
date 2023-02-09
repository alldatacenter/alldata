/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.

 * According to cos feature, we modify some class，comment, field name, etc.
 */


package com.qcloud.cos.exception;

import java.util.HashMap;
import java.util.Map;

import com.qcloud.cos.exception.CosServiceException.ErrorType;

public class CosServiceExceptionBuilder {

    /**
     * The unique COS identifier for the service request the caller made. The COS request ID can
     * uniquely identify the COS request.
     */
    private String requestId;

    /**
     * The COS error code represented by this exception (ex: InvalidParameterValue).
     */
    private String errorCode;

    /**
     * The error message as returned by the service.
     */
    private String errorMessage;

    /**
     * The HTTP status code that was returned with this error
     */
    private int statusCode;

    /**
     * An COS specific request ID that provides additional debugging information.
     */
    private String traceId;

    /**
     * Additional information on the exception.
     */
    private Map<String, String> additionalDetails;

    /**
     * Returns the error XML received in the HTTP Response or null if the exception is constructed
     * from the headers.
     */
    private String errorResponseXml;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Returns the human-readable error message provided by the service
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the human-readable error message provided by the service
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Sets the HTTP status code that was returned with this service exception.
     *
     * @param statusCode The HTTP status code that was returned with this service exception.
     */
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * Returns the HTTP status code that was returned with this service exception.
     *
     * @return The HTTP status code that was returned with this service exception.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Gets Qcloud COS's extended request ID. This ID is required debugging information in the case
     *
     * @return Qcloud COS's extended request ID.
     */
    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    /**
     * Returns any additional information retrieved in the error response.
     */
    public Map<String, String> getAdditionalDetails() {
        return additionalDetails;
    }

    /**
     * Sets additional information about the response.
     */
    public void setAdditionalDetails(Map<String, String> additionalDetails) {
        this.additionalDetails = additionalDetails;
    }

    /**
     * Adds an entry to the additional information map.
     */
    public void addAdditionalDetail(String key, String detail) {
        if (detail == null || detail.trim().isEmpty()) {
            return;
        }

        if (this.additionalDetails == null) {
            this.additionalDetails = new HashMap<String, String>();
        }

        String additionalContent = this.additionalDetails.get(key);
        if (additionalContent != null && !additionalContent.trim().isEmpty()) {
            detail = additionalContent + "-" + detail;
        }
        if (!detail.isEmpty()) {
            additionalDetails.put(key, detail);
        }
    }

    /**
     * Returns the original error response XML received from Qcloud COS
     */
    public String getErrorResponseXml() {
        return errorResponseXml;
    }

    /**
     * Sets the error response XML received from Cos
     */
    public void setErrorResponseXml(String errorResponseXml) {
        this.errorResponseXml = errorResponseXml;
    }

    /**
     * Creates a new CosServiceException object with the values set.
     */
    public CosServiceException build() {
        CosServiceException cosException =
                errorResponseXml == null ? new CosServiceException(errorMessage)
                        : new CosServiceException(errorMessage, errorResponseXml);
        cosException.setErrorCode(errorCode);
        cosException.setTraceId(traceId);
        cosException.setStatusCode(statusCode);
        cosException.setRequestId(requestId);
        cosException.setAdditionalDetails(additionalDetails);
        cosException.setErrorType(errorTypeOf(statusCode));
        return cosException;
    }

    /**
     * Returns the COS error type information by looking at the HTTP status code in the error
     * response. COS error responses don't explicitly declare a sender or client fault like other COS
     * services, so we have to use the HTTP status code to infer this information.
     *
     * @param httpResponse The HTTP error response to use to determine the right error type to set.
     */
    private ErrorType errorTypeOf(int statusCode) {
        return statusCode >= 500 ? ErrorType.Service : ErrorType.Client;
    }
}
