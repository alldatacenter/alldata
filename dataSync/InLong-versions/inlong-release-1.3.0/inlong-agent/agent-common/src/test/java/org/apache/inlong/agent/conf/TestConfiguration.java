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

import static org.junit.Assert.assertEquals;

import org.apache.inlong.agent.constant.JobConstants;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

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

}
