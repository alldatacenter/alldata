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

package org.apache.inlong.agent.plugin.channel;

import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.constant.AgentConstants;
import org.apache.inlong.agent.metrics.AgentMetricItem;
import org.apache.inlong.agent.metrics.AgentMetricItemSet;
import org.apache.inlong.agent.plugin.Channel;
import org.apache.inlong.agent.plugin.Message;
import org.apache.inlong.common.metric.MetricRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.inlong.agent.constant.CommonConstants.DEFAULT_PROXY_INLONG_GROUP_ID;
import static org.apache.inlong.agent.constant.CommonConstants.DEFAULT_PROXY_INLONG_STREAM_ID;
import static org.apache.inlong.agent.constant.CommonConstants.PROXY_INLONG_GROUP_ID;
import static org.apache.inlong.agent.constant.CommonConstants.PROXY_INLONG_STREAM_ID;
import static org.apache.inlong.agent.metrics.AgentMetricItem.KEY_INLONG_GROUP_ID;
import static org.apache.inlong.agent.metrics.AgentMetricItem.KEY_INLONG_STREAM_ID;
import static org.apache.inlong.agent.metrics.AgentMetricItem.KEY_PLUGIN_ID;

/**
 * memory channel
 */
public class MemoryChannel implements Channel {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryChannel.class);

    private LinkedBlockingQueue<Message> queue;
    // metric
    private AgentMetricItemSet metricItemSet;
    private static final AtomicLong METRIC_INDEX = new AtomicLong(0);
    private String inlongGroupId;
    private String inlongStreamId;

    public MemoryChannel() {
    }

    @Override
    public void push(Message message) {
        try {
            if (message != null) {
                AgentMetricItem metricItem = getMetricItem(new HashMap<String, String>());
                metricItem.pluginReadCount.incrementAndGet();
                queue.put(message);
                metricItem.pluginReadSuccessCount.incrementAndGet();
            }
        } catch (InterruptedException ex) {
            this.metricItemReadFailed();
        }
    }

    @Override
    public boolean push(Message message, long timeout, TimeUnit unit) {
        try {
            if (message != null) {
                AgentMetricItem metricItem = getMetricItem(new HashMap<String, String>());
                metricItem.pluginReadCount.incrementAndGet();
                boolean result = queue.offer(message, timeout, unit);
                if (result) {
                    metricItem.pluginReadSuccessCount.incrementAndGet();
                } else {
                    metricItem.pluginReadFailCount.incrementAndGet();
                }
                return result;
            }
        } catch (InterruptedException ex) {
            this.metricItemReadFailed();
        }
        return false;
    }

    @Override
    public Message pull(long timeout, TimeUnit unit) {
        try {
            Message message = queue.poll(timeout, unit);
            if (message != null) {
                AgentMetricItem metricItem = getMetricItem(new HashMap<String, String>());
                metricItem.pluginSendSuccessCount.incrementAndGet();
                metricItem.pluginSendCount.incrementAndGet();
            }
            return message;
        } catch (InterruptedException ex) {
            this.metricItemSendFailed();
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public void init(JobProfile jobConf) {
        inlongGroupId = jobConf.get(PROXY_INLONG_GROUP_ID, DEFAULT_PROXY_INLONG_GROUP_ID);
        inlongStreamId = jobConf.get(PROXY_INLONG_STREAM_ID, DEFAULT_PROXY_INLONG_STREAM_ID);
        queue = new LinkedBlockingQueue<>(
                jobConf.getInt(AgentConstants.CHANNEL_MEMORY_CAPACITY,
                        AgentConstants.DEFAULT_CHANNEL_MEMORY_CAPACITY));
        String metricName = String.join("-", this.getClass().getSimpleName(),
                String.valueOf(METRIC_INDEX.incrementAndGet()));
        this.metricItemSet = new AgentMetricItemSet(metricName);
        MetricRegister.register(metricItemSet);
    }

    @Override
    public void destroy() {
        if (queue != null) {
            queue.clear();
        }
        LOGGER.info("destroy channel, show memory channel metric:");
    }

    private AgentMetricItem getMetricItem(Map<String, String> dimens) {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put(KEY_PLUGIN_ID, this.getClass().getSimpleName());
        dimensions.put(KEY_INLONG_GROUP_ID, inlongGroupId);
        dimensions.put(KEY_INLONG_STREAM_ID, inlongStreamId);
        dimens.forEach((key, value) -> {
            dimensions.put(key, value);
        });
        return this.metricItemSet.findMetricItem(dimensions);
    }

    private void metricItemReadFailed() {
        Map<String, String> dimensions = new HashMap<String, String>();
        dimensions.put(KEY_INLONG_GROUP_ID, inlongGroupId);
        dimensions.put(KEY_INLONG_STREAM_ID, inlongStreamId);
        AgentMetricItem metricItem = getMetricItem(dimensions);
        metricItem.pluginReadFailCount.incrementAndGet();
        LOGGER.debug("plugin read failed:{}", dimensions.toString());
        Thread.currentThread().interrupt();
        return;
    }

    private void metricItemSendFailed() {
        Map<String, String> dimensions = new HashMap<String, String>();
        dimensions.put(KEY_INLONG_GROUP_ID, inlongGroupId);
        dimensions.put(KEY_INLONG_STREAM_ID, inlongStreamId);
        AgentMetricItem metricItem = getMetricItem(dimensions);
        metricItem.pluginSendFailCount.incrementAndGet();
        metricItem.pluginSendCount.incrementAndGet();
        LOGGER.debug("plugin send failed:{}", dimensions.toString());
        Thread.currentThread().interrupt();
        return;
    }
}
