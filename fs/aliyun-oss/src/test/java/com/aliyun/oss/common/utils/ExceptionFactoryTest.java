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

package com.aliyun.oss.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import javax.net.ssl.SSLException;

import com.aliyun.oss.OSSException;
import com.aliyun.oss.internal.model.OSSErrorResult;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.NonRepeatableRequestException;
import org.apache.http.conn.ConnectTimeoutException;
import org.junit.Test;

import com.aliyun.oss.ClientErrorCode;
import com.aliyun.oss.ClientException;

import javax.net.ssl.SSLException;

public class ExceptionFactoryTest {

    @Test
    public void testCreateNetworkException() {
        SocketTimeoutException ste = new SocketTimeoutException();
        ClientException ex = ExceptionFactory.createNetworkException(ste);
        assertEquals(ex.getErrorCode(), ClientErrorCode.SOCKET_TIMEOUT);

        SocketException se = new SocketException();
        ex = ExceptionFactory.createNetworkException(se);
        assertEquals(ex.getErrorCode(), ClientErrorCode.SOCKET_EXCEPTION);

        ConnectTimeoutException cte = new ConnectTimeoutException();
        ex = ExceptionFactory.createNetworkException(cte);
        assertEquals(ex.getErrorCode(), ClientErrorCode.CONNECTION_TIMEOUT);

        SSLException slle = new SSLException("");
        ex = ExceptionFactory.createNetworkException(slle);
        assertEquals(ex.getErrorCode(), ClientErrorCode.SSL_EXCEPTION);

        IOException ioe = new IOException();
        ex = ExceptionFactory.createNetworkException(ioe);
        assertEquals(ex.getErrorCode(), ClientErrorCode.UNKNOWN);

        ClientProtocolException cpe = new ClientProtocolException(new NonRepeatableRequestException());
        ex = ExceptionFactory.createNetworkException(cpe);
        assertEquals(ex.getErrorCode(), ClientErrorCode.NONREPEATABLE_REQUEST);

        OSSException ose = ExceptionFactory.createInvalidResponseException("request id", new ConnectTimeoutException());
        assertNotNull(ose);
        ose = ExceptionFactory.createInvalidResponseException("request id", new ConnectTimeoutException());
        assertNotNull(ose);
        ose = ExceptionFactory.createOSSException(new OSSErrorResult());
        assertNotNull(ose);
    }
}
