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

package org.apache.inlong.sdk.sort.metrics;

import java.util.concurrent.atomic.AtomicLong;
import org.apache.inlong.common.metric.CountMetric;
import org.apache.inlong.common.metric.Dimension;
import org.apache.inlong.common.metric.MetricDomain;
import org.apache.inlong.common.metric.MetricItem;

@MetricDomain(name = "SortSdk")
public class SortSdkMetricItem extends MetricItem {

    //Dimension
    public static final String KEY_SORT_TASK_ID = "sortTaskId";
    public static final String KEY_CLUSTER_ID = "clusterId";
    public static final String KEY_TOPIC_ID = "topicId";
    public static final String KEY_PARTITION_ID = "partitionId";

    //CountMetric
    //consume
    public static final String M_CONSUME_SIZE = "consumeSize";
    public static final String M_CONSUME_MSG_COUNT = "consumeMsgCount";
    //callback
    public static final String M_CALL_BACK_COUNT = "callbackCount";
    public static final String M_CALL_BACK_DONE_COUNT = "callbackDoneCount";
    public static final String M_CALL_BACK_TIME_COST = "callbakTimeCost";
    public static final String M_CALL_BACK_FAIL_COUNT = "callbackFailCount";
    //topic chanage
    public static final String M_TOPIC_ONLINE_COUNT = "topicOnlineCount";
    public static final String M_TOPIC_OFFLINE_COUNT = "topicOfflineCount";
    //ack
    public static final String M_ACK_FAIL_COUNT = "ackFailCount";
    public static final String M_ACK_SUCC_COUNT = "ackSUCCCount";
    //request manager
    public static final String M_REQUEST_MANAGER_COUNT = "requestManagerCount";
    public static final String M_REQUEST_MANAGER_TIME_COST = "requestManagerTimeCost";
    public static final String M_REQUEST_MANAGER_FAIL_COUNT = "requestManagerFailCount";
    public static final String M_REQUEST_MANAGER_CONF_CHANAGED_COUNT = "requestManagerConfChanagedCount";
    public static final String M_RQUEST_MANAGER_COMMON_ERROR_COUNT = "requestManagerCommonErrorCount";
    public static final String M_RQUEST_MANAGER_PARAM_ERROR_COUNT = "requestManagerParamErrorCount";

    @Dimension
    public String sortTaskId;
    @Dimension
    public String clusterId;
    @Dimension
    public String topic;
    @Dimension
    public String partitionId;

    @CountMetric
    public AtomicLong consumeSize = new AtomicLong(0);
    @CountMetric
    public AtomicLong consumeMsgCount = new AtomicLong(0);
    @CountMetric
    public AtomicLong callbackCount = new AtomicLong(0);
    @CountMetric
    public AtomicLong callbackDoneCount = new AtomicLong(0);
    @CountMetric
    public AtomicLong callbackTimeCost = new AtomicLong(0);
    @CountMetric
    public AtomicLong callbackFailCount = new AtomicLong(0);
    @CountMetric
    public AtomicLong topicOnlineCount = new AtomicLong(0);
    @CountMetric
    public AtomicLong topicOfflineCount = new AtomicLong(0);
    @CountMetric
    public AtomicLong ackFailCount = new AtomicLong(0);
    @CountMetric
    public AtomicLong ackSuccCount = new AtomicLong(0);
    @CountMetric
    public AtomicLong requestManagerCount = new AtomicLong(0);
    @CountMetric
    public AtomicLong requestManagerTimeCost = new AtomicLong(0);
    @CountMetric
    public AtomicLong requestManagerFailCount = new AtomicLong(0);
    @CountMetric
    public AtomicLong requestManagerConfChangedCount = new AtomicLong(0);
    @CountMetric
    public AtomicLong requestManagerCommonErrorCount = new AtomicLong(0);
    @CountMetric
    public AtomicLong requestManagerParamErrorCount = new AtomicLong(0);

    public SortSdkMetricItem(String sortTaskId) {
        this.sortTaskId = sortTaskId;
    }

}
