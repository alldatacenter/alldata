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

import static org.apache.inlong.agent.constant.JobConstants.JOB_CYCLE_UNIT;
import static org.apache.inlong.agent.constant.JobConstants.JOB_DATA_TIME;
import static org.apache.inlong.agent.constant.JobConstants.JOB_INSTANCE_ID;

import java.util.concurrent.atomic.AtomicLong;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.core.task.TaskPositionManager;
import org.apache.inlong.agent.plugin.Message;
import org.apache.inlong.agent.plugin.MessageFilter;
import org.apache.inlong.agent.plugin.Sink;
import org.apache.inlong.agent.plugin.metrics.SinkJmxMetric;
import org.apache.inlong.agent.plugin.metrics.SinkMetrics;
import org.apache.inlong.agent.plugin.metrics.SinkPrometheusMetrics;
import org.apache.inlong.agent.utils.AgentUtils;
import org.apache.inlong.agent.utils.ConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockSink implements Sink {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockSink.class);

    public static final String MOCK_SINK_TAG_NAME = "AgentMockSinkMetric";

    private final AtomicLong number = new AtomicLong(0);
    private TaskPositionManager taskPositionManager;
    private String sourceFileName;
    private String jobInstanceId;
    private long dataTime;

    private final SinkMetrics sinkMetrics;
    private static AtomicLong metricsIndex = new AtomicLong(0);

    public MockSink() {
        if (ConfigUtil.isPrometheusEnabled()) {
            this.sinkMetrics = new SinkPrometheusMetrics(AgentUtils.getUniqId(
                    MOCK_SINK_TAG_NAME,  metricsIndex.incrementAndGet()));
        } else {
            this.sinkMetrics = new SinkJmxMetric(AgentUtils.getUniqId(
                    MOCK_SINK_TAG_NAME,  metricsIndex.incrementAndGet()));
        }
    }

    @Override
    public void write(Message message) {
        if (message != null) {
            number.incrementAndGet();
            taskPositionManager.updateSinkPosition(jobInstanceId, sourceFileName, 1);
            // increment the count of successful sinks
            sinkMetrics.incSinkSuccessCount();
        } else {
            // increment the count of failed sinks
            sinkMetrics.incSinkFailCount();
        }
    }

    @Override
    public void setSourceName(String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }

    @Override
    public MessageFilter initMessageFilter(JobProfile jobConf) {
        return null;
    }

    @Override
    public void init(JobProfile jobConf) {
        taskPositionManager = TaskPositionManager.getTaskPositionManager();
        jobInstanceId = jobConf.get(JOB_INSTANCE_ID);
        dataTime = AgentUtils.timeStrConvertToMillSec(jobConf.get(JOB_DATA_TIME, ""),
            jobConf.get(JOB_CYCLE_UNIT, ""));
        LOGGER.info("get dataTime is : {}", dataTime);
    }

    @Override
    public void destroy() {
        LOGGER.info("destroy mockSink, sink line number is : {}", number.get());
    }

}
