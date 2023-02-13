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

package org.apache.inlong.agent.conf;

import org.apache.inlong.agent.constant.JobConstants;
import org.apache.inlong.agent.pojo.JobProfileDto;
import org.apache.inlong.common.constant.MQType;
import org.apache.inlong.common.pojo.agent.DataConfig;
import org.apache.inlong.common.pojo.dataproxy.DataProxyTopicInfo;
import org.apache.inlong.common.pojo.dataproxy.MQClusterInfo;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.apache.inlong.agent.constant.JobConstants.JOB_PROXY_SEND;
import static org.apache.inlong.agent.constant.JobConstants.JOB_SINK;
import static org.apache.inlong.agent.pojo.JobProfileDto.DEFAULT_DATAPROXY_SINK;
import static org.apache.inlong.agent.pojo.JobProfileDto.PULSAR_SINK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestConfiguration {

    private static AgentConfiguration agentConf;
    private static JobProfile jobConf;
    private static JobProfile jobJsonConf;

    @BeforeClass
    public static void setup() {
        agentConf = AgentConfiguration.getAgentConf();
        jobConf = JobProfile.parsePropertiesFile("job.properties");
        jobJsonConf = JobProfile.parseJsonFile("job.json");
    }

    @Test
    public void testGetInt() throws Exception {
        assertEquals(10, agentConf.getInt("agent.maxSize", 15));
        assertEquals(15, agentConf.getInt("key.not.exists", 15));
    }

    @Test
    public void testGetLong() throws Exception {
        assertEquals(200L, agentConf.getLong("agent.maxBuff", 15));
        agentConf.setLong("agent.maxBuff", 20);
        assertEquals(20L, agentConf.getLong("agent.maxBuff", 15));
        assertEquals(15L, agentConf.getLong("key.not.exists", 15));
    }

    @Test
    public void testString() throws Exception {
        assertEquals("manager", agentConf.get("agent.conf.resource", "none"));
        agentConf.set("agent.conf.resource", "selfDefine");
        assertEquals("selfDefine", agentConf.get("agent.conf.resource", "none"));
        assertEquals("none", agentConf.get("key.not.exists", "none"));
    }

    @Test
    public void testJobConf() throws Exception {
        Assert.assertTrue(jobConf.allRequiredKeyExist());
        assertEquals("1", jobConf.get(JobConstants.JOB_ID));
        assertEquals("test", jobConf.get(JobConstants.JOB_NAME));

        Assert.assertTrue(jobJsonConf.allRequiredKeyExist());
        assertEquals("1", jobJsonConf.get(JobConstants.JOB_ID));
        assertEquals("test", jobJsonConf.get(JobConstants.JOB_NAME));
    }

    @Test
    public void testJobSinkConf() {
        DataConfig dataConfig = new DataConfig();
        dataConfig.setTaskType(201);
        dataConfig.setDataReportType(1);
        JobProfile profile = JobProfileDto.convertToTriggerProfile(dataConfig);
        assertEquals(profile.get(JOB_SINK), DEFAULT_DATAPROXY_SINK);
        assertTrue(profile.getBoolean(JOB_PROXY_SEND, false));

        dataConfig.setDataReportType(0);
        profile = JobProfileDto.convertToTriggerProfile(dataConfig);
        assertEquals(profile.get(JOB_SINK), DEFAULT_DATAPROXY_SINK);
        assertFalse(profile.getBoolean(JOB_PROXY_SEND, true));

        List<MQClusterInfo> mqClusterInfos = new ArrayList<>();
        MQClusterInfo mqCluster = new MQClusterInfo();
        mqCluster.setMqType(MQType.PULSAR);
        mqCluster.setToken("token");
        mqCluster.setUrl("mqurl");
        assertTrue(mqCluster.isValid());

        mqClusterInfos.add(mqCluster);
        dataConfig.setDataReportType(2);
        dataConfig.setMqClusters(mqClusterInfos);
        dataConfig.setTopicInfo(new DataProxyTopicInfo("topic", "groupId"));
        profile = JobProfileDto.convertToTriggerProfile(dataConfig);
        List<MQClusterInfo> mqClusterResult = profile.getMqClusters();

        assertEquals(profile.get(JOB_SINK), PULSAR_SINK);
        assertEquals(mqClusterResult.size(), 1);
        assertEquals(mqClusterResult.get(0).getToken(), "token");
        assertEquals(mqClusterResult.get(0).getUrl(), "mqurl");

        DataProxyTopicInfo topicResult = profile.getMqTopic();
        assertEquals(topicResult.getTopic(), "topic");
        assertEquals(topicResult.getInlongGroupId(), "groupId");
    }

}
