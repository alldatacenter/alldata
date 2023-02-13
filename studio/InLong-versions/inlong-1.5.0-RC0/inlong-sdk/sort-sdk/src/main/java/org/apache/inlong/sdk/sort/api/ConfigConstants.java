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

package org.apache.inlong.sdk.sort.api;

/**
 * SortSdk config constants
 */
public class ConfigConstants {

    public static final String CALLBACK_QUEUE_SIZE = "callbackQueueSize";
    public static final String PULSAR_RECEIVE_QUEUE_SIZE = "pulsarReceiveQueueSize";
    public static final String STATS_INTERVAL_SECONDS = "statsIntervalSeconds";
    public static final String KAFKA_FETCH_WAIT_MS = "kafkaFetchWaitMs";
    public static final String KAFKA_FETCH_SIZE_BYTES = "kafkaFetchSizeBytes";
    public static final String KAFKA_SOCKET_RECV_BUFFER_SIZE = "kafkaSocketRecvBufferSize";
    public static final String LOCAL_IP = "localIp";
    public static final String APP_NAME = "appName";
    public static final String SERVER_NAME = "serverName";
    public static final String CONTAINER_ID = "containerId";
    public static final String INSTANCE_NAME = "instanceName";
    public static final String ENV = "env";
    public static final String MANAGER_API_URL = "managerApiUrl";
    public static final String MANAGER_API_VERSION = "managerApiVersion";
    public static final String CONSUME_STRATEGY = "consumeStrategy";
    public static final String TOPIC_MANAGER_TYPE = "topicManagerType";
    public static final String REPORT_STATISTIC_INTERVAL_SEC = "reportStatisticIntervalSec";
    public static final String UPDATE_META_DATA_INTERVAL_SEC = "updateMetaDataIntervalSec";
    public static final String ACK_TIMEOUT_SEC = "ackTimeoutSec";
    public static final String CLEAN_OLD_CONSUMER_INTERVAL_SEC = "cleanOldConsumerIntervalSec";
    public static final String IS_PROMETHEUS_ENABLED = "isPrometheusEnabled";
    public static final String EMPTY_POLL_SLEEP_STEP_MS = "emptyPollSleepStepMs";
    public static final String MAX_EMPTY_POLL_SLEEP_MS = "maxEmptyPollSleepMs";
    public static final String EMPTY_POLL_TIMES = "emptyPollTimes";
    public static final String MAX_CONSUMER_SIZE = "maxConsumerSize";

    public static final String CONSUMER_SUBSET_TYPE = "consumerSubsetType";
    public static final String CONSUMER_SUBSET_SIZE = "consumerSubsetSize";

    public static final String IS_TOPIC_STATICS_ENABLED = "isTopicStaticsEnabled";
    public static final String IS_PARTITION_STATICS_ENABLED = "isPartitionStaticsEnabled";

}
