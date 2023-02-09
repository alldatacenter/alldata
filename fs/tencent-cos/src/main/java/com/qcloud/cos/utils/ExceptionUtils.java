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

package com.qcloud.cos.utils;

import com.qcloud.cos.exception.ClientExceptionConstants;
import com.qcloud.cos.exception.CosClientException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class ExceptionUtils {

    public static CosClientException createClientException(IOException ex) {
        String errorCode = ClientExceptionConstants.UNKNOWN;
        if (ex instanceof ConnectTimeoutException) {
            errorCode = ClientExceptionConstants.CONNECTION_TIMEOUT;
        } else if (ex instanceof UnknownHostException) {
            errorCode = ClientExceptionConstants.UNKNOWN_HOST;
        } else if (ex instanceof HttpHostConnectException) {
            errorCode = ClientExceptionConstants.HOST_CONNECT;
        } else if (ex instanceof SocketTimeoutException) {
            errorCode = ClientExceptionConstants.SOCKET_TIMEOUT;
        } else if(ex instanceof ClientProtocolException) {
            errorCode = ClientExceptionConstants.CLIENT_PROTOCAL_EXCEPTION;
        }

        return new CosClientException(ex.getMessage(), errorCode, ex);
    }
}
