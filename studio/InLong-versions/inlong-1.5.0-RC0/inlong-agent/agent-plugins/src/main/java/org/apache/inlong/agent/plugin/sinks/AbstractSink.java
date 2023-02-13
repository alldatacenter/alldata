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

package org.apache.inlong.agent.plugin.sinks;

import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.message.PackProxyMessage;
import org.apache.inlong.agent.metrics.AgentMetricItem;
import org.apache.inlong.agent.metrics.AgentMetricItemSet;
import org.apache.inlong.agent.plugin.MessageFilter;
import org.apache.inlong.agent.plugin.Sink;
import org.apache.inlong.common.metric.MetricRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.inlong.agent.constant.AgentConstants.AGENT_MESSAGE_FILTER_CLASSNAME;
import static org.apache.inlong.agent.constant.CommonConstants.DEFAULT_PROXY_BATCH_FLUSH_INTERVAL;
import static org.apache.inlong.agent.constant.CommonConstants.DEFAULT_PROXY_INLONG_GROUP_ID;
import static org.apache.inlong.agent.constant.CommonConstants.DEFAULT_PROXY_INLONG_STREAM_ID;
import static org.apache.inlong.agent.constant.CommonConstants.PROXY_BATCH_FLUSH_INTERVAL;
import static org.apache.inlong.agent.constant.CommonConstants.PROXY_INLONG_GROUP_ID;
import static org.apache.inlong.agent.constant.CommonConstants.PROXY_INLONG_STREAM_ID;
import static org.apache.inlong.agent.constant.JobConstants.JOB_INSTANCE_ID;
import static org.apache.inlong.agent.metrics.AgentMetricItem.KEY_INLONG_GROUP_ID;
import static org.apache.inlong.agent.metrics.AgentMetricItem.KEY_INLONG_STREAM_ID;
import static org.apache.inlong.agent.metrics.AgentMetricItem.KEY_PLUGIN_ID;

/**
 * abstract sink: sink data to remote data center
 */
public abstract class AbstractSink implements Sink {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSink.class);
    protected String inlongGroupId;
    protected String inlongStreamId;

    // metric
    protected AgentMetricItemSet metricItemSet;
    protected AgentMetricItem sinkMetric;
    protected Map<String, String> dimensions;
    protected static final AtomicLong METRIC_INDEX = new AtomicLong(0);

    protected JobProfile jobConf;
    protected String sourceName;
    protected String jobInstanceId;
    protected int batchFlushInterval;
    // key is stream id, value is a batch of messages belong to the same stream id
    protected ConcurrentHashMap<String, PackProxyMessage> cache;

    @Override
    public MessageFilter initMessageFilter(JobProfile jobConf) {
        if (jobConf.hasKey(AGENT_MESSAGE_FILTER_CLASSNAME)) {
            try {
                return (MessageFilter) Class.forName(jobConf.get(AGENT_MESSAGE_FILTER_CLASSNAME))
                        .getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                LOGGER.error("init message filter error", e);
            }
        }
        return null;
    }

    @Override
    public void setSourceName(String sourceFileName) {
        this.sourceName = sourceFileName;
    }

    @Override
    public void init(JobProfile jobConf) {
        this.jobConf = jobConf;
        jobInstanceId = jobConf.get(JOB_INSTANCE_ID);
        inlongGroupId = jobConf.get(PROXY_INLONG_GROUP_ID, DEFAULT_PROXY_INLONG_GROUP_ID);
        inlongStreamId = jobConf.get(PROXY_INLONG_STREAM_ID, DEFAULT_PROXY_INLONG_STREAM_ID);
        cache = new ConcurrentHashMap<>(10);
        batchFlushInterval = jobConf.getInt(PROXY_BATCH_FLUSH_INTERVAL, DEFAULT_PROXY_BATCH_FLUSH_INTERVAL);

        this.dimensions = new HashMap<>();
        dimensions.put(KEY_PLUGIN_ID, this.getClass().getSimpleName());
        dimensions.put(KEY_INLONG_GROUP_ID, inlongGroupId);
        dimensions.put(KEY_INLONG_STREAM_ID, inlongStreamId);
        String metricName = String.join("-", this.getClass().getSimpleName(),
                String.valueOf(METRIC_INDEX.incrementAndGet()));
        this.metricItemSet = new AgentMetricItemSet(metricName);
        MetricRegister.register(metricItemSet);
        sinkMetric = metricItemSet.findMetricItem(dimensions);
    }
}
