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
import org.apache.inlong.agent.message.BatchProxyMessage;
import org.apache.inlong.agent.plugin.Message;
import org.apache.inlong.agent.plugin.MessageFilter;
import org.apache.inlong.agent.utils.AgentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.inlong.agent.constant.JobConstants.JOB_CYCLE_UNIT;
import static org.apache.inlong.agent.constant.JobConstants.JOB_DATA_TIME;
import static org.apache.inlong.agent.constant.JobConstants.JOB_INSTANCE_ID;

public class MockSink extends AbstractSink {

    public static final String MOCK_SINK_TAG_NAME = "AgentMockSinkMetric";
    private static final Logger LOGGER = LoggerFactory.getLogger(MockSink.class);
    private final AtomicLong number = new AtomicLong(0);
    private String sourceFileName;
    private String jobInstanceId;
    private long dataTime;
    private List<Message> messages = new ArrayList<>();

    public MockSink() {

    }

    @Override
    public void write(Message message) {
        if (message != null) {
            messages.add(message);
            number.incrementAndGet();
            BatchProxyMessage msg = new BatchProxyMessage();
            msg.setJobId(jobInstanceId);
            // increment the count of successful sinks
            sinkMetric.sinkSuccessCount.incrementAndGet();
        } else {
            // increment the count of failed sinks
            sinkMetric.sinkFailCount.incrementAndGet();
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
        super.init(jobConf);
        jobInstanceId = jobConf.get(JOB_INSTANCE_ID);
        dataTime = AgentUtils.timeStrConvertToMillSec(jobConf.get(JOB_DATA_TIME, ""),
                jobConf.get(JOB_CYCLE_UNIT, ""));
        sourceFileName = "test";
        LOGGER.info("get dataTime is : {}", dataTime);
    }

    @Override
    public void destroy() {
        LOGGER.info("destroy mockSink, sink line number is : {}", number.get());
    }

    public List<Message> getResult() {
        return messages;
    }
}
