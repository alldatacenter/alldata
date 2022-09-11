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

import com.google.common.base.Joiner;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.plugin.MessageFilter;
import org.apache.inlong.agent.plugin.Sink;
import org.apache.inlong.agent.plugin.metrics.SinkJmxMetric;
import org.apache.inlong.agent.plugin.metrics.SinkMetrics;
import org.apache.inlong.agent.plugin.metrics.SinkPrometheusMetrics;
import org.apache.inlong.agent.utils.ConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

import static org.apache.inlong.agent.constant.AgentConstants.AGENT_MESSAGE_FILTER_CLASSNAME;
import static org.apache.inlong.agent.constant.CommonConstants.DEFAULT_PROXY_INLONG_GROUP_ID;
import static org.apache.inlong.agent.constant.CommonConstants.DEFAULT_PROXY_INLONG_STREAM_ID;
import static org.apache.inlong.agent.constant.CommonConstants.PROXY_INLONG_GROUP_ID;
import static org.apache.inlong.agent.constant.CommonConstants.PROXY_INLONG_STREAM_ID;

/**
 * abstract sink: sink data to remote data center
 */
public abstract class AbstractSink implements Sink {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSink.class);

    protected static SinkMetrics sinkMetric;

    protected static SinkMetrics streamMetric;
    private static AtomicLong index = new AtomicLong(0);
    protected String inlongGroupId;
    protected String inlongStreamId;

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
    public void init(JobProfile jobConf) {
        inlongGroupId = jobConf.get(PROXY_INLONG_GROUP_ID, DEFAULT_PROXY_INLONG_GROUP_ID);
        inlongStreamId = jobConf.get(PROXY_INLONG_STREAM_ID, DEFAULT_PROXY_INLONG_STREAM_ID);
    }

    /**
     * init sinkMetric
     *
     * @param tagName metric tagName
     */
    protected void intMetric(String tagName) {
        String label = Joiner.on(",").join(tagName, index.getAndIncrement());
        if (ConfigUtil.isPrometheusEnabled()) {
            sinkMetric = new SinkPrometheusMetrics(label);
        } else {
            sinkMetric = new SinkJmxMetric(label);
        }
        label = Joiner.on(",").join(tagName, inlongGroupId, inlongStreamId);
        if (ConfigUtil.isPrometheusEnabled()) {
            streamMetric = new SinkPrometheusMetrics(label);
        } else {
            streamMetric = new SinkJmxMetric(label);
        }
    }

}
