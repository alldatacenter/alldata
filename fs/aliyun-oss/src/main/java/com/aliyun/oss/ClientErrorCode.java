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

public interface ClientErrorCode {

    /**
     * Unknown error. This means the error is not expected.
     */
    static final String UNKNOWN = "Unknown";

    /**
     * Unknown host. This error is returned when a
     * {@link java.net.UnknownHostException} is thrown.
     */
    static final String UNKNOWN_HOST = "UnknownHost";

    /**
     * connection times out.
     */
    static final String CONNECTION_TIMEOUT = "ConnectionTimeout";

    /**
     * Socket times out
     */
    static final String SOCKET_TIMEOUT = "SocketTimeout";

    /**
     * Socket exception
     */
    static final String SOCKET_EXCEPTION = "SocketException";

    /**
     * Connection is refused by server side.
     */
    static final String CONNECTION_REFUSED = "ConnectionRefused";

    /**
     * The input stream is not repeatable for reading.
     */
    static final String NONREPEATABLE_REQUEST = "NonRepeatableRequest";
    
    /**
     * Thread interrupted while reading the input stream.
     */
    static final String INPUTSTREAM_READING_ABORTED = "InputStreamReadingAborted";

    /**
     * Ssl exception
     */
    static final String SSL_EXCEPTION = "SslException";
}
