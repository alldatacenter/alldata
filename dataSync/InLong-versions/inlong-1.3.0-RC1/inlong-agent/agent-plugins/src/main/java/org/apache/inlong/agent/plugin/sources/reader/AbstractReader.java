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

package org.apache.inlong.agent.plugin.sources.reader;

import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.metrics.AgentMetricItem;
import org.apache.inlong.agent.metrics.AgentMetricItemSet;
import org.apache.inlong.agent.plugin.Reader;
import org.apache.inlong.common.metric.MetricRegister;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.inlong.agent.constant.CommonConstants.DEFAULT_PROXY_INLONG_GROUP_ID;
import static org.apache.inlong.agent.constant.CommonConstants.DEFAULT_PROXY_INLONG_STREAM_ID;
import static org.apache.inlong.agent.constant.CommonConstants.PROXY_INLONG_GROUP_ID;
import static org.apache.inlong.agent.constant.CommonConstants.PROXY_INLONG_STREAM_ID;
import static org.apache.inlong.agent.metrics.AgentMetricItem.KEY_INLONG_GROUP_ID;
import static org.apache.inlong.agent.metrics.AgentMetricItem.KEY_INLONG_STREAM_ID;
import static org.apache.inlong.agent.metrics.AgentMetricItem.KEY_PLUGIN_ID;

/**
 * Abstract reader, init reader and reader metrics
 */
public abstract class AbstractReader implements Reader {

    protected static final AtomicLong METRIC_INDEX = new AtomicLong(0);
    protected String inlongGroupId;
    protected String inlongStreamId;
    //metric
    protected AgentMetricItemSet metricItemSet;
    protected AgentMetricItem readerMetric;
    protected String metricName;
    protected Map<String, String> dimensions;

    @Override
    public void init(JobProfile jobConf) {
        inlongGroupId = jobConf.get(PROXY_INLONG_GROUP_ID, DEFAULT_PROXY_INLONG_GROUP_ID);
        inlongStreamId = jobConf.get(PROXY_INLONG_STREAM_ID, DEFAULT_PROXY_INLONG_STREAM_ID);

        this.dimensions = new HashMap<>();
        dimensions.put(KEY_PLUGIN_ID, this.getClass().getSimpleName());
        dimensions.put(KEY_INLONG_GROUP_ID, inlongGroupId);
        dimensions.put(KEY_INLONG_STREAM_ID, inlongStreamId);
        metricName = String.join("-", this.getClass().getSimpleName(),
                String.valueOf(METRIC_INDEX.incrementAndGet()));
        this.metricItemSet = new AgentMetricItemSet(metricName);
        MetricRegister.register(metricItemSet);
        readerMetric = metricItemSet.findMetricItem(dimensions);
    }

    public String getInlongGroupId() {
        return inlongGroupId;
    }
}
