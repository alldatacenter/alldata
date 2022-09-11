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

import java.nio.charset.StandardCharsets;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.plugin.Message;
import org.apache.inlong.agent.plugin.MessageFilter;

/**
 * message write to console
 */
public class ConsoleSink extends AbstractSink {

    private static final String CONSOLE_SINK_TAG_NAME = "AgentConsoleSinkMetric";

    public ConsoleSink() {

    }

    @Override
    public void write(Message message) {
        if (message != null) {
            System.out.println(new String(message.getBody(), StandardCharsets.UTF_8));
            // increment the count of successful sinks
            sinkMetric.incSinkSuccessCount();
            streamMetric.incSinkSuccessCount();
        } else {
            // increment the count of failed sinks
            sinkMetric.incSinkFailCount();
            streamMetric.incSinkFailCount();
        }
    }

    @Override
    public void setSourceName(String sourceFileName) {

    }

    @Override
    public MessageFilter initMessageFilter(JobProfile jobConf) {
        return null;
    }

    @Override
    public void init(JobProfile jobConf) {
        super.init(jobConf);
        intMetric(CONSOLE_SINK_TAG_NAME);
    }

    @Override
    public void destroy() {

    }
}
