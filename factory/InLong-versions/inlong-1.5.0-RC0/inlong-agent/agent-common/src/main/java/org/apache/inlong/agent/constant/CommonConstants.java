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

package org.apache.inlong.agent.constant;

import org.apache.inlong.agent.utils.AgentUtils;

/**
 * Common constants.
 */
public class CommonConstants {

    public static final String PROXY_NET_TAG = "proxy.net.tag";
    public static final String DEFAULT_PROXY_NET_TAG = "";

    public static final String PROXY_INLONG_GROUP_ID = "proxy.inlongGroupId";
    public static final String DEFAULT_PROXY_INLONG_GROUP_ID = "default_inlong_group_id";
    public static final String POSITION_SUFFIX = ".position";

    public static final String PROXY_INLONG_STREAM_ID = "proxy.inlongStreamId";
    public static final String DEFAULT_PROXY_INLONG_STREAM_ID = "default_inlong_stream_id";

    public static final String PROXY_LOCAL_HOST = "proxy.localHost";
    public static final String DEFAULT_PROXY_LOCALHOST = AgentUtils.getLocalIp();

    public static final String PROXY_IS_LOCAL_VISIT = "proxy.isLocalVisit";
    public static final boolean DEFAULT_PROXY_IS_LOCAL_VISIT = true;

    public static final String PROXY_TOTAL_ASYNC_PROXY_SIZE = "proxy.total.async.proxy.size";
    public static final int DEFAULT_PROXY_TOTAL_ASYNC_PROXY_SIZE = 200 * 1024 * 1024;

    public static final String PROXY_ALIVE_CONNECTION_NUM = "proxy.alive.connection.num";
    public static final int DEFAULT_PROXY_ALIVE_CONNECTION_NUM = 10;

    public static final String PROXY_MSG_TYPE = "proxy.msgType";
    public static final int DEFAULT_PROXY_MSG_TYPE = 7;

    public static final String PROXY_IS_COMPRESS = "proxy.is.compress";
    public static final boolean DEFAULT_PROXY_IS_COMPRESS = true;

    public static final String PROXY_MAX_SENDER_PER_GROUP = "proxy.max.sender.per.group";
    public static final int DEFAULT_PROXY_MAX_SENDER_PER_GROUP = 10;

    // max size of message list
    public static final String PROXY_PACKAGE_MAX_SIZE = "proxy.package.maxSize";

    // determine if the send method is sync or async
    public static final String PROXY_SEND_SYNC = "proxy.sync";

    // the same task must have the same Partition Key if choose sync
    public static final String PROXY_SEND_PARTITION_KEY = "proxy.partitionKey";

    // max size of single batch in bytes, default is 200KB.
    public static final int DEFAULT_PROXY_PACKAGE_MAX_SIZE = 200000;

    public static final String PROXY_MESSAGE_SEMAPHORE = "proxy.semaphore";
    public static final int DEFAULT_PROXY_MESSAGE_SEMAPHORE = 20000;

    public static final String PROXY_INLONG_STREAM_ID_QUEUE_MAX_NUMBER = "proxy.group.queue.maxNumber";
    public static final int DEFAULT_PROXY_INLONG_STREAM_ID_QUEUE_MAX_NUMBER = 10000;

    public static final String PROXY_PACKAGE_MAX_TIMEOUT_MS = "proxy.package.maxTimeout.ms";
    public static final int DEFAULT_PROXY_PACKAGE_MAX_TIMEOUT_MS = 4 * 1000;

    public static final String PROXY_BATCH_FLUSH_INTERVAL = "proxy.batch.flush.interval";
    public static final int DEFAULT_PROXY_BATCH_FLUSH_INTERVAL = 1000;

    public static final String PROXY_SENDER_MAX_TIMEOUT = "proxy.sender.maxTimeout";
    // max timeout in seconds.
    public static final int DEFAULT_PROXY_SENDER_MAX_TIMEOUT = 20;

    public static final String PROXY_SENDER_MAX_RETRY = "proxy.sender.maxRetry";
    public static final int DEFAULT_PROXY_SENDER_MAX_RETRY = 5;

    public static final String PROXY_IS_FILE = "proxy.isFile";
    public static final boolean DEFAULT_IS_FILE = false;

    public static final String PROXY_CLIENT_IO_THREAD_NUM = "client.iothread.num";
    public static final int DEFAULT_PROXY_CLIENT_IO_THREAD_NUM =
            Runtime.getRuntime().availableProcessors();

