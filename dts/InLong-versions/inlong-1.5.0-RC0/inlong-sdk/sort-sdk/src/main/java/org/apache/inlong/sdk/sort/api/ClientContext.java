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

import org.apache.inlong.common.metric.MetricRegister;
import org.apache.inlong.sdk.sort.entity.InLongTopic;
import org.apache.inlong.sdk.sort.metrics.SortSdkMetricItem;
import org.apache.inlong.sdk.sort.metrics.SortSdkMetricItemSet;

import java.util.HashMap;
import java.util.Map;

public abstract class ClientContext implements Cleanable {

    protected final SortClientConfig config;

    protected final String sortTaskId;

    protected final SortSdkMetricItemSet metricItemSet;

    public ClientContext(SortClientConfig config) {
        this.config = config;
        this.sortTaskId = config.getSortTaskId();
        this.metricItemSet = new SortSdkMetricItemSet(config.getSortTaskId());
        MetricRegister.register(this.metricItemSet);
    }

    public SortClientConfig getConfig() {
        return config;
    }

    @Override
    public boolean clean() {
        return true;
    }

    public void addConsumeTime(InLongTopic topic, int partitionId) {
        SortSdkMetricItem metricItem = this.getMetricItem(topic, partitionId);
        metricItem.consumeTimes.incrementAndGet();
    }

    public void addConsumeSuccess(InLongTopic topic, int partitionId, int size, int count, long time) {
        SortSdkMetricItem metricItem = this.getMetricItem(topic, partitionId);
        metricItem.consumeSize.addAndGet(size);
        metricItem.consumeMsgCount.addAndGet(count);
        metricItem.consumeTimeCost.addAndGet(time);
    }

    public void addConsumeFilter(InLongTopic topic, int partitionId, int count) {
        SortSdkMetricItem metricItem = this.getMetricItem(topic, partitionId);
        metricItem.filterCount.addAndGet(count);
    }

    public void addConsumeEmpty(InLongTopic topic, int partitionId, long time) {
        SortSdkMetricItem metricItem = this.getMetricItem(topic, partitionId);
        metricItem.consumeEmptyCount.incrementAndGet();
        metricItem.consumeTimeCost.addAndGet(time);
    }

    public void addConsumeError(InLongTopic topic, int partitionId, long time) {
        SortSdkMetricItem metricItem = this.getMetricItem(topic, partitionId);
        metricItem.consumeErrorCount.incrementAndGet();
        metricItem.consumeTimeCost.addAndGet(time);
    }

    public void addCallBack(InLongTopic topic, int partitionId) {
        SortSdkMetricItem metricItem = this.getMetricItem(topic, partitionId);
        metricItem.callbackCount.incrementAndGet();
    }

    public void addCallBackSuccess(InLongTopic topic, int partitionId, int count, long time) {
        SortSdkMetricItem metricItem = this.getMetricItem(topic, partitionId);
        metricItem.callbackDoneCount.addAndGet(count);
        metricItem.callbackTimeCost.addAndGet(time);
    }

    public void addCallBackFail(InLongTopic topic, int partitionId, int count, long time) {
        SortSdkMetricItem metricItem = this.getMetricItem(topic, partitionId);
        metricItem.callbackFailCount.addAndGet(count);
        metricItem.callbackTimeCost.addAndGet(time);
    }

    public void addAckSuccess(InLongTopic topic, int partitionId) {
        SortSdkMetricItem metricItem = this.getMetricItem(topic, partitionId);
        metricItem.ackSuccCount.incrementAndGet();
    }

    public void addAckFail(InLongTopic topic, int partitionId) {
        SortSdkMetricItem metricItem = this.getMetricItem(topic, partitionId);
        metricItem.ackFailCount.incrementAndGet();
    }

    public void addTopicOnlineCount(int count) {
        SortSdkMetricItem metricItem = this.getMetricItem(null, -1);
        metricItem.topicOnlineCount.addAndGet(count);
    }

    public void addTopicOfflineCount(int count) {
        SortSdkMetricItem metricItem = this.getMetricItem(null, -1);
        metricItem.topicOfflineCount.addAndGet(count);
    }

    public void addRequestManager() {
        SortSdkMetricItem metricItem = this.getMetricItem(null, -1);
        metricItem.requestManagerCount.incrementAndGet();
    }

    public void addRequestManagerFail(long time) {
        SortSdkMetricItem metricItem = this.getMetricItem(null, -1);
        metricItem.requestManagerFailCount.incrementAndGet();
        metricItem.requestManagerTimeCost.addAndGet(time);
    }

    public void addRequestManagerConfChange() {
        SortSdkMetricItem metricItem = this.getMetricItem(null, -1);
        metricItem.requestManagerConfChangedCount.incrementAndGet();
    }

    public void addRequestManagerCommonError() {
        SortSdkMetricItem metricItem = this.getMetricItem(null, -1);
        metricItem.requestManagerCommonErrorCount.incrementAndGet();
    }

    public void addRequestManagerParamError() {
        SortSdkMetricItem metricItem = this.getMetricItem(null, -1);
        metricItem.requestManagerParamErrorCount.incrementAndGet();
    }

    private SortSdkMetricItem getMetricItem(InLongTopic topic, int partitionId) {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put(SortSdkMetricItem.KEY_SORT_TASK_ID, sortTaskId);
        if (topic != null && config.isTopicStaticsEnabled()) {
            dimensions.put(SortSdkMetricItem.KEY_CLUSTER_ID, topic.getInLongCluster().getClusterId());
            dimensions.put(SortSdkMetricItem.KEY_TOPIC_ID, topic.getTopic());
        }
        if (config.isPartitionStaticsEnabled()) {
            dimensions.put(SortSdkMetricItem.KEY_PARTITION_ID, String.valueOf(partitionId));
        }
        return metricItemSet.findMetricItem(dimensions);
    }

    public void acquireRequestPermit() throws InterruptedException {
        config.getGlobalInProgressRequest().acquireUninterruptibly();
    }

    public void releaseRequestPermit() {
        config.getGlobalInProgressRequest().release();
    }

}
