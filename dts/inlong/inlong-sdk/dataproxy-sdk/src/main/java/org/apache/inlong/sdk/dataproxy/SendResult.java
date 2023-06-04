/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.dataproxy;

public enum SendResult {
    INVALID_ATTRIBUTES, // including DataProxyErrCode(100,101,102,112)
    OK,
    TIMEOUT,
    CONNECTION_BREAK,
    THREAD_INTERRUPT,
    ASYNC_CALLBACK_BUFFER_FULL,
    NO_CONNECTION,
    INVALID_DATA, // including DataProxyErrCode(103, 111)
    BODY_EXCEED_MAX_LEN, // DataProxyErrCode(104)
    SINK_SERVICE_UNREADY, // DataProxyErrCode(1)
    UNCONFIGURED_GROUPID_OR_STREAMID, // DataProxyErrCode(113)
    TOPIC_IS_BLANK, // DataProxyErrCode(115)
    DATAPROXY_FAIL_TO_RECEIVE, // DataProxyErrCode(114,116,117,118,119,120)

    UNKOWN_ERROR
}
