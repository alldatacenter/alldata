/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.client.common;

public class TClientConstants {

    public static final int CFG_DEFAULT_REGISTER_RETRY_TIMES = 5;
    public static final int CFG_DEFAULT_HEARTBEAT_RETRY_TIMES = 5;
    public static final long CFG_DEFAULT_HEARTBEAT_PERIOD_MS = 13000;
    public static final long CFG_DEFAULT_REGFAIL_WAIT_PERIOD_MS = 1000;
    public static final long CFG_DEFAULT_MSG_NOTFOUND_WAIT_PERIOD_MS = 400L;
    public static final long CFG_DEFAULT_CONSUME_READ_WAIT_PERIOD_MS = 90000L;
    public static final long CFG_DEFAULT_CONSUME_READ_CHECK_SLICE_MS = 3L;
    public static final long CFG_DEFAULT_PUSH_LISTENER_WAIT_PERIOD_MS = 3000L;
    public static final long CFG_DEFAULT_PULL_REB_CONFIRM_WAIT_PERIOD_MS = 3000L;
    public static final long CFG_DEFAULT_PULL_PROTECT_CONFIRM_WAIT_PERIOD_MS = 60000L;
    public static final long CFG_DEFAULT_SHUTDOWN_REBALANCE_WAIT_PERIOD_MS = 10000L;
    public static final long CFG_DEFAULT_HEARTBEAT_PERIOD_AFTER_RETRY_FAIL = 60000;
    public static final int CFG_DEFAULT_CLIENT_PUSH_FETCH_THREAD_CNT =
            Runtime.getRuntime().availableProcessors();

    public static final int MAX_CONNECTION_FAILURE_LOG_TIMES = 10;
    public static final int MAX_SUBSCRIBE_REPORT_INTERVAL_TIMES = 6;

    public static final long CFG_DEFAULT_META_QUERY_WAIT_PERIOD_MS = 10000L;
    public static final long CFG_MIN_META_QUERY_WAIT_PERIOD_MS = 5000L;
}
