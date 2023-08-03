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

package org.apache.inlong.agent.core.task;

import org.apache.inlong.agent.conf.AgentConfiguration;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.apache.inlong.agent.constant.FetcherConstants.AGENT_GLOBAL_CHANNEL_PERMIT;
import static org.apache.inlong.agent.constant.FetcherConstants.AGENT_GLOBAL_READER_QUEUE_PERMIT;
import static org.apache.inlong.agent.constant.FetcherConstants.AGENT_GLOBAL_READER_SOURCE_PERMIT;
import static org.apache.inlong.agent.constant.FetcherConstants.AGENT_GLOBAL_WRITER_PERMIT;
import static org.apache.inlong.agent.constant.FetcherConstants.DEFAULT_AGENT_GLOBAL_CHANNEL_PERMIT;
import static org.apache.inlong.agent.constant.FetcherConstants.DEFAULT_AGENT_GLOBAL_READER_QUEUE_PERMIT;
import static org.apache.inlong.agent.constant.FetcherConstants.DEFAULT_AGENT_GLOBAL_READER_SOURCE_PERMIT;
import static org.apache.inlong.agent.constant.FetcherConstants.DEFAULT_AGENT_GLOBAL_WRITER_PERMIT;

public class TestMemoryManager {

    private static AgentConfiguration conf;

    @BeforeClass
    public static void setup() throws Exception {
        conf = AgentConfiguration.getAgentConf();
    }

    @Test
    public void testAll() {
        int sourcePermit = conf.getInt(AGENT_GLOBAL_READER_SOURCE_PERMIT, DEFAULT_AGENT_GLOBAL_READER_SOURCE_PERMIT);
        int readerQueuePermit = conf.getInt(AGENT_GLOBAL_READER_QUEUE_PERMIT, DEFAULT_AGENT_GLOBAL_READER_QUEUE_PERMIT);
        int channelPermit = conf.getInt(AGENT_GLOBAL_CHANNEL_PERMIT, DEFAULT_AGENT_GLOBAL_CHANNEL_PERMIT);
        int writerPermit = conf.getInt(AGENT_GLOBAL_WRITER_PERMIT, DEFAULT_AGENT_GLOBAL_WRITER_PERMIT);

        boolean suc = MemoryManager.getInstance().tryAcquire(AGENT_GLOBAL_READER_SOURCE_PERMIT, sourcePermit);
        Assert.assertTrue(suc);
        suc = MemoryManager.getInstance().tryAcquire(AGENT_GLOBAL_READER_QUEUE_PERMIT, readerQueuePermit);
        Assert.assertTrue(suc);
        suc = MemoryManager.getInstance().tryAcquire(AGENT_GLOBAL_CHANNEL_PERMIT, channelPermit);
        Assert.assertTrue(suc);
        suc = MemoryManager.getInstance().tryAcquire(AGENT_GLOBAL_WRITER_PERMIT, writerPermit);
        Assert.assertTrue(suc);

        suc = MemoryManager.getInstance().tryAcquire(AGENT_GLOBAL_READER_SOURCE_PERMIT, 1);
        Assert.assertFalse(suc);
        suc = MemoryManager.getInstance().tryAcquire(AGENT_GLOBAL_READER_QUEUE_PERMIT, 1);
        Assert.assertFalse(suc);
        suc = MemoryManager.getInstance().tryAcquire(AGENT_GLOBAL_CHANNEL_PERMIT, 1);
        Assert.assertFalse(suc);
        suc = MemoryManager.getInstance().tryAcquire(AGENT_GLOBAL_WRITER_PERMIT, 1);
        Assert.assertFalse(suc);

        MemoryManager.getInstance().release(AGENT_GLOBAL_READER_SOURCE_PERMIT, sourcePermit);
        MemoryManager.getInstance().release(AGENT_GLOBAL_READER_QUEUE_PERMIT, readerQueuePermit);
        MemoryManager.getInstance().release(AGENT_GLOBAL_CHANNEL_PERMIT, channelPermit);
        MemoryManager.getInstance().release(AGENT_GLOBAL_WRITER_PERMIT, writerPermit);

        suc = MemoryManager.getInstance().tryAcquire(AGENT_GLOBAL_READER_SOURCE_PERMIT, sourcePermit);
        Assert.assertTrue(suc);
        suc = MemoryManager.getInstance().tryAcquire(AGENT_GLOBAL_READER_QUEUE_PERMIT, readerQueuePermit);
        Assert.assertTrue(suc);
        suc = MemoryManager.getInstance().tryAcquire(AGENT_GLOBAL_CHANNEL_PERMIT, channelPermit);
        Assert.assertTrue(suc);
        suc = MemoryManager.getInstance().tryAcquire(AGENT_GLOBAL_WRITER_PERMIT, writerPermit);
        Assert.assertTrue(suc);
    }

}