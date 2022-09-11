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

package org.apache.inlong.audit.consts;

public class ConfigConstants {

    public static final String CONFIG_PORT = "port";

    public static final String CONFIG_HOST = "host";

    public static final String MSG_FACTORY_NAME = "msg-factory-name";

    public static final String SERVICE_PROCESSOR_NAME = "service-decoder-name";

    public static final String MESSAGE_HANDLER_NAME = "message-handler-name";

    public static final String MAX_MSG_LENGTH = "max-msg-length";

    public static final String CUSTOM_CHANNEL_PROCESSOR = "custom-cp";

    public static final String TCP_NO_DELAY = "tcpNoDelay";

    public static final String KEEP_ALIVE = "keepAlive";

    public static final String HIGH_WATER_MARK = "highWaterMark";

    public static final String RECEIVE_BUFFER_SIZE = "receiveBufferSize";

    public static final String SEND_BUFFER_SIZE = "sendBufferSize";

    public static final String TRAFFIC_CLASS = "trafficClass";

    public static final String MAX_THREADS = "max-threads";

    public static final int MSG_MAX_LENGTH_BYTES = 20 * 1024 * 1024;

    public static final String LINK_MAX_ALLOWED_DELAYED_MSG_COUNT = "link_max_allowed_delayed_msg_count";

    public static final String SESSION_WARN_DELAYED_MSG_COUNT = "session_warn_delayed_msg_count";

    public static final String SESSION_MAX_ALLOWED_DELAYED_MSG_COUNT = "session_max_allowed_delayed_msg_count";

    public static final String NETTY_WRITE_BUFFER_HIGH_WATER_MARK = "netty_write_buffer_high_water_mark";

    public static final String RECOVER_THREAD_COUNT = "recover_thread_count";

    public static final long DEFAULT_LINK_MAX_ALLOWED_DELAYED_MSG_COUNT = 80000L;

    public static final long DEFAULT_SESSION_WARN_DELAYED_MSG_COUNT = 2000000L;

    public static final long DEFAULT_SESSION_MAX_ALLOWED_DELAYED_MSG_COUNT = 4000000L;

    public static final long DEFAULT_NETTY_WRITE_BUFFER_HIGH_WATER_MARK = 15 * 1024 * 1024L;
}
