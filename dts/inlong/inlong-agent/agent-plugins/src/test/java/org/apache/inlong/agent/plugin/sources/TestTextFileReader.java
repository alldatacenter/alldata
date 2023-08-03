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
import org.apache.inlong.agent.constant.DataCollectType;
import org.apache.inlong.agent.constant.FileTriggerType;
import org.apache.inlong.agent.constant.MetadataConstants;
import org.apache.inlong.agent.plugin.AgentBaseTestsHelper;
import org.apache.inlong.agent.plugin.Message;
import org.apache.inlong.agent.plugin.Reader;
import org.apache.inlong.agent.plugin.sources.reader.file.FileReaderOperator;
import org.apache.inlong.agent.utils.AgentUtils;
import org.apache.inlong.common.metric.MetricRegister;

import com.google.gson.Gson;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.apache.inlong.agent.constant.CommonConstants.PROXY_INLONG_GROUP_ID;
import static org.apache.inlong.agent.constant.CommonConstants.PROXY_INLONG_STREAM_ID;
import static org.apache.inlong.agent.constant.JobConstants.JOB_DIR_FILTER_PATTERNS;
import static org.apache.inlong.agent.constant.JobConstants.JOB_FILE_CONTENT_COLLECT_TYPE;
import static org.apache.inlong.agent.constant.JobConstants.JOB_FILE_LINE_END_PATTERN;
import static org.apache.inlong.agent.constant.JobConstants.JOB_FILE_MAX_WAIT;
import static org.apache.inlong.agent.constant.JobConstants.JOB_FILE_META_ENV_LIST;
import static org.apache.inlong.agent.constant.JobConstants.JOB_FILE_TRIGGER_TYPE;
import static org.apache.inlong.agent.constant.JobConstants.JOB_GROUP_ID;
import static org.apache.inlong.agent.constant.JobConstants.JOB_INSTANCE_ID;
import static org.apache.inlong.agent.constant.JobConstants.JOB_STREAM_ID;
import static org.apache.inlong.agent.constant.KubernetesConstants.KUBERNETES;
import static org.apache.inlong.agent.constant.MetadataConstants.ENV_CVM;

@PowerMockIgnore({"javax.net.ssl.*", "javax.management.*", "javax.script.*", "com.sun.org.apache.xerces.*",
        "javax.xml.*", "org.xml.*",
        "org.w3c.*"})
