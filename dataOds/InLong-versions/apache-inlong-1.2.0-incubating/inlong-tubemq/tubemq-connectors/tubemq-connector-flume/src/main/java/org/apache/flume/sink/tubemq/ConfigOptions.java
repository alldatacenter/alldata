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

package org.apache.flume.sink.tubemq;

/**
 * Flume configuration options for tubemq sink
 */
public class ConfigOptions {

    // master host of tube mq cluster
    public static final String MASTER_HOST_PORT_LIST = "master-host-port-list";

    // topic name
    public static final String TOPIC = "topic";

    public static final String HEARTBEAT_PERIOD = "heartbeat-period"; // in milliseconds
    public static final long DEFAULT_HEARTBEAT_PERIOD = 15000L;

    public static final String RPC_TIMEOUT = "rpc-timeout";
    public static final long DEFAULT_RPC_TIMEOUT = 20000L;

    public static final String LINK_MAX_ALLOWED_DELAYED_MSG_COUNT = "link-max-allowed-delayed-msg-count";
    public static final long DEFAULT_LINK_MAX_ALLOWED_DELAYED_MSG_COUNT = 80000L;

    public static final String SESSION_WARN_DELAYED_MSG_COUNT = "session-warn-delayed-msg-count";
    public static final long DEFAULT_SESSION_WARN_DELAYED_MSG_COUNT = 2000000L;

    public static final String SESSION_MAX_ALLOWED_DELAYED_MSG_COUNT = "session-max-allowed-delayed-msg-count";
    public static final long DEFAULT_SESSION_MAX_ALLOWED_DELAYED_MSG_COUNT = 4000000L;

    public static final String NETTY_WRITE_BUFFER_HIGH_WATER_MARK = "netty-write-buffer-high-water-mark";
    public static final long DEFAULT_NETTY_WRITE_BUFFER_HIGH_WATER_MARK = 15 * 1024 * 1024L;

    public static final String SINK_THREAD_NUM = "thread-num";
    public static final int DEFAULT_SINK_THREAD_NUM = 4;

    public static final String RETRY_QUEUE_CAPACITY = "retry-queue-capacity";
    public static final int DEFAULT_RETRY_QUEUE_CAPACITY = 10000;

    public static final String EVENT_QUEUE_CAPACITY = "retry-queue-capacity";
    public static final int DEFAULT_EVENT_QUEUE_CAPACITY = 1000;

    public static final String EVENT_MAX_RETRY_TIME = "event-max-retry-time";
    public static final int DEFAULT_EVENT_MAX_RETRY_TIME = 5;

    public static final String EVENT_OFFER_TIMEOUT = "event-offer-timeout";
    public static final long DEFAULT_EVENT_OFFER_TIMEOUT = 3 * 1000; // in milliseconds

}
