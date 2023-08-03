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

package org.apache.inlong.dataproxy.source;

public class SourceConstants {

    // source host
    public static final String SRCCXT_CONFIG_HOST = "host";
    // system env source host
    public static final String SYSENV_HOST_IP = "inlongHostIp";
    // default source host
    public static final String VAL_DEF_HOST_VALUE = "0.0.0.0";
    // source port
    public static final String SRCCXT_CONFIG_PORT = "port";
    // system env source port
    public static final String SYSENV_HOST_PORT = "inlongHostPort";
    // message factory name
    public static final String SRCCXT_MSG_FACTORY_NAME = "msg-factory-name";
    // message handler name
    public static final String SRCCXT_MESSAGE_HANDLER_NAME = "message-handler-name";
    // default attributes
    public static final String SRCCXT_DEF_ATTR = "attr";
    // max message length
    public static final String SRCCXT_MAX_MSG_LENGTH = "max-msg-length";
    // allowed max message length
    public static final int VAL_MAX_MAX_MSG_LENGTH = 20 * 1024 * 1024;
    public static final int VAL_MIN_MAX_MSG_LENGTH = 5;
    public static final int VAL_DEF_MAX_MSG_LENGTH = 1024 * 64;
    // whether compress message
    public static final String SRCCXT_MSG_COMPRESSED = "msg-compressed";
    public static final boolean VAL_DEF_MSG_COMPRESSED = true;
    // whether filter empty message
    public static final String SRCCXT_FILTER_EMPTY_MSG = "filter-empty-msg";
    public static final boolean VAL_DEF_FILTER_EMPTY_MSG = false;
    // whether custom channel processor
    public static final String SRCCXT_CUSTOM_CHANNEL_PROCESSOR = "custom-cp";
    public static final boolean VAL_DEF_CUSTOM_CH_PROCESSOR = false;
    // max net accept process threads
    public static final String SRCCXT_MAX_ACCEPT_THREADS = "max-accept-threads";
    public static final int VAL_DEF_NET_ACCEPT_THREADS = 1;
    public static final int VAL_MIN_ACCEPT_THREADS = 1;
    public static final int VAL_MAX_ACCEPT_THREADS = 10;
    // max net worker process threads
    public static final String SRCCXT_MAX_WORKER_THREADS = "max-threads";
    public static final int VAL_DEF_WORKER_THREADS = Runtime.getRuntime().availableProcessors();
    public static final int VAL_MIN_WORKER_THREADS = 1;
    // max connection count
    public static final String SRCCXT_MAX_CONNECTION_CNT = "connections";
    public static final int VAL_DEF_MAX_CONNECTION_CNT = 5000;
    public static final int VAL_MIN_CONNECTION_CNT = 0;
    // max receive buffer size
    public static final String SRCCXT_RECEIVE_BUFFER_SIZE = "receiveBufferSize";
    public static final int VAL_DEF_RECEIVE_BUFFER_SIZE = 64 * 1024;
    public static final int VAL_MIN_RECEIVE_BUFFER_SIZE = 0;
    // max send buffer size
    public static final String SRCCXT_SEND_BUFFER_SIZE = "sendBufferSize";
    public static final int VAL_DEF_SEND_BUFFER_SIZE = 64 * 1024;
    public static final int VAL_MIN_SEND_BUFFER_SIZE = 0;
    // connect backlog
    public static final String SRCCXT_CONN_BACKLOG = "con-backlog";
    public static final int VAL_DEF_CONN_BACKLOG = 128;
    public static final int VAL_MIN_CONN_BACKLOG = 0;
    // connect linger
    public static final String SRCCXT_CONN_LINGER = "con-linger";
    public static final int VAL_MIN_CONN_LINGER = 0;
    // connect reuse address
    public static final String SRCCXT_REUSE_ADDRESS = "reuse-address";
    public static final boolean VAL_DEF_REUSE_ADDRESS = true;
    // tcp parameter no delay
    public static final String SRCCXT_TCP_NO_DELAY = "tcpNoDelay";
    public static final boolean VAL_DEF_TCP_NO_DELAY = true;
    // tcp parameter keep alive
    public static final String SRCCXT_TCP_KEEP_ALIVE = "keepAlive";
    public static final boolean VAL_DEF_TCP_KEEP_ALIVE = true;
    // tcp parameter high water mark
    public static final String SRCCXT_TCP_HIGH_WATER_MARK = "highWaterMark";
    public static final int VAL_DEF_TCP_HIGH_WATER_MARK = 64 * 1024;
    public static final int VAL_MIN_TCP_HIGH_WATER_MARK = 0;
    // tcp parameter enable busy wait
    public static final String SRCCXT_TCP_ENABLE_BUSY_WAIT = "enableBusyWait";
    public static final boolean VAL_DEF_TCP_ENABLE_BUSY_WAIT = false;
    // tcp parameters max read idle time
    public static final String SRCCXT_MAX_READ_IDLE_TIME_MS = "maxReadIdleTime";
    public static final long VAL_DEF_READ_IDLE_TIME_MS = 3 * 60 * 1000;
    public static final long VAL_MIN_READ_IDLE_TIME_MS = 60 * 1000;
    public static final long VAL_MAX_READ_IDLE_TIME_MS = 70 * 60 * 1000;
    // source protocol type
    public static final String SRC_PROTOCOL_TYPE_TCP = "tcp";
    public static final String SRC_PROTOCOL_TYPE_UDP = "udp";
    public static final String SRC_PROTOCOL_TYPE_HTTP = "http";

    public static final String SERVICE_PROCESSOR_NAME = "service-decoder-name";
    public static final String ENABLE_EXCEPTION_RETURN = "enableExceptionReturn";

