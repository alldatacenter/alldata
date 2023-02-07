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

package org.apache.inlong.dataproxy.consts;

public class ConfigConstants {

    public static final String COMPONENT_NAME = "Inlong-DataProxy";

    public static final String CONFIG_PORT = "port";

    public static final String CONFIG_HOST = "host";

    public static final String MSG_FACTORY_NAME = "msg-factory-name";

    public static final String SERVICE_PROCESSOR_NAME = "service-decoder-name";

    public static final String MESSAGE_HANDLER_NAME = "message-handler-name";

    public static final String MAX_MSG_LENGTH = "max-msg-length";

    public static final String MSG_COMPRESSED = "msg-compressed";

    public static final String TOPIC = "topic";

    public static final String ATTR = "attr";

    public static final String FILTER_EMPTY_MSG = "filter-empty-msg";

    public static final String TCP_NO_DELAY = "tcpNoDelay";

    public static final String KEEP_ALIVE = "keepAlive";

    public static final String HIGH_WATER_MARK = "highWaterMark";

    public static final String RECEIVE_BUFFER_SIZE = "receiveBufferSize";
    public static final String ENABLE_EXCEPTION_RETURN = "enableExceptionReturn";

    public static final String SEND_BUFFER_SIZE = "sendBufferSize";

    public static final String TRAFFIC_CLASS = "trafficClass";

    public static final String MAX_THREADS = "max-threads";

    public static final String ENABLE_BUSY_WAIT = "enableBusyWait";

    public static final String STAT_INTERVAL_SEC = "stat-interval-sec";

    public static final String MAX_MONITOR_CNT = "max-monitor-cnt";
    public static final int DEF_MONITOR_STAT_CNT = 300000;

    public static final String HEART_INTERVAL_SEC = "heart-interval-sec";

    public static final String PACKAGE_TIMEOUT_SEC = "package-timeout-sec";

    public static final String HEART_SERVERS = "heart-servers";

    public static final String CUSTOM_CHANNEL_PROCESSOR = "custom-cp";

    public static final String UDP_PROTOCOL = "udp";
    public static final String TCP_PROTOCOL = "tcp";

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
    public static final String MANAGER_HOST = "manager.hosts";
    public static final String PROXY_CLUSTER_NAME = "proxy.cluster.name";
    public static final String DEFAULT_PROXY_CLUSTER_NAME = "DataProxy";
    public static final String PROXY_CLUSTER_TAG = "proxy.cluster.tag";
    public static final String PROXY_CLUSTER_EXT_TAG = "proxy.cluster.extTag";
    public static final String PROXY_CLUSTER_INCHARGES = "proxy.cluster.inCharges";
    public static final String CONFIG_CHECK_INTERVAL = "configCheckInterval";
    public static final String SOURCE_NO_TOPIC_ACCEPT = "source.topic.notfound.accept";
    public static final String SINK_NO_TOPIC_RESEND = "sink.topic.notfound.resend";

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
