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

package org.apache.inlong.agent.plugin;

import org.apache.commons.io.IOUtils;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.conf.TriggerProfile;
import org.apache.inlong.agent.constant.FileCollectType;
import org.apache.inlong.agent.core.job.JobWrapper;
import org.apache.inlong.agent.core.trigger.TriggerManager;
import org.apache.inlong.agent.db.StateSearchKey;
import org.apache.inlong.agent.plugin.utils.TestUtils;
import org.apache.inlong.agent.utils.AgentUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.inlong.agent.constant.AgentConstants.AGENT_MESSAGE_FILTER_CLASSNAME;
import static org.apache.inlong.agent.constant.CommonConstants.PROXY_INLONG_GROUP_ID;
import static org.apache.inlong.agent.constant.CommonConstants.PROXY_INLONG_STREAM_ID;
import static org.apache.inlong.agent.constant.JobConstants.JOB_CYCLE_UNIT;
import static org.apache.inlong.agent.constant.JobConstants.JOB_DIR_FILTER_PATTERN;
import static org.apache.inlong.agent.constant.JobConstants.JOB_FILE_COLLECT_TYPE;
import static org.apache.inlong.agent.constant.JobConstants.JOB_FILE_MAX_WAIT;
import static org.apache.inlong.agent.constant.JobConstants.JOB_FILE_TIME_OFFSET;
import static org.apache.inlong.agent.constant.JobConstants.JOB_READ_WAIT_TIMEOUT;
import static org.awaitility.Awaitility.await;

