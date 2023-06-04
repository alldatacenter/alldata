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

package org.apache.inlong.dataproxy.config.pojo;

import org.apache.flume.Context;
import org.apache.pulsar.shade.io.netty.util.NettyRuntime;
import org.apache.pulsar.shade.io.netty.util.internal.SystemPropertyUtil;

import java.util.HashMap;
import java.util.Map;

public class MQClusterConfig extends Context {

    private static final String SEND_REMOTE = "send_remote";
    private static final boolean DEFAULT_SEND_REMOTE = false;
    private static final String SEND_TIMEOUT = "send_timeout_mill";
    private static final int DEFAULT_SEND_TIMEOUT_MILL = 30 * 1000;

    private static final String CLIENT_ID_CACHE = "client_id_cache";
    private static final boolean DEFAULT_CLIENT_ID_CACHE = true;
    private static final String CLIENT_TIMEOUT = "client_timeout_second";
    private static final int DEFAULT_CLIENT_TIMEOUT_SECOND = 30;

    private static final String ENABLE_BATCH = "enable_batch";
    private static final boolean DEFAULT_ENABLE_BATCH = true;

    private static final String BLOCK_IF_QUEUE_FULL = "block_if_queue_full";
    private static final boolean DEFAULT_BLOCK_IF_QUEUE_FULL = true;

    private static final String MAX_PENDING_MESSAGES = "max_pending_messages";
    private static final int DEFAULT_MAX_PENDING_MESSAGES = 10000;

    private static final String MAX_PENDING_MESSAGES_ACROSS_PARTITIONS = "max_pending_messages_across_partitions";
    private static final int DEFAULT_MAX_PENDING_MESSAGES_ACROSS_PARTITIONS = 500000;

    private static final String COMPRESSION_TYPE = "compression_type";
    private static final String DEFAULT_COMPRESSION_TYPE = "NONE";

    private static final String MAX_BATCHING_MESSAGES = "max_batching_messages";
    private static final int DEFAULT_MAX_BATCHING_MESSAGES = 1000;

    private static final String MAX_BATCHING_BYTES = "max_batching_bytes";
    private static final int DEFAULT_MAX_BATCHING_BYTES = 128 * 1024;

    private static final String MAX_BATCHING_PUBLISH_DELAY_MILLIS = "max_batching_publish_delay_millis";
    private static final long DEFAULT_MAX_BATCHING_PUBLISH_DELAY_MILLIS = 1L;

    private static final String RETRY_INTERVAL_WHEN_SEND_ERROR_MILL = "retry_interval_when_send_error_ms";
    private static final long DEFAULT_RETRY_INTERVAL_WHEN_SEND_ERROR_MILL = 30 * 1000L;

    private static final String SLA_METRIC_SINK = "sla_metric_sink";
    private static final boolean DEFAULT_SLA_METRIC_SINK = false;

    // TubeMQ params
    private static final String MAX_SURVIVED_TIME = "max_survived_time";
    private static final int DEFAULT_MAX_SURVIVED_TIME = 30000;

    private static final String MAX_SURVIVED_SIZE = "max_survived_size";
    private static final int DEFAULT_MAX_SURVIVED_SIZE = 5000000;

    private static final String NEW_CHECK_PATTERN = "new_check_pattern";
    private static final boolean DEFAULT_NEW_CHECK_PATTERN = true;

    private static final String OLD_METRIC_ON = "old_metric_on";
    private static final boolean DEFAULT_OLD_METRIC_ON = true;

    private static final String SET_VALUE = "set";
    private static final int DEFAULT_SET_VALUE = 10;

    private static final String MAX_TOPICS_EACH_PRODUCER_HOLD = "max_topic_each_producer_hold";
    private static final int DEFAULT_MAX_TOPICS_EACH_PRODUCER_HOLD = 200;

    private static final String TUBE_REQUEST_TIMEOUT = "tube_request_timeout";
    private static final long DEFAULT_TUBE_REQUEST_TIMEOUT = 20000L;

    private static final String LINK_MAX_ALLOWED_DELAYED_MSG_COUNT = "link_max_allowed_delayed_msg_count";
    private static final long DEFAULT_LINK_MAX_ALLOWED_DELAYED_MSG_COUNT = 80000L;

    private static final String SESSION_WARN_DELAYED_MSG_COUNT = "session_warn_delayed_msg_count";
    private static final long DEFAULT_SESSION_WARN_DELAYED_MSG_COUNT = 2000000L;

    private static final String SESSION_MAX_ALLOWED_DELAYED_MSG_COUNT = "session_max_allowed_delayed_msg_count";
    private static final long DEFAULT_SESSION_MAX_ALLOWED_DELAYED_MSG_COUNT = 4000000L;

    private static final String NETTY_WRITE_BUFFER_HIGH_WATER_MARK = "netty_write_buffer_high_water_mark";
    private static final long DEFAULT_NETTY_WRITE_BUFFER_HIGH_WATER_MARK = 15 * 1024 * 1024L;

