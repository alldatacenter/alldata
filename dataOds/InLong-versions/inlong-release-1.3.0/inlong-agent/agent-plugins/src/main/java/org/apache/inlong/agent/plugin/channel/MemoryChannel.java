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
import org.apache.inlong.agent.message.ProxyMessage;
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
import static org.apache.inlong.agent.metrics.AgentMetricItem.KEY_INLONG_GROUP_ID;
import static org.apache.inlong.agent.metrics.AgentMetricItem.KEY_PLUGIN_ID;

/**
 * memory channel
 */
public class MemoryChannel implements Channel {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryChannel.class);

    private LinkedBlockingQueue<Message> queue;
    //metric
    private AgentMetricItemSet metricItemSet;
    private static final AtomicLong METRIC_INDEX = new AtomicLong(0);

    public MemoryChannel() {
    }

    @Override
    public void push(Message message) {
        String groupId = DEFAULT_PROXY_INLONG_GROUP_ID;
        try {
            if (message != null) {
                if (message instanceof ProxyMessage) {
                    groupId = ((ProxyMessage) message).getInlongGroupId();
                }
                AgentMetricItem metricItem = getMetricItem(KEY_INLONG_GROUP_ID, groupId);
                metricItem.pluginReadCount.incrementAndGet();
                queue.put(message);
                metricItem.pluginReadSuccessCount.incrementAndGet();
            }
        } catch (InterruptedException ex) {
            getMetricItem(KEY_INLONG_GROUP_ID, groupId).pluginReadFailCount.incrementAndGet();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public boolean push(Message message, long timeout, TimeUnit unit) {
        String groupId = DEFAULT_PROXY_INLONG_GROUP_ID;
        try {
            if (message != null) {
                if (message instanceof ProxyMessage) {
                    groupId = ((ProxyMessage) message).getInlongGroupId();
                }
                AgentMetricItem metricItem = getMetricItem(KEY_INLONG_GROUP_ID, groupId);
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
            AgentMetricItem metricItem = getMetricItem(KEY_INLONG_GROUP_ID, groupId);
            metricItem.pluginReadFailCount.incrementAndGet();
            Thread.currentThread().interrupt();
        }
        return false;
    }

    @Override
    public Message pull(long timeout, TimeUnit unit) {
        String groupId = DEFAULT_PROXY_INLONG_GROUP_ID;
        try {
            Message message = queue.poll(timeout, unit);
            if (message != null) {
                if (message instanceof ProxyMessage) {
                    groupId = ((ProxyMessage) message).getInlongGroupId();
                }
                AgentMetricItem metricItem = getMetricItem(KEY_INLONG_GROUP_ID, groupId);
                metricItem.pluginSendSuccessCount.incrementAndGet();
            }
            return message;
        } catch (InterruptedException ex) {
            AgentMetricItem metricItem = getMetricItem(KEY_INLONG_GROUP_ID, groupId);
            metricItem.pluginSendFailCount.incrementAndGet();
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public void init(JobProfile jobConf) {
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

    private AgentMetricItem getMetricItem(String otherKey, String value) {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put(KEY_PLUGIN_ID, this.getClass().getSimpleName());
        dimensions.put(otherKey, value);
        return this.metricItemSet.findMetricItem(dimensions);
    }
}