public class TestFileAgent {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestFileAgent.class);
    private static final ClassLoader LOADER = TestFileAgent.class.getClassLoader();
    private static final String RECORD = "This is the test line for huge file\n";
    private static Path testRootDir;
    private static MiniAgent agent;
    private static AgentBaseTestsHelper helper;

    @BeforeClass
    public static void setup() {
        try {
            helper = new AgentBaseTestsHelper(TestFileAgent.class.getName()).setupAgentHome();
            agent = new MiniAgent();
            agent.start();
            testRootDir = helper.getTestRootDir();
        } catch (Exception e) {
            LOGGER.error("setup failure");
        }
    }

    @AfterClass
    public static void shutdown() throws Exception {
        if (agent != null) {
            agent.stop();
        }
        helper.teardownAgentHome();
    }

    private void createFiles(String fileName) throws Exception {
        final Path hugeFile = Paths.get(testRootDir.toString(), fileName);
        FileWriter writer = new FileWriter(hugeFile.toFile());
        for (int i = 0; i < 2; i++) {
            writer.write(RECORD);
        }
        writer.flush();
        writer.close();
    }

    @Test
    public void testFileAgent() throws Exception {
        for (int i = 0; i < 2; i++) {
            createFiles(String.format("hugeFile.%s.txt", i));
        }
        createJobProfile(0);
        assertJobSuccess();
    }

    @Test
    public void testReadTimeout() throws Exception {
        for (int i = 0; i < 10; i++) {
            createFiles(String.format("hugeFile.%s.txt", i));
        }
        createJobProfile(10);
        assertJobSuccess();
    }

    private void createJobProfile(long readWaitTimeMilliseconds) throws IOException {
        try (InputStream stream = LOADER.getResourceAsStream("fileAgentJob.json")) {
            if (stream != null) {
                String jobJson = IOUtils.toString(stream, StandardCharsets.UTF_8);
                JobProfile profile = JobProfile.parseJsonStr(jobJson);
                profile.set(JOB_DIR_FILTER_PATTERN, Paths.get(testRootDir.toString(),
                        "hugeFile.[0-9].txt").toString());
                profile.set(JOB_READ_WAIT_TIMEOUT, String.valueOf(readWaitTimeMilliseconds));
                profile.set(PROXY_INLONG_GROUP_ID, "groupid");
                profile.set(PROXY_INLONG_STREAM_ID, "streamid");
                agent.submitJob(profile);
            }
        }
    }

    @Test
    public void testOneJobOnly() throws Exception {
        String jsonString = TestUtils.getTestTriggerProfile();
        TriggerProfile triggerProfile = TriggerProfile.parseJsonStr(jsonString);
        triggerProfile.set(JOB_DIR_FILTER_PATTERN, helper.getParentPath() + triggerProfile.get(JOB_DIR_FILTER_PATTERN));
        triggerProfile.set(JOB_DIR_FILTER_PATTERN, Paths.get(testRootDir.toString(),
                "test[0-9].dat").toString());
        triggerProfile.set(JOB_FILE_MAX_WAIT, "-1");
        TriggerManager triggerManager = agent.getManager().getTriggerManager();
        triggerManager.addTrigger(triggerProfile);
        TestUtils.createHugeFiles("test0.dat", testRootDir.toString(), RECORD);
        TestUtils.createHugeFiles("test1.dat", testRootDir.toString(), RECORD);
        await().atMost(30, TimeUnit.SECONDS).until(this::checkOnlyOneJob);
        Assert.assertTrue(checkOnlyOneJob());
    }

    private Long checkFullPathReadJob() {
        Map<String, JobWrapper> jobs = agent.getManager().getJobManager().getJobs();
        AtomicLong result = new AtomicLong(0L);
        jobs.forEach((s, jobWrapper) -> {
            if (FileCollectType.FULL.equals(jobWrapper.getJob().getJobConf().get(JOB_FILE_COLLECT_TYPE, null))) {
                result.set(jobWrapper.getAllTasks().size());
            }
        });
        return result.get();
    }

    @Test
    public void testOneJobFullPath() throws Exception {
        URI uri = Objects.requireNonNull(getClass().getClassLoader().getResource("test")).toURI();
        String path = Paths.get(uri).toString();
        String fileName = path + "/increment_test.txt";
        TestUtils.deleteFile(fileName);

        String jsonString = TestUtils.getTestTriggerProfile();
        TriggerProfile triggerProfile = TriggerProfile.parseJsonStr(jsonString);
        triggerProfile.set(JOB_DIR_FILTER_PATTERN, path);
        triggerProfile.set(JOB_FILE_MAX_WAIT, "-1");
        triggerProfile.set(JOB_FILE_COLLECT_TYPE, FileCollectType.FULL);
        TriggerManager triggerManager = agent.getManager().getTriggerManager();
        triggerManager.submitTrigger(triggerProfile);
        Thread.sleep(2000);
        Assert.assertEquals(3L, checkFullPathReadJob().longValue());
        TestUtils.createFile(fileName);
        Thread.sleep(10000);
        TestUtils.deleteFile(fileName);
    }

    private boolean checkOnlyOneJob() {
        Map<String, JobWrapper> jobs = agent.getManager().getJobManager().getJobs();
        AtomicBoolean result = new AtomicBoolean(false);
        if (jobs.size() == 1) {
            jobs.forEach((s, jobWrapper) ->
                    result.set(jobWrapper.getJob().getJobConf().get(JOB_DIR_FILTER_PATTERN)
                            .equals(testRootDir + FileSystems.getDefault().getSeparator() + "test0.dat"))
            );
        }
        return result.get();
    }

    @Test
    public void testCycleUnit() throws Exception {
        String nowDate = AgentUtils.formatCurrentTimeWithoutOffset("yyyyMMdd");
        try (InputStream stream = LOADER.getResourceAsStream("fileAgentJob.json")) {
            if (stream != null) {
                String jobJson = IOUtils.toString(stream, StandardCharsets.UTF_8);
                JobProfile profile = JobProfile.parseJsonStr(jobJson);
                profile.set(JOB_DIR_FILTER_PATTERN, Paths.get(testRootDir.toString(),
                        "YYYYMMDD").toString());
                profile.set(JOB_CYCLE_UNIT, "D");
                agent.submitTriggerJob(profile);
            }
        }
        createFiles(nowDate);
        assertJobSuccess();
    }

    @Test
    public void testGroupIdFilter() throws Exception {
        String nowDate = AgentUtils.formatCurrentTimeWithoutOffset("yyyyMMdd");
        try (InputStream stream = LOADER.getResourceAsStream("fileAgentJob.json")) {
            if (stream != null) {
                String jobJson = IOUtils.toString(stream, StandardCharsets.UTF_8);
                JobProfile profile = JobProfile.parseJsonStr(jobJson);
                profile.set(JOB_DIR_FILTER_PATTERN, Paths.get(testRootDir.toString(),
                        "YYYYMMDD").toString());
                profile.set(JOB_CYCLE_UNIT, "D");
                profile.set(AGENT_MESSAGE_FILTER_CLASSNAME,
                        "org.apache.inlong.agent.plugin.filter.DefaultMessageFilter");
                agent.submitTriggerJob(profile);
            }
        }
        createFiles(nowDate);
        assertJobSuccess();
    }

    @Test
    public void testTimeOffset() throws Exception {
        String theDateBefore = AgentUtils.formatCurrentTimeWithOffset("yyyyMMdd", -1, 0, 0);
        try (InputStream stream = LOADER.getResourceAsStream("fileAgentJob.json")) {
            if (stream != null) {
                String jobJson = IOUtils.toString(stream, StandardCharsets.UTF_8);
                JobProfile profile = JobProfile.parseJsonStr(jobJson);
                profile.set(JOB_DIR_FILTER_PATTERN, Paths.get(testRootDir.toString(),
                        "YYYYMMDD").toString());
                profile.set(JOB_FILE_TIME_OFFSET, "-1d");
                profile.set(JOB_CYCLE_UNIT, "D");
                agent.submitTriggerJob(profile);
            }
        }
        createFiles(theDateBefore);
        assertJobSuccess();
    }

    private void assertJobSuccess() {
        JobProfile jobConf = agent.getManager().getJobManager().getJobConfDb().getJob(StateSearchKey.SUCCESS);
        if (jobConf != null) {
            Assert.assertEquals(1, jobConf.getInt("job.id"));
        }
    }

}