    private static final String HEARTBEAT_C2M_PERIOD_MS_MARK = "tube_heartbeat_period_ms";
    private static final long DEFAULT_HEARTBEAT_C2M_PERIOD_MS = 15000L;

    private static final String RECOVER_THREAD_COUNT = "recover_thread_count";
    private static final int DEFAULT_RECOVER_THREAD_COUNT = Runtime.getRuntime().availableProcessors() + 1;

    // log params
    private static final String LOG_TOPIC = "proxy_log_topic";
    private static final String DEFAULT_LOG_TOPIC = "manager";
    private static final String LOG_GROUP_ID = "proxy_log_groupid";
    private static final String DEFAULT_LOG_GROUP_ID = "proxy_measure_log";
    private static final String LOG_STREAM_ID = "proxy_log_streamid";
    private static final String DEFAULT_LOG_STREAM_ID = "manager";
    private static final String LOG_EVERY_N_EVENTS = "log_every_n_events";
    private static final int DEFAULT_LOG_EVERY_N_EVENTS = 100000;

    /*
     * stat params
     */
    private static final String STAT_INTERVAL_SEC = "stat_interval_sec";
    private static final int DEFAULT_STAT_INTERVAL_SEC = 60;

    /*
     * Pulsar params
     */
    private static final String SINK_THREAD_NUM = "thread_num";
    private static final int DEFAULT_THREAD_NUM = 4;
    private static final String DISK_IO_RATE_PER_SEC = "disk_io_rate_per_sec";
    private static final long DEFAULT_DISK_IO_RATE_PER_SEC = 0L;
    private static final String PULSAR_IO_THREADS = "pulsar_io_threads";
    private static final int DEFAULT_PULSAR_IO_THREADS = Math.max(1, SystemPropertyUtil
            .getInt("io.netty.eventLoopThreads", NettyRuntime.availableProcessors() * 2));
    private static final String PULSAR_CONNECTIONS_PRE_BROKER = "connections_pre_broker";
    private static final int DEFAULT_CONNECTIONS_PRE_BROKER = 1;
    private static final String EVENT_QUEUE_SIZE = "event_queue_size";
    private static final int DEFAULT_EVENT_QUEUE_SIZE = 10000;
    private static final String BAD_EVENT_QUEUE_SIZE = "bad_event_queue_size";
    private static final int DEFAULT_BAD_EVENT_QUEUE_SIZE = 10000;
    private static final String MAX_RETRY_SEND_TIMES = "max_retry_send_times";
    private static final int DEFAULT_MAX_RETRY_SEND_TIMES = 16;

    public static String PULSAR_AUTH_TYPE = "pulsar_auth_type";
    public static String PULSAR_DEFAULT_AUTH_TYPE = "token";

    private Map<String, String> url2token = new HashMap<>();

    public long getLinkMaxAllowedDelayedMsgCount() {
        return getLong(LINK_MAX_ALLOWED_DELAYED_MSG_COUNT, DEFAULT_LINK_MAX_ALLOWED_DELAYED_MSG_COUNT);
    }

    public long getSessionWarnDelayedMsgCount() {
        return getLong(SESSION_WARN_DELAYED_MSG_COUNT, DEFAULT_SESSION_WARN_DELAYED_MSG_COUNT);
    }

    public long getSessionMaxAllowedDelayedMsgCount() {
        return getLong(SESSION_MAX_ALLOWED_DELAYED_MSG_COUNT, DEFAULT_SESSION_MAX_ALLOWED_DELAYED_MSG_COUNT);
    }

    public long getNettyWriteBufferHighWaterMark() {
        return getLong(NETTY_WRITE_BUFFER_HIGH_WATER_MARK, DEFAULT_NETTY_WRITE_BUFFER_HIGH_WATER_MARK);
    }

    public long getTubeHeartbeatPeriodMs() {
        return getLong(HEARTBEAT_C2M_PERIOD_MS_MARK, DEFAULT_HEARTBEAT_C2M_PERIOD_MS);
    }

    public int getRecoverThreadCount() {
        return getInteger(RECOVER_THREAD_COUNT, DEFAULT_RECOVER_THREAD_COUNT);
    }

    public int getEventQueueSize() {
        return getInteger(EVENT_QUEUE_SIZE, DEFAULT_EVENT_QUEUE_SIZE);
    }

    public int getBadEventQueueSize() {
        return getInteger(BAD_EVENT_QUEUE_SIZE, DEFAULT_BAD_EVENT_QUEUE_SIZE);
    }

    public Map<String, String> getUrl2token() {
        return url2token;
    }

    public void setUrl2token(Map<String, String> url2token) {
        this.url2token = url2token;
    }

    public String getAuthType() {
        return getString(PULSAR_AUTH_TYPE, PULSAR_DEFAULT_AUTH_TYPE);
    }

    public int getPulsarClientIoThreads() {
        return getInteger(PULSAR_IO_THREADS, DEFAULT_PULSAR_IO_THREADS);
    }