    public static final String TRAFFIC_CLASS = "trafficClass";

    public static final String HEART_INTERVAL_SEC = "heart-interval-sec";

    public static final String PACKAGE_TIMEOUT_SEC = "package-timeout-sec";

    public static final String HEART_SERVERS = "heart-servers";

    public static final String TOPIC_KEY = "topic";
    public static final String REMOTE_IP_KEY = "srcIp";
    public static final String DATAPROXY_IP_KEY = "dpIp";
    public static final String MSG_ENCODE_VER = "msgEnType";
    public static final String REMOTE_IDC_KEY = "idc";
    public static final String MSG_COUNTER_KEY = "msgcnt";
    public static final String PKG_COUNTER_KEY = "pkgcnt";
    public static final String PKG_TIME_KEY = "msg.pkg.time";
    public static final String TRANSFER_KEY = "transfer";
    public static final String DEST_IP_KEY = "dstIp";
    public static final String INTERFACE_KEY = "interface";
    public static final String SINK_MIN_METRIC_KEY = "sink-min-metric-topic";
    public static final String SINK_HOUR_METRIC_KEY = "sink-hour-metric-topic";
    public static final String SINK_TEN_METRIC_KEY = "sink-ten-metric-topic";
    public static final String SINK_QUA_METRIC_KEY = "sink-qua-metric-topic";
    public static final String L5_MIN_METRIC_KEY = "l5-min-metric-topic";
    public static final String L5_MIN_FAIL_METRIC_KEY = "l5-min-fail-metric-key";
    public static final String L5_HOUR_METRIC_KEY = "l5-hour-metric-topic";
    public static final String L5_ID_KEY = "l5id";
    public static final String SET_KEY = "set";
    public static final String CLUSTER_ID_KEY = "clusterId";

    public static final String DECODER_BODY = "body";
    public static final String DECODER_TOPICKEY = "topic_key";
    public static final String DECODER_ATTRS = "attrs";
    public static final String MSG_TYPE = "msg_type";
    public static final String COMPRESS_TYPE = "compress_type";
    public static final String EXTRA_ATTR = "extra_attr";
    public static final String COMMON_ATTR_MAP = "common_attr_map";
    public static final String MSG_LIST = "msg_list";
    public static final String VERSION_TYPE = "version";
    public static final String FILE_CHECK_DATA = "file-check-data";
    public static final String MINUTE_CHECK_DATA = "minute-check-data";
    public static final String SLA_METRIC_DATA = "sla-metric-data";
    public static final String SLA_METRIC_GROUPID = "manager_sla_metric";

    public static final String FILE_BODY = "file-body";
    public static final int MSG_MAX_LENGTH_BYTES = 20 * 1024 * 1024;

    public static final String SEQUENCE_ID = "sequencial_id";

    public static final String TOTAL_LEN = "totalLen";

    public static final String LINK_MAX_ALLOWED_DELAYED_MSG_COUNT = "link_max_allowed_delayed_msg_count";
    public static final String SESSION_WARN_DELAYED_MSG_COUNT = "session_warn_delayed_msg_count";
    public static final String SESSION_MAX_ALLOWED_DELAYED_MSG_COUNT = "session_max_allowed_delayed_msg_count";
    public static final String NETTY_WRITE_BUFFER_HIGH_WATER_MARK = "netty_write_buffer_high_water_mark";
    public static final String RECOVER_THREAD_COUNT = "recover_thread_count";

    public static final String MANAGER_PATH = "/inlong/manager/openapi";
    public static final String MANAGER_GET_CONFIG_PATH = "/dataproxy/getConfig";
    public static final String MANAGER_GET_ALL_CONFIG_PATH = "/dataproxy/getAllConfig";
    public static final String MANAGER_HEARTBEAT_REPORT = "/heartbeat/report";

    public static final String MANAGER_AUTH_SECRET_ID = "manager.auth.secretId";
    public static final String MANAGER_AUTH_SECRET_KEY = "manager.auth.secretKey";
    // Pulsar config
    public static final String KEY_TENANT = "tenant";
    public static final String KEY_NAMESPACE = "namespace";

    public static final String KEY_SERVICE_URL = "serviceUrl";
    public static final String KEY_AUTHENTICATION = "authentication";
    public static final String KEY_STATS_INTERVAL_SECONDS = "statsIntervalSeconds";

    public static final String KEY_ENABLEBATCHING = "enableBatching";
    public static final String KEY_BATCHINGMAXBYTES = "batchingMaxBytes";
    public static final String KEY_BATCHINGMAXMESSAGES = "batchingMaxMessages";
    public static final String KEY_BATCHINGMAXPUBLISHDELAY = "batchingMaxPublishDelay";
    public static final String KEY_MAXPENDINGMESSAGES = "maxPendingMessages";
    public static final String KEY_MAXPENDINGMESSAGESACROSSPARTITIONS = "maxPendingMessagesAcrossPartitions";
    public static final String KEY_SENDTIMEOUT = "sendTimeout";
    public static final String KEY_COMPRESSIONTYPE = "compressionType";
    public static final String KEY_BLOCKIFQUEUEFULL = "blockIfQueueFull";
    public static final String KEY_ROUNDROBINROUTERBATCHINGPARTITIONSWITCHFREQUENCY = "roundRobinRouter"
            + "BatchingPartitionSwitchFrequency";

    public static final String KEY_IOTHREADS = "ioThreads";
    public static final String KEY_MEMORYLIMIT = "memoryLimit";
    public static final String KEY_CONNECTIONSPERBROKER = "connectionsPerBroker";
}
