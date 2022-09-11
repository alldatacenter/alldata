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

package org.apache.inlong.agent.plugin.trigger;

import java.nio.file.Path;
import java.util.HashMap;
import org.apache.inlong.agent.conf.TriggerProfile;
import org.apache.inlong.agent.constant.AgentConstants;
import org.apache.inlong.agent.plugin.AgentBaseTestsHelper;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestWatchDirTrigger {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestWatchDirTrigger.class);
    private static Path testRootDir;
    private static DirectoryTrigger dirTrigger;
    private static AgentBaseTestsHelper helper;

    @BeforeClass
    public static void setup() throws Exception {
        helper = new AgentBaseTestsHelper(TestWatchDirTrigger.class.getName()).setupAgentHome();
        testRootDir = helper.getTestRootDir();
        LOGGER.info("test root dir is {}", testRootDir);
        dirTrigger = new DirectoryTrigger();
        TriggerProfile jobConf = TriggerProfile.parseJsonStr("");
        jobConf.setInt(AgentConstants.TRIGGER_CHECK_INTERVAL, 1);
        dirTrigger.init(jobConf);
        dirTrigger.start();
    }

    @AfterClass
    public static void teardown() throws Exception {
        LOGGER.info("start to teardown test case");
        dirTrigger.stop();
        dirTrigger.join();
        helper.teardownAgentHome();
    }

    @Test
    public void testWatchEntity() throws Exception {
        PathPattern a1 = new PathPattern(helper.getParentPath().toString());
        PathPattern a2 = new PathPattern(helper.getParentPath().toString());
        HashMap<PathPattern, Integer> map = new HashMap<>();
        map.put(a1, 10);
        Integer result = map.remove(a2);
        Assert.assertEquals(a1, a2);
        Assert.assertEquals(10, result.intValue());
    }
}
