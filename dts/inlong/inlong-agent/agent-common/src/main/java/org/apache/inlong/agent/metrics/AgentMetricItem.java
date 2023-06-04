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

package org.apache.inlong.agent.metrics;

import org.apache.inlong.common.metric.CountMetric;
import org.apache.inlong.common.metric.Dimension;
import org.apache.inlong.common.metric.GaugeMetric;
import org.apache.inlong.common.metric.MetricDomain;
import org.apache.inlong.common.metric.MetricItem;

import java.util.concurrent.atomic.AtomicLong;

@MetricDomain(name = "Agent")
public class AgentMetricItem extends MetricItem {

    // key
    public static final String KEY_PLUGIN_ID = "pluginId"; // for source, reader, channel, sink
    public static final String KEY_INLONG_GROUP_ID = "inlongGroupId";
    public static final String KEY_INLONG_STREAM_ID = "inlongStreamId";
    public static final String KEY_COMPONENT_NAME = "componentName"; // for TaskManager, JobManager

    // job
    public static final String M_JOB_RUNNING_COUNT = "jobRunningCount";
    public static final String M_JOB_FATAL_COUNT = "jobFatalCount";
    // task
    public static final String M_TASK_RUNNING_COUNT = "taskRunningCount";
    public static final String M_TASK_RETRYING_COUNT = "taskRetryingCount";
    public static final String M_TASK_FATAL_COUNT = "taskFatalCount";
    // sink
    public static final String M_SINK_SUCCESS_COUNT = "sinkSuccessCount";
    public static final String M_SINK_FAIL_COUNT = "sinkFailCount";
    // source
    public static final String M_SOURCE_SUCCESS_COUNT = "sourceSuccessCount";
    public static final String M_SOURCE_FAIL_COUNT = "sourceFailCount";
    // plugin
    public static final String M_PLUGIN_READ_COUNT = "pluginReadCount";
    public static final String M_PLUGIN_SEND_COUNT = "pluginSendCount";
    public static final String M_PLUGIN_READ_FAIL_COUNT = "pluginReadFailCount";
    public static final String M_PLUGIN_SEND_FAIL_COUNT = "pluginSendFailCount";
    public static final String M_PLUGIN_READ_SUCCESS_COUNT = "pluginReadSuccessCount";
    public static final String M_PLUGIN_SEND_SUCCESS_COUNT = "pluginSendSuccessCount";

    @Dimension
    public String pluginId;
    @Dimension
    public String inlongGroupId;
    @Dimension
    public String inlongStreamId;

    @GaugeMetric
    public AtomicLong jobRunningCount = new AtomicLong(0);
    @CountMetric
    public AtomicLong jobFatalCount = new AtomicLong(0);
    @GaugeMetric
    public AtomicLong taskRunningCount = new AtomicLong(0);
    @GaugeMetric
    public AtomicLong taskRetryingCount = new AtomicLong(0);
    @CountMetric
    public AtomicLong taskFatalCount = new AtomicLong(0);
    @CountMetric
    public AtomicLong sinkSuccessCount = new AtomicLong(0);
    @CountMetric
    public AtomicLong sinkFailCount = new AtomicLong(0);
    @CountMetric
    public AtomicLong sourceSuccessCount = new AtomicLong(0);
    @CountMetric
    public AtomicLong sourceFailCount = new AtomicLong(0);
    @CountMetric
    public AtomicLong pluginReadCount = new AtomicLong(0);
    @CountMetric
    public AtomicLong pluginSendCount = new AtomicLong(0);
    @CountMetric
    public AtomicLong pluginReadFailCount = new AtomicLong(0);
    @CountMetric
    public AtomicLong pluginSendFailCount = new AtomicLong(0);
    @CountMetric
    public AtomicLong pluginReadSuccessCount = new AtomicLong(0);
    @CountMetric
    public AtomicLong pluginSendSuccessCount = new AtomicLong(0);
}
