/*
 * Copyright 2010-2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.qcloud.cos.retry;

import com.qcloud.cos.exception.ClientExceptionConstants;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import org.apache.http.HttpStatus;

import java.util.HashSet;
import java.util.Set;

public class RetryUtils {

    static final Set<Integer> RETRYABLE_STATUS_CODES = new HashSet<Integer>(4);
    static final Set<String> RETRYABLE_CLIENT_ERROR_CODES = new HashSet<>(1);

    static {
        RETRYABLE_STATUS_CODES.add(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        RETRYABLE_STATUS_CODES.add(HttpStatus.SC_BAD_GATEWAY);
        RETRYABLE_STATUS_CODES.add(HttpStatus.SC_SERVICE_UNAVAILABLE);
        RETRYABLE_STATUS_CODES.add(HttpStatus.SC_GATEWAY_TIMEOUT);

        RETRYABLE_CLIENT_ERROR_CODES.add(ClientExceptionConstants.CONNECTION_TIMEOUT);
        RETRYABLE_CLIENT_ERROR_CODES.add(ClientExceptionConstants.HOST_CONNECT);
        RETRYABLE_CLIENT_ERROR_CODES.add(ClientExceptionConstants.UNKNOWN_HOST);
        RETRYABLE_CLIENT_ERROR_CODES.add(ClientExceptionConstants.SOCKET_TIMEOUT);
        RETRYABLE_CLIENT_ERROR_CODES.add(ClientExceptionConstants.CLIENT_PROTOCAL_EXCEPTION);
    }

    /**
     * Returns true if the specified exception is a retryable service side exception.
     *
     * @param exception The exception to test.
     * @return True if the exception resulted from a retryable service error, otherwise false.
     */
    public static boolean isRetryableServiceException(Exception exception) {
        return exception instanceof CosServiceException && isRetryableServiceException((CosServiceException) exception);
    }

    /**
     * Returns true if the specified exception is a retryable service side exception.
     *
     * @param exception The exception to test.
     * @return True if the exception resulted from a retryable service error, otherwise false.
     */
    public static boolean isRetryableServiceException(CosServiceException exception) {
        return RETRYABLE_STATUS_CODES.contains(exception.getStatusCode());
    }

    /**
     * Returns true if the specified exception is a retryable service side exception.
     *
     * @param exception The exception to test.
     * @return True if the exception resulted from a retryable service error, otherwise false.
     */
    public static boolean isRetryableClientException(Exception exception) {
        return exception instanceof CosClientException && isRetryableClientException((CosClientException) exception);
    }

    /**
     * Returns true if the specified exception is a retryable service side exception.
     *
     * @param exception The exception to test.
     * @return True if the exception resulted from a retryable service error, otherwise false.
     */
    public static boolean isRetryableClientException(CosClientException exception) {
        return RETRYABLE_CLIENT_ERROR_CODES.contains(exception.getErrorCode());
    }

}
