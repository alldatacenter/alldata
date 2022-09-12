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

package org.apache.inlong.agent.plugin.filter;

import static org.apache.inlong.agent.constant.AgentConstants.AGENT_MESSAGE_FILTER_CLASSNAME;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.message.ProxyMessage;
import org.apache.inlong.agent.plugin.AgentBaseTestsHelper;
import org.apache.inlong.agent.plugin.Message;
import org.apache.inlong.agent.plugin.MessageFilter;
import org.apache.inlong.agent.plugin.sinks.AbstractSink;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestStreamIdFilter {

    private static AgentBaseTestsHelper helper;
    private static Path testPath;

    @BeforeClass
    public static void setup() {
        helper = new AgentBaseTestsHelper(TestDateFormatRegex.class.getName()).setupAgentHome();
        testPath = helper.getTestRootDir();
    }

    @AfterClass
    public static void teardown() {
        helper.teardownAgentHome();
    }

    @Test
    public void testStreamId() {
        DefaultMessageFilter messageFilter = new DefaultMessageFilter();
        ProxyMessage proxyMessage = new ProxyMessage("streamId|this is a line of file".getBytes(
            StandardCharsets.UTF_8), new HashMap<>());
        String s = messageFilter.filterStreamId(proxyMessage, "|".getBytes(StandardCharsets.UTF_8));
        Assert.assertEquals(s, "streamId");
    }

    @Test
    public void testAbstractSink() {
        MockSink sinkTest = new MockSink();
        JobProfile jobProfile = new JobProfile();
        jobProfile.set(AGENT_MESSAGE_FILTER_CLASSNAME, "org.apache.inlong.agent.plugin.filter.DefaultMessageFilter");
        MessageFilter messageFilter = sinkTest.initMessageFilter(jobProfile);
        ProxyMessage proxyMessage = new ProxyMessage("tid|this is a line of file".getBytes(
            StandardCharsets.UTF_8), new HashMap<>());
        String s = messageFilter.filterStreamId(proxyMessage, "|".getBytes(StandardCharsets.UTF_8));
        Assert.assertEquals(s, "tid");
    }

    class MockSink extends AbstractSink {

        @Override
        public void write(Message message) {

        }

        @Override
        public void setSourceName(String sourceFileName) {

        }

        @Override
        public void init(JobProfile jobConf) {

        }

        @Override
        public void destroy() {

        }
    }

}

