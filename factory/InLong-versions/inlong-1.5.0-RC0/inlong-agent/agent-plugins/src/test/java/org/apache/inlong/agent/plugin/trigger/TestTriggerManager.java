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

import org.apache.inlong.agent.conf.AgentConfiguration;
import org.apache.inlong.agent.conf.TriggerProfile;
import org.apache.inlong.agent.constant.AgentConstants;
import org.apache.inlong.agent.constant.JobConstants;
import org.apache.inlong.agent.plugin.MiniAgent;
import org.apache.inlong.agent.plugin.utils.TestUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.WatchKey;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

public class TestTriggerManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestTriggerManager.class);

    private static MiniAgent agent;

    @ClassRule
    public static final TemporaryFolder WATCH_FOLDER = new TemporaryFolder();

    public static final String FILE_JOB_TEMPLATE = "{\n"
            + "  \"job\": {\n"
            + "    \"fileJob\": {\n"
            + "      \"trigger\": \"org.apache.inlong.agent.plugin.trigger.DirectoryTrigger\",\n"
            + "      \"dir\": {\n"
            + "        \"patterns\": \"/AgentBaseTestsHelper/"
            + "org.apache.tubemq.inlong.plugin.fetcher.TestTdmFetcher/test*.dat\"\n"
            + "      },\n"
            + "      \"thread\" : {\n"
            + "        \"running\": {\n"
            + "          \"core\": \"4\"\n"
            + "        }\n"
            + "      } \n"
            + "    },\n"
            + "    \"id\": 1,\n"
            + "    \"op\": \"0\",\n"
            + "    \"ip\": \"127.0.0.1\",\n"
            + "    \"groupId\": \"groupId\",\n"
            + "    \"streamId\": \"streamId\",\n"
            + "    \"name\": \"fileAgentTest\",\n"
            + "    \"source\": \"org.apache.inlong.agent.plugin.sources.TextFileSource\",\n"
            + "    \"sink\": \"org.apache.inlong.agent.plugin.sinks.MockSink\",\n"
            + "    \"channel\": \"org.apache.inlong.agent.plugin.channel.MemoryChannel\",\n"
            + "    \"standalone\": true,\n"
            + "    \"deliveryTime\": \"1231313\",\n"
            + "    \"splitter\": \"&\"\n"
            + "  }\n"
            + "}";

    @BeforeClass
    public static void setup() {
        try {
            AgentConfiguration.getAgentConf().set(AgentConstants.AGENT_HOME, WATCH_FOLDER.getRoot().getAbsolutePath());
            agent = new MiniAgent();
            agent.start();
        } catch (Exception e) {
            LOGGER.error("setup failure");
        }
    }

    @After
    public void teardownEach() {
        agent.cleanupTriggers();
    }

    @Test
    public void testRestartTriggerJobRestore() throws Exception {
        TriggerProfile triggerProfile1 = TriggerProfile.parseJsonStr(FILE_JOB_TEMPLATE);
        triggerProfile1.set(JobConstants.JOB_ID, "1");
        triggerProfile1.set(JobConstants.JOB_DIR_FILTER_PATTERNS,
                WATCH_FOLDER.getRoot() + "/*.log");
        agent.submitTrigger(triggerProfile1);

        WATCH_FOLDER.newFolder("tmp");
        TestUtils.createHugeFiles("1.log", WATCH_FOLDER.getRoot().getAbsolutePath(), "asdqwdqd");
        TestUtils.createHugeFiles("2.log", WATCH_FOLDER.getRoot().getAbsolutePath(), "asdasdasd");
        await().atMost(10, TimeUnit.SECONDS).until(() -> agent.getManager().getTaskManager().getTaskSize() == 3);

        agent.restart();
        await().atMost(10, TimeUnit.SECONDS).until(() -> agent.getManager().getTaskManager().getTaskSize() == 3);

        // cleanup
        TestUtils.deleteFile(WATCH_FOLDER.getRoot().getAbsolutePath() + "/1.log");
        TestUtils.deleteFile(WATCH_FOLDER.getRoot().getAbsolutePath() + "/2.log");
        TestUtils.deleteFile(WATCH_FOLDER.getRoot().getAbsolutePath() + "/tmp/3.log");
    }

    @Test
    public void testMultiTriggerWatchSameDir() throws Exception {
        TriggerProfile triggerProfile1 = TriggerProfile.parseJsonStr(FILE_JOB_TEMPLATE);
        triggerProfile1.set(JobConstants.JOB_ID, "1");
        triggerProfile1.set(JobConstants.JOB_DIR_FILTER_PATTERNS,
                WATCH_FOLDER.getRoot() + "/*.log");

        TriggerProfile triggerProfile2 = TriggerProfile.parseJsonStr(FILE_JOB_TEMPLATE);
        triggerProfile2.set(JobConstants.JOB_ID, "2");
        triggerProfile2.set(JobConstants.JOB_DIR_FILTER_PATTERNS,
                WATCH_FOLDER.getRoot() + "/*.txt");

        agent.submitTrigger(triggerProfile1);
        agent.submitTrigger(triggerProfile2);

        TestUtils.createHugeFiles("1.log", WATCH_FOLDER.getRoot().getAbsolutePath(), "asdqwdqd");
        TestUtils.createHugeFiles("1.txt", WATCH_FOLDER.getRoot().getAbsolutePath(), "asdasdasd");
        await().atMost(10, TimeUnit.SECONDS).until(() -> agent.getManager().getTaskManager().getTaskSize() == 4);

        // cleanup
        TestUtils.deleteFile(WATCH_FOLDER.getRoot().getAbsolutePath() + "/1.log");
        TestUtils.deleteFile(WATCH_FOLDER.getRoot().getAbsolutePath() + "/1.txt");
    }

    @Test
    public void testSubmitAndShutdown() throws Exception {
        TriggerProfile triggerProfile1 = TriggerProfile.parseJsonStr(FILE_JOB_TEMPLATE);
        triggerProfile1.set(JobConstants.JOB_ID, "1");
        triggerProfile1.set(JobConstants.JOB_DIR_FILTER_PATTERNS,
                WATCH_FOLDER.getRoot() + "/*.log");

        // submit trigger
        agent.submitTrigger(triggerProfile1);
        TestUtils.createHugeFiles("1.log", WATCH_FOLDER.getRoot().getAbsolutePath(), "asdqwdqd");
        DirectoryTrigger trigger = (DirectoryTrigger) agent.getManager()
                .getTriggerManager().getTrigger(triggerProfile1.getTriggerId());
        await().atMost(10, TimeUnit.SECONDS).until(() -> {
            if (trigger.getWatchers().size() == 0) {
                return false;
            }

            for (Map.Entry<WatchKey, Set<DirectoryTrigger>> entry : trigger.getWatchers().entrySet()) {
                if (entry.getValue().size() != 1) {
                    return false;
                }
                if (entry.getValue().size() == 1 && !entry.getValue().stream().findAny().get().equals(trigger)) {
                    return false;
                }
            }
            return true;
        });

        // shutdown trigger
        agent.getManager().getTriggerManager().deleteTrigger(triggerProfile1.getTriggerId());
        await().atMost(10, TimeUnit.SECONDS).until(() -> trigger.getWatchers().size() == 0);
        TestUtils.deleteFile(WATCH_FOLDER.getRoot().getAbsolutePath() + "/1.log");
    }
}
