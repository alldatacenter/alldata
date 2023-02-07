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

package org.apache.inlong.agent.plugin.sources;

import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.metrics.AgentMetricItem;
import org.apache.inlong.agent.metrics.AgentMetricItemSet;
import org.apache.inlong.agent.plugin.Source;
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

public abstract class AbstractSource implements Source {

    protected String inlongGroupId;
    protected String inlongStreamId;
    // metric
    protected AgentMetricItemSet metricItemSet;
    protected AgentMetricItem sourceMetric;
    protected String metricName;
    protected Map<String, String> dimensions;
    protected static final AtomicLong METRIX_INDEX = new AtomicLong(0);

    protected void init(JobProfile conf) {
        inlongGroupId = conf.get(PROXY_INLONG_GROUP_ID, DEFAULT_PROXY_INLONG_GROUP_ID);
        inlongStreamId = conf.get(PROXY_INLONG_STREAM_ID, DEFAULT_PROXY_INLONG_STREAM_ID);
        // register metric
        this.dimensions = new HashMap<>();
        dimensions.put(KEY_PLUGIN_ID, this.getClass().getSimpleName());
        dimensions.put(KEY_INLONG_GROUP_ID, inlongGroupId);
        dimensions.put(KEY_INLONG_STREAM_ID, inlongStreamId);
        metricName = String.join("-", this.getClass().getSimpleName(),
                String.valueOf(METRIX_INDEX.incrementAndGet()));
        this.metricItemSet = new AgentMetricItemSet(metricName);
        MetricRegister.register(metricItemSet);
        sourceMetric = metricItemSet.findMetricItem(dimensions);
    }
}
