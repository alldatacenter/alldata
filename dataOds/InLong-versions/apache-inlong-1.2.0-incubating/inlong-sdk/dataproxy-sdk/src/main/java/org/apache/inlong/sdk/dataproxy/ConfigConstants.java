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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.dataproxy;

import java.util.concurrent.TimeUnit;

public class ConfigConstants {
    public static final String PROXY_SDK_VERSION = "1.2.11";

    public static final int ALIVE_CONNECTIONS = 3;
    public static final int MAX_TIMEOUT_CNT = 3;
    public static final int LOAD_THRESHOLD = 0;
    public static final int CYCLE = 30;

    public static final int MSG_TYPE = 7;
    public static final int COMPRESS_SIZE = 120;

    /* Configure the thread pool size for sync message sending. */
    public static final int SYNC_THREAD_POOL_SIZE = 5;
    public static final int MAX_SYNC_THREAD_POOL_SIZE = 10;

    /* Configure the in-memory callback size for asynchronously message sending. */
    public static final int ASYNC_CALLBACK_SIZE = 50000;
    public static final int MAX_ASYNC_CALLBACK_SIZE = 2000000;

    /* Configure the proxy IP list refresh parameters. */
    public static final int PROXY_UPDATE_INTERVAL_MINUTES = 5;

    /* one hour interval */
    public static final int PROXY_HTTP_UPDATE_INTERVAL_MINUTES = 60;

    public static final int PROXY_UPDATE_MAX_RETRY = 10;

    public static final int MAX_LINE_CNT = 30;

    /* Default connection,connect timeout in milliseconds.*/
    public static final long DEFAULT_CONNECT_TIMEOUT_MILLIS =
            TimeUnit.MILLISECONDS.convert(40, TimeUnit.SECONDS);

    public static final long DEFAULT_REQUEST_TIMEOUT_MILLIS =
            TimeUnit.MILLISECONDS.convert(40, TimeUnit.SECONDS);

    public static final int DEFAULT_SEND_BUFFER_SIZE = 16777216;
    public static final int DEFAULT_RECEIVE_BUFFER_SIZE = 16777216;

    public static final String RECEIVE_BUFFER_SIZE = "receiveBufferSize";
    public static final String SEND_BUFFER_SIZE = "sendBufferSize";

    public static final String REQUEST_HEADER_AUTHORIZATION = "Authorization";
    public static final int FLAG_ALLOW_AUTH = 1 << 7;
    public static final int FLAG_ALLOW_ENCRYPT = 1 << 6;
    public static final int FLAG_ALLOW_COMPRESS = 1 << 5;

}