@PrepareForTest({MetricRegister.class})
public class TestTextFileReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestTextFileReader.class);
    private static final Gson GSON = new Gson();
    private static Path testDir;
    private static AgentBaseTestsHelper helper;

    @BeforeClass
    public static void setup() {
        helper = new AgentBaseTestsHelper(TestTextFileReader.class.getName()).setupAgentHome();
        testDir = helper.getTestRootDir();
    }

    @AfterClass
    public static void teardown() throws Exception {
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
        URI uri = getClass().getClassLoader().getResource("test").toURI();
        JobProfile jobConfiguration = JobProfile.parseJsonStr("{}");
        String mainPath = Paths.get(uri).toString();
        jobConfiguration.set(JOB_DIR_FILTER_PATTERNS, Paths.get(mainPath,
                "2.txt").toFile().getAbsolutePath());
        jobConfiguration.set(JOB_INSTANCE_ID, "test");
        jobConfiguration.set(PROXY_INLONG_GROUP_ID, "groupid");
        jobConfiguration.set(PROXY_INLONG_STREAM_ID, "streamid");
        jobConfiguration.set(JOB_GROUP_ID, "groupid");
        jobConfiguration.set(JOB_STREAM_ID, "streamid");
        TextFileSource fileSource = new TextFileSource();
        List<Reader> readerList = fileSource.split(jobConfiguration);
        Assert.assertEquals(1, readerList.size());
        Reader reader = readerList.get(0);
        reader.init(jobConfiguration);
        while (!reader.isFinished()) {
            Message message = reader.read();
            if (message == null) {
                break;
            }
            Assert.assertTrue(message.toString().contains("hello")
                    || message.toString().contains("world"));
            LOGGER.info("message is {}", message.toString());
        }
    }

    @Test
    public void testFileRowDataRead() throws URISyntaxException {
        URI uri = getClass().getClassLoader().getResource("test").toURI();
        JobProfile jobConfiguration = JobProfile.parseJsonStr("{}");
        String mainPath = Paths.get(uri).toString();
        jobConfiguration.set(JOB_DIR_FILTER_PATTERNS, Paths.get(mainPath,
                "3.txt").toFile().getAbsolutePath());
        jobConfiguration.set(JOB_INSTANCE_ID, "test");
        jobConfiguration.set(PROXY_INLONG_GROUP_ID, "groupid");
        jobConfiguration.set(PROXY_INLONG_STREAM_ID, "streamid");
        jobConfiguration.set(JOB_GROUP_ID, "groupid");
        jobConfiguration.set(JOB_STREAM_ID, "streamid");
        TextFileSource fileSource = new TextFileSource();
        List<Reader> readerList = fileSource.split(jobConfiguration);
        Assert.assertEquals(1, readerList.size());
        Reader reader = readerList.get(0);
        reader.init(jobConfiguration);
        while (!reader.isFinished()) {
            Message message = reader.read();
            if (message == null) {
                break;
            }
        }

    }

    /**
     * Custom line end character.
     */
    @Test
    public void testLineEnd() throws Exception {
        URI uri = getClass().getClassLoader().getResource("test").toURI();
        JobProfile jobConfiguration = JobProfile.parseJsonStr("{}");
        String mainPath = Paths.get(uri).toString();
        jobConfiguration.set(JOB_DIR_FILTER_PATTERNS, Paths.get(mainPath,
                "1.txt").toFile().getAbsolutePath());
        jobConfiguration.set(JOB_INSTANCE_ID, "test");
        jobConfiguration.set(PROXY_INLONG_GROUP_ID, "groupid");
        jobConfiguration.set(PROXY_INLONG_STREAM_ID, "streamid");
        jobConfiguration.set(JOB_GROUP_ID, "groupid");
        jobConfiguration.set(JOB_STREAM_ID, "streamid");
        jobConfiguration.set(JOB_FILE_TRIGGER_TYPE, FileTriggerType.FULL);
        jobConfiguration.set(JOB_FILE_LINE_END_PATTERN, "line-end-symbol");
        jobConfiguration.set(JOB_FILE_META_ENV_LIST, KUBERNETES);
        TextFileSource fileSource = new TextFileSource();
        List<Reader> readerList = fileSource.split(jobConfiguration);
        Assert.assertEquals(1, readerList.size());
        Reader reader = readerList.get(0);
        reader.init(jobConfiguration);
        while (!reader.isFinished()) {
            Message message = reader.read();
            if (message == null) {
                break;
            }
            String content = getContent(message.toString());
            LOGGER.info("content is {}", content);
            Assert.assertTrue(
                    content.equalsIgnoreCase("hello line-end-symbol aa")
                            || content.equalsIgnoreCase("world line-end-symbol")
                            || content.equalsIgnoreCase("agent line-end-symbol"));
        }
    }

    /**
     * increment of file data
     */
    @Test
    public void testIncrementData() throws Exception {
        URI uri = getClass().getClassLoader().getResource("test").toURI();
        JobProfile jobConfiguration = JobProfile.parseJsonStr("{}");
        String mainPath = Paths.get(uri).toString();
        jobConfiguration.set(JOB_DIR_FILTER_PATTERNS, Paths.get(mainPath,
                "1.txt").toFile().getAbsolutePath());
        jobConfiguration.set(JOB_INSTANCE_ID, "test");
        jobConfiguration.set(PROXY_INLONG_GROUP_ID, "groupid");
        jobConfiguration.set(PROXY_INLONG_STREAM_ID, "streamid");
        jobConfiguration.set(JOB_GROUP_ID, "groupid");
        jobConfiguration.set(JOB_STREAM_ID, "streamid");
        jobConfiguration.set(JOB_FILE_TRIGGER_TYPE, FileTriggerType.FULL);
        jobConfiguration.set(JOB_FILE_CONTENT_COLLECT_TYPE, DataCollectType.FULL);
        TextFileSource fileSource = new TextFileSource();
        List<Reader> readerList = fileSource.split(jobConfiguration);
        Assert.assertEquals(1, readerList.size());
        Reader reader = readerList.get(0);
        reader.init(jobConfiguration);

        while (!reader.isFinished()) {
            Message message = reader.read();
            if (null != message) {
                LOGGER.info("message is {}", message.toString());
                continue;
            }
            Assert.assertNull(message);
            break;
        }
    }

    @Test
    public void testTextSeekReader() throws Exception {
        Path localPath = Paths.get(testDir.toString(), "test.txt");
        LOGGER.info("start to create {}", localPath);
        List<String> beforeList = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            beforeList.add("world");
        }
        Files.write(localPath, beforeList, StandardOpenOption.CREATE);
        List<String> afterList = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            afterList.add("world");
        }
        Files.write(localPath, afterList, StandardOpenOption.APPEND);
        final FileReaderOperator fileReaderOperator = new FileReaderOperator(localPath.toFile(), 0);
        JobProfile jobProfile = new JobProfile();
        jobProfile.set(PROXY_INLONG_GROUP_ID, "groupid");
        jobProfile.set(PROXY_INLONG_STREAM_ID, "streamid");
        jobProfile.set(JOB_INSTANCE_ID, "1");
        jobProfile.set(JOB_FILE_META_ENV_LIST, ENV_CVM);
        fileReaderOperator.init(jobProfile);
        fileReaderOperator.fetchData();
        Assert.assertEquals("world", getContent(
                new String(fileReaderOperator.read().getBody(), StandardCharsets.UTF_8)));
    }

    @Test
    public void testTextTailTimeout() throws Exception {
        JobProfile jobProfile = new JobProfile();
        jobProfile.setInt(JOB_FILE_MAX_WAIT, 1);
        jobProfile.set(PROXY_INLONG_GROUP_ID, "groupid");
        jobProfile.set(PROXY_INLONG_STREAM_ID, "streamid");
        jobProfile.set(JOB_INSTANCE_ID, "1");
        Path localPath = Paths.get(testDir.toString(), "test1.txt");
        FileReaderOperator reader = new FileReaderOperator(localPath.toFile(), 0);
        if (localPath.toFile().exists()) {
            localPath.toFile().delete();
        }
        localPath.toFile().createNewFile();
        reader.init(jobProfile);
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                List<String> beforeList = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                    beforeList.add("hello, this is a new line for testTextSeekReader");
                }
                Files.write(localPath, beforeList, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception ignored) {
                LOGGER.info("ignored Exception ", ignored);
            }
        });
        TimeUnit.SECONDS.sleep(3);
        int count = 0;
        while (!reader.isFinished() && count < 5) {
            count += 1;
            LOGGER.info("ignored count ", count);
        }
        Assert.assertEquals(5, count);
    }

    private String getContent(String message) {
        Map<String, String> logJson = GSON.fromJson(message, Map.class);
        return logJson.get(MetadataConstants.DATA_CONTENT);
    }
}