    public int getPulsarConnectionsPreBroker() {
        return getInteger(PULSAR_CONNECTIONS_PRE_BROKER, DEFAULT_CONNECTIONS_PRE_BROKER);
    }

    public int getSendTimeoutMs() {
        return getInteger(SEND_TIMEOUT, DEFAULT_SEND_TIMEOUT_MILL);
    }

    public int getClientTimeoutSecond() {
        return getInteger(CLIENT_TIMEOUT, DEFAULT_CLIENT_TIMEOUT_SECOND);
    }

    public int getMaxBatchingBytes() {
        return getInteger(MAX_BATCHING_BYTES, DEFAULT_MAX_BATCHING_BYTES);
    }

    public long getMaxBatchingPublishDelayMillis() {
        return getLong(MAX_BATCHING_PUBLISH_DELAY_MILLIS, DEFAULT_MAX_BATCHING_PUBLISH_DELAY_MILLIS);
    }

    public boolean getEnableBatch() {
        return getBoolean(ENABLE_BATCH, DEFAULT_ENABLE_BATCH);
    }

    public boolean getBlockIfQueueFull() {
        return getBoolean(BLOCK_IF_QUEUE_FULL, DEFAULT_BLOCK_IF_QUEUE_FULL);
    }

    public int getMaxPendingMessages() {
        return getInteger(MAX_PENDING_MESSAGES, DEFAULT_MAX_PENDING_MESSAGES);
    }

    public int getMaxPendingMessagesAcrossPartitions() {
        return getInteger(MAX_PENDING_MESSAGES_ACROSS_PARTITIONS,
                DEFAULT_MAX_PENDING_MESSAGES_ACROSS_PARTITIONS);
    }

    public String getCompressionType() {
        return getString(COMPRESSION_TYPE, DEFAULT_COMPRESSION_TYPE);
    }

    public int getMaxBatchingMessages() {
        return getInteger(MAX_BATCHING_MESSAGES, DEFAULT_MAX_BATCHING_MESSAGES);
    }

    public long getRetryIntervalWhenSendErrorMs() {
        return getLong(RETRY_INTERVAL_WHEN_SEND_ERROR_MILL, DEFAULT_RETRY_INTERVAL_WHEN_SEND_ERROR_MILL);
    }

    public int getMaxRetryCnt() {
        return getInteger(MAX_RETRY_SEND_TIMES, DEFAULT_MAX_RETRY_SEND_TIMES);
    }

    public int getStatIntervalSec() {
        return getInteger(STAT_INTERVAL_SEC, DEFAULT_STAT_INTERVAL_SEC);
    }

    public int getLogEveryNEvents() {
        return getInteger(LOG_EVERY_N_EVENTS, DEFAULT_LOG_EVERY_N_EVENTS);
    }

    public boolean getClientIdCache() {
        return getBoolean(CLIENT_ID_CACHE, DEFAULT_CLIENT_ID_CACHE);
    }

    public int getThreadNum() {
        return getInteger(SINK_THREAD_NUM, DEFAULT_THREAD_NUM);
    }

    public long getDiskIoRatePerSec() {
        return getLong(DISK_IO_RATE_PER_SEC, DEFAULT_DISK_IO_RATE_PER_SEC);
    }

    public int getMaxSurvivedTime() {
        return getInteger(MAX_SURVIVED_TIME, DEFAULT_MAX_SURVIVED_TIME);
    }

    public int getMaxSurvivedSize() {
        return getInteger(MAX_SURVIVED_SIZE, DEFAULT_MAX_SURVIVED_SIZE);
    }

    public boolean getNewCheckPattern() {
        return getBoolean(NEW_CHECK_PATTERN, DEFAULT_NEW_CHECK_PATTERN);
    }

    public boolean getOldMetricOn() {
        return getBoolean(OLD_METRIC_ON, DEFAULT_OLD_METRIC_ON);
    }

    public int getSetValue() {
        return getInteger(SET_VALUE, DEFAULT_SET_VALUE);
    }

    public int getMaxTopicsEachProducerHold() {
        return getInteger(MAX_TOPICS_EACH_PRODUCER_HOLD, DEFAULT_MAX_TOPICS_EACH_PRODUCER_HOLD);
    }

    public long getTubeRpcTimeoutMs() {
        return getLong(TUBE_REQUEST_TIMEOUT, DEFAULT_TUBE_REQUEST_TIMEOUT);
    }

    public String getLogTopic() {
        return getString(LOG_TOPIC, DEFAULT_LOG_TOPIC);
    }

    public String getLogStreamId() {
        return getString(LOG_STREAM_ID, DEFAULT_LOG_STREAM_ID);
    }

    public String getLogGroupId() {
        return getString(LOG_GROUP_ID, DEFAULT_LOG_GROUP_ID);
    }

    public boolean getEnableSendRemote() {
        return getBoolean(SEND_REMOTE, DEFAULT_SEND_REMOTE);
    }

    public boolean getEnableSlaMetricSink() {
        return getBoolean(SLA_METRIC_SINK, DEFAULT_SLA_METRIC_SINK);
    }
}