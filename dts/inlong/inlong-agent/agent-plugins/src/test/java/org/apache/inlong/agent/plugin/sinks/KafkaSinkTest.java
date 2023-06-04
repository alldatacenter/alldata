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
import org.apache.inlong.agent.message.ProxyMessage;
import org.apache.inlong.agent.plugin.AgentBaseTestsHelper;
import org.apache.inlong.agent.plugin.MiniAgent;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.apache.inlong.agent.constant.CommonConstants.PROXY_KEY_GROUP_ID;
import static org.apache.inlong.agent.constant.CommonConstants.PROXY_KEY_STREAM_ID;
import static org.junit.Assert.assertEquals;

public class KafkaSinkTest {

    private static MockSink kafkaSink;
    private static JobProfile jobProfile;
    private static AgentBaseTestsHelper helper;
    private static MiniAgent agent;

    @BeforeClass
    public static void setUp() throws Exception {
        helper = new AgentBaseTestsHelper(KafkaSinkTest.class.getName()).setupAgentHome();
        agent = new MiniAgent();
        jobProfile = JobProfile.parseJsonFile("kafkaSinkJob.json");
        jobProfile.set("job.mqClusters",
                "[{\"url\":\"mqurl\",\"token\":\"token\",\"mqType\":\"KAFKA\",\"params\":{}}]");
        jobProfile.set("job.topicInfo", "{\"topic\":\"topic\",\"inlongGroupId\":\"groupId\"}");
        System.out.println(jobProfile.toJsonStr());
        kafkaSink = new MockSink();
        kafkaSink.init(jobProfile);
    }

    @Test
    public void testWrite() {
        String body = "testMesage";
        Map<String, String> attr = new HashMap<>();
        attr.put(PROXY_KEY_GROUP_ID, "groupId");
        attr.put(PROXY_KEY_STREAM_ID, "streamId");
        long count = 5;
        for (long i = 0; i < 5; i++) {
            kafkaSink.write(new ProxyMessage(body.getBytes(StandardCharsets.UTF_8), attr));
        }
        assertEquals(kafkaSink.sinkMetric.sinkSuccessCount.get(), count);
    }

}
