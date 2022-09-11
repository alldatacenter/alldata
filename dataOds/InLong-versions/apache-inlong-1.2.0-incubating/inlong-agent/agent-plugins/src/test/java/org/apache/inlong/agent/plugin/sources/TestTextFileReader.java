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

package org.apache.inlong.agent.plugin.sources;

import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.plugin.AgentBaseTestsHelper;
import org.apache.inlong.agent.plugin.Message;
import org.apache.inlong.agent.plugin.Reader;
import org.apache.inlong.agent.plugin.sources.reader.TextFileReader;
import org.apache.inlong.agent.plugin.utils.TestUtils;
import org.apache.inlong.agent.utils.AgentUtils;
import org.apache.inlong.common.metric.MetricRegister;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.apache.inlong.agent.constant.CommonConstants.PROXY_INLONG_GROUP_ID;
import static org.apache.inlong.agent.constant.CommonConstants.PROXY_INLONG_STREAM_ID;
import static org.apache.inlong.agent.constant.JobConstants.JOB_DIR_FILTER_PATTERN;
import static org.apache.inlong.agent.constant.JobConstants.JOB_FILE_MAX_WAIT;
import static org.apache.inlong.agent.constant.JobConstants.JOB_INSTANCE_ID;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.script.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*",
        "org.w3c.*"})
@PrepareForTest({MetricRegister.class})
public class TestTextFileReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestTextFileReader.class);
    private static Path testDir;
    private static AgentBaseTestsHelper helper;

    @BeforeClass
    public static void setup() {
        helper = new AgentBaseTestsHelper(TestTextFileReader.class.getName()).setupAgentHome();
        testDir = helper.getTestRootDir();
    }

    @AfterClass
    public static void teardown() {
        helper.teardownAgentHome();
    }

    @Test
    public void testStreamClose() throws Exception {
        Path uri = Paths.get(Objects.requireNonNull(
                getClass().getClassLoader().getResource("agent.properties")).toURI());
        Stream<String> stream = null;
        BufferedReader reader = null;
        try {
            reader = Files.newBufferedReader(uri);
            stream = reader.lines();
            Iterator<String> iterator = stream.iterator();
            stream.close();
            if (iterator.hasNext()) {
                String line = iterator.next();
                Assert.assertTrue(line.startsWith("#"));
                LOGGER.info(line);
            }
        } finally {
            AgentUtils.finallyClose(reader);
            AgentUtils.finallyClose(stream);
        }
    }

    @Test
    public void testTextFileReader() throws Exception {
        TestUtils.mockMetricRegister();
        URI uri = getClass().getClassLoader().getResource("test").toURI();
        JobProfile jobConfiguration = JobProfile.parseJsonStr("{}");
        String mainPath = Paths.get(uri).toString();
        jobConfiguration.set(JOB_DIR_FILTER_PATTERN, Paths.get(mainPath,
                "[1-2].txt").toFile().getAbsolutePath());
        jobConfiguration.set(JOB_INSTANCE_ID, "test");
        jobConfiguration.set(PROXY_INLONG_GROUP_ID, "groupid");
        jobConfiguration.set(PROXY_INLONG_STREAM_ID, "streamid");
        TextFileSource fileSource = new TextFileSource();
        List<Reader> readerList = fileSource.split(jobConfiguration);
        Assert.assertEquals(2, readerList.size());
        Reader reader = readerList.get(0);
        reader.init(jobConfiguration);
        while (!reader.isFinished()) {
            Message message = reader.read();
            if (message == null) {
                break;
            }
            Assert.assertTrue("hello".equals(message.toString())
                    || "world".equals(message.toString()));
            LOGGER.info("message is {}", message.toString());
        }
    }

    @Test
    public void testTextSeekReader() throws Exception {
        TestUtils.mockMetricRegister();
        Path localPath = Paths.get(testDir.toString(), "test.txt");
        LOGGER.info("start to create {}", localPath);
        List<String> beforeList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            beforeList.add("hello, this is a new line for testTextSeekReader");
        }
        Files.write(localPath, beforeList, StandardOpenOption.CREATE);
        List<String> afterList = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            afterList.add("world");
        }
        Files.write(localPath, afterList, StandardOpenOption.APPEND);
        TextFileReader reader = new TextFileReader(localPath.toFile(), 1000);
        JobProfile jobProfile = new JobProfile();
        jobProfile.set(PROXY_INLONG_GROUP_ID, "groupid");
        jobProfile.set(PROXY_INLONG_STREAM_ID, "streamid");
        reader.init(jobProfile);

        Assert.assertEquals("world", new String(reader.read().getBody()));

    }

    @Test
    public void testTextTailTimeout() throws Exception {
        TestUtils.mockMetricRegister();
        JobProfile jobProfile = new JobProfile();
        jobProfile.setInt(JOB_FILE_MAX_WAIT, 1);
        jobProfile.set(PROXY_INLONG_GROUP_ID, "groupid");
        jobProfile.set(PROXY_INLONG_STREAM_ID, "streamid");
        Path localPath = Paths.get(testDir.toString(), "test1.txt");
        TextFileReader reader = new TextFileReader(localPath.toFile(), 0);
        if (localPath.toFile().exists()) {
            localPath.toFile().delete();
        }
        localPath.toFile().createNewFile();
        reader.init(jobProfile);
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                List<String> beforeList = new ArrayList<>();
                for (int i = 0; i < 1000; i++) {
                    beforeList.add("hello, this is a new line for testTextSeekReader");
                }
                Files.write(localPath, beforeList, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception ignored) {
                LOGGER.info("ignored Exception ", ignored);
            }
        });
        TimeUnit.SECONDS.sleep(5);
        int count = 0;
        while (!reader.isFinished() && count < 1000) {
            count += 1;
        }
        Assert.assertEquals(1000, count);
    }
}