    public static final String PROXY_CLIENT_ENABLE_BUSY_WAIT = "client.enable.busy.wait";
    public static final boolean DEFAULT_PROXY_CLIENT_ENABLE_BUSY_WAIT = false;

    public static final String PROXY_RETRY_SLEEP = "proxy.retry.sleep";
    public static final long DEFAULT_PROXY_RETRY_SLEEP = 500;

    public static final String FIELD_SPLITTER = "proxy.field.splitter";
    public static final String DEFAULT_FIELD_SPLITTER = "|";

    public static final String PROXY_KEY_GROUP_ID = "inlongGroupId";
    public static final String PROXY_KEY_STREAM_ID = "inlongStreamId";
    public static final String PROXY_KEY_DATA = "dataKey";
    public static final String PROXY_KEY_ID = "id";
    public static final String PROXY_KEY_AGENT_IP = "agentip";
    public static final String PROXY_OCEANUS_F = "f";
    public static final String PROXY_OCEANUS_BL = "bl";

    // config for pulsar
    // pulsar host port like http://host1:port1
    public static final String PULSAR_SERVERS = "pulsar.servers";
    // pulsar topic name
    public static final String PULSAR_TOPIC = "pulsar.topic";
    // whether async sending data
    public static final String PULSAR_PRODUCER_ASYNC = "pulsar.producer.async";
    public static final boolean DEFAULT_PULSAR_PRODUCER_ASYNC = true;

    public static final String PULSAR_PRODUCER_MAX_PENDING_COUNT = "pulsar.producer.maxPending.count";
    public static final int DEFAULT_PULSAR_PRODUCER_MAX_PENDING_COUNT = 10000;

    public static final String PULSAR_PRODUCER_THREAD_NUM = "pulsar.producer.thread.num";
    public static final int DEFAULT_PULSAR_PRODUCER_THREAD_NUM = 1;

    public static final String PULSAR_PRODUCER_ENABLE_BATCH = "pulsar.producer.enable.batch";
    public static final boolean DEFAULT_PULSAR_PRODUCER_ENABLE_BATCH = true;

    public static final String PULSAR_SINK_POLL_TIMEOUT = "pulsar.sink.poll.timeout";
    // time in ms
    public static final long DEFAULT_PULSAR_SINK_POLL_TIMEOUT = 1000;

    public static final String PULSAR_SINK_CACHE_CAPACITY = "pulsar.sink.cache.capacity";
    public static final int DEFAULT_PULSAR_SINK_CACHE_CAPACITY = 100000;

    public static final String PULSAR_PRODUCER_COMPRESS_TYPE = "pulsar.producer.compress.type";
    public static final String DEFAULT_PULSAR_PRODUCER_COMPRESS_TYPE = "snappy";

    public static final String PULSAR_PRODUCER_BATCH_MAXSIZE = "pulsar.producer.batch.maxsize";
    public static final int DEFAULT_PULSAR_PRODUCER_BATCH_MAXSIZE = 1024 * 1024;

    public static final String PULSAR_PRODUCER_BATCH_MAXCOUNT = "pulsar.producer.batch.maxcount";
    public static final int DEFAULT_PULSAR_PRODUCER_BATCH_MAXCOUNT = 1000;

    public static final String PULSAR_PRODUCER_BLOCK_QUEUE = "pulsar.producer.block.queue";
    public static final boolean DEFAULT_PULSAR_PRODUCER_BLOCK_QUEUE = true;

    public static final String FILE_MAX_NUM = "file.max.num";
    public static final int DEFAULT_FILE_MAX_NUM = 4096;

    public static final String TRIGGER_ID_PREFIX = "trigger_";

    public static final String COMMAND_STORE_INSTANCE_NAME = "commandStore";

    public static final String AGENT_OS_NAME = "os.name";
    public static final String AGENT_NIX_OS = "nix";
    public static final String AGENT_NUX_OS = "nux";
    public static final String AGENT_COLON = ":";

    public static final Integer DEFAULT_MAP_CAPACITY = 16;

    public static final String KEY_METRICS_INDEX = "metricsIndex";

    public static final String COMMA = ",";
    public static final String DELIMITER_UNDERLINE = "_";
    public static final String DELIMITER_HYPHEN = "-";

}
