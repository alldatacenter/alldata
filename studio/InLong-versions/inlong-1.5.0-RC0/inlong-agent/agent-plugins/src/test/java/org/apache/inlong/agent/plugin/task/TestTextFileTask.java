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

package org.apache.inlong.agent.plugin.task;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.constant.DataCollectType;
import org.apache.inlong.agent.constant.JobConstants;
import org.apache.inlong.agent.constant.MetadataConstants;
import org.apache.inlong.agent.core.task.Task;
import org.apache.inlong.agent.core.task.TaskManager;
import org.apache.inlong.agent.core.task.TaskWrapper;
import org.apache.inlong.agent.metrics.AgentMetricItem;
import org.apache.inlong.agent.metrics.AgentMetricItemSet;
import org.apache.inlong.agent.plugin.Channel;
import org.apache.inlong.agent.plugin.Reader;
import org.apache.inlong.agent.plugin.channel.MemoryChannel;
import org.apache.inlong.agent.plugin.sinks.MockSink;
import org.apache.inlong.agent.plugin.sources.TextFileSource;
import org.apache.inlong.agent.plugin.sources.reader.file.MonitorTextFile;
import org.apache.inlong.agent.plugin.utils.TestUtils;
import org.apache.inlong.common.metric.MetricItem;
import org.apache.inlong.common.metric.MetricRegister;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.apache.inlong.agent.constant.JobConstants.JOB_FILE_META_ENV_LIST;
import static org.apache.inlong.agent.constant.KubernetesConstants.KUBERNETES;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;
import static org.powermock.api.support.membermodification.MemberMatcher.field;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TaskManager.class, MetricRegister.class})
@PowerMockIgnore("javax.management.*")
public class TestTextFileTask {

    private static TaskManager taskManager;
    private static AgentMetricItemSet agentMetricItemSet;
    private static AgentMetricItem agentMetricItem;
    public static final TemporaryFolder TMP_FOLDER = new TemporaryFolder();
    private static List<String> taskCache;
    private static AtomicLong atomicLong;
    private static AtomicLong atomicCountLong;
    private static final Gson GSON = new Gson();

    @BeforeClass
    public static void setup() throws Exception {
        atomicLong = new AtomicLong(0L);
        atomicCountLong = new AtomicLong(0L);
        taskCache = new ArrayList<>();
        TMP_FOLDER.create();

        // mock metrics
        taskManager = new TaskManager(null);
        agentMetricItemSet = mock(AgentMetricItemSet.class);
        agentMetricItem = mock(AgentMetricItem.class);
        whenNew(AgentMetricItemSet.class).withArguments(anyString()).thenReturn(agentMetricItemSet);
        when(agentMetricItemSet.findMetricItem(any())).thenReturn(agentMetricItem);
        field(AgentMetricItem.class, "pluginReadCount").set(agentMetricItem, atomicLong);
        field(AgentMetricItem.class, "pluginReadSuccessCount").set(agentMetricItem, atomicCountLong);
        PowerMockito.mockStatic(MetricRegister.class);
        PowerMockito.doNothing().when(
                MetricRegister.class, "register", any(MetricItem.class));
    }

    @AfterClass
    public static void teardown() {
        TMP_FOLDER.delete();
    }

    @After
    public void teardownEach() {
        taskCache.forEach(taskManager::removeTask);
        taskCache.clear();
    }

    public MockSink mockTextTask(JobProfile jobProfile) {
        List<Reader> readers = new TextFileSource().split(jobProfile);
        Channel channel = new MemoryChannel();
        MockSink sink = new MockSink();

        readers.forEach(reader -> {
            String taskId = String.format("Text file read %s", reader.getReadSource());
            TaskWrapper taskWrapper =
                    new TaskWrapper(taskManager, new Task(taskId, reader, sink, channel, jobProfile));
            taskManager.submitTask(taskWrapper);
            taskCache.add(taskId);
        });
        return sink;
    }

    /**
     * Test metadata info with env list contains cvm.
     */
    @Test
    public void testMetadataWithVM() throws IOException {
        File file = TMP_FOLDER.newFile();
        JobProfile jobProfile = new JobProfile();
        jobProfile.set(JobConstants.JOB_INSTANCE_ID, "1");
        jobProfile.set(JobConstants.JOB_DIR_FILTER_PATTERNS, file.getAbsolutePath());
        jobProfile.set(JobConstants.JOB_FILE_META_ENV_LIST, MetadataConstants.ENV_CVM);
        jobProfile.set(JobConstants.JOB_FILE_CONTENT_COLLECT_TYPE, DataCollectType.FULL);
        jobProfile.set(JobConstants.JOB_TASK_BEGIN_WAIT_SECONDS, String.valueOf(0));

        // mock data
        final MockSink sink = mockTextTask(jobProfile);
        StringBuffer sb = new StringBuffer();
        sb.append("TEST");
        sb.append(System.lineSeparator());

        TestUtils.write(file.getAbsolutePath(), sb);
        await().atMost(10, TimeUnit.SECONDS).until(() -> sink.getResult().size() == 1);
        sink.getResult().forEach(message -> {
            String content = new String(message.getBody(), Charset.forName("UTF-8"));
            Map<String, String> logJson = GSON.fromJson(content, Map.class);
            Assert.assertTrue(logJson.containsKey(MetadataConstants.METADATA_SOURCE_IP));
            Assert.assertTrue(logJson.containsKey(MetadataConstants.METADATA_FILE_NAME));
            Assert.assertTrue(logJson.containsKey(MetadataConstants.METADATA_HOST_NAME));
            Assert.assertTrue(logJson.containsKey(MetadataConstants.DATA_CONTENT));
            Assert.assertTrue(logJson.containsKey(MetadataConstants.DATA_CONTENT_TIME));
        });
    }

    /**
     * Test read full data.
     */
    @Test
    public void testReadFull() throws IOException {
        File file = TMP_FOLDER.newFile();
        StringBuffer sb = new StringBuffer();
        String testData1 = IntStream.range(0, 100)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(System.lineSeparator()));
        sb.append(testData1);
        sb.append(System.lineSeparator());
        TestUtils.write(file.getAbsolutePath(), sb);
        sb.setLength(0);

        JobProfile jobProfile = new JobProfile();
        jobProfile.set(JobConstants.JOB_INSTANCE_ID, "1");
        jobProfile.set(JobConstants.JOB_DIR_FILTER_PATTERNS, file.getAbsolutePath());
        jobProfile.set(JobConstants.JOB_TASK_BEGIN_WAIT_SECONDS, String.valueOf(0));
        jobProfile.set(JobConstants.JOB_FILE_CONTENT_COLLECT_TYPE, DataCollectType.FULL);
        jobProfile.set(JOB_FILE_META_ENV_LIST, KUBERNETES);
        // mock data
        final MockSink sink = mockTextTask(jobProfile);
        await().atMost(10, TimeUnit.SECONDS).until(() -> sink.getResult().size() == 100);
        await().atMost(10, TimeUnit.SECONDS).until(() -> MonitorTextFile.getInstance().monitorNum() == 1);
        String testData = IntStream.range(100, 300)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(System.lineSeparator()));
        sb.append(testData);
        sb.append(System.lineSeparator());
        TestUtils.write(file.getAbsolutePath(), sb);

        await().atMost(10, TimeUnit.SECONDS).until(() -> sink.getResult().size() == 300);
        String collectData = sink.getResult().stream().map(message -> {
            String content = new String(message.getBody(), Charset.forName("UTF-8"));
            Map<String, String> logJson = GSON.fromJson(content, Map.class);
            return logJson.get(MetadataConstants.DATA_CONTENT);
        }).collect(Collectors.joining(System.lineSeparator()));
        Assert.assertEquals(testData1 + System.lineSeparator() + testData, collectData);
    }

    /**
     * Test read increment data.
     */
    @Test
    public void testReadIncrement() throws IOException {
        File file = TMP_FOLDER.newFile();
        StringBuffer sb = new StringBuffer();
        sb.append(IntStream.range(0, 100)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(System.lineSeparator())));
        sb.append(System.lineSeparator());
        TestUtils.write(file.getAbsolutePath(), sb);
        sb.setLength(0);

        JobProfile jobProfile = new JobProfile();
        jobProfile.set(JobConstants.JOB_INSTANCE_ID, "1");
        jobProfile.set(JobConstants.JOB_DIR_FILTER_PATTERNS, file.getAbsolutePath());
        jobProfile.set(JobConstants.JOB_FILE_CONTENT_COLLECT_TYPE, DataCollectType.INCREMENT);
        jobProfile.set(JobConstants.JOB_TASK_BEGIN_WAIT_SECONDS, String.valueOf(0));
        jobProfile.set(JOB_FILE_META_ENV_LIST, KUBERNETES);

        // mock data
        final MockSink sink = mockTextTask(jobProfile);
        await().atMost(10, TimeUnit.SECONDS).until(() -> MonitorTextFile.getInstance().monitorNum() == 1);
        String testData = IntStream.range(100, 300)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(System.lineSeparator()));
        sb.append(testData);
        sb.append(System.lineSeparator());
        TestUtils.write(file.getAbsolutePath(), sb);

        await().atMost(10, TimeUnit.SECONDS).until(() -> sink.getResult().size() == 200);
        String collectData = sink.getResult().stream().map(message -> {
            String content = new String(message.getBody(), Charset.forName("UTF-8"));
            Map<String, String> logJson = GSON.fromJson(content, Map.class);
            return logJson.get(MetadataConstants.DATA_CONTENT);
        }).collect(Collectors.joining(System.lineSeparator()));
        Assert.assertEquals(testData, collectData);
    }

    /**
     * Test line end pattern
     */
    @Test
    public void testLineEndPattern() throws IOException {
        File file = TMP_FOLDER.newFile();
        JobProfile jobProfile = new JobProfile();
        jobProfile.set(JobConstants.JOB_INSTANCE_ID, "1");
        jobProfile.set(JobConstants.JOB_DIR_FILTER_PATTERNS, file.getAbsolutePath());
        jobProfile.set(JobConstants.JOB_FILE_CONTENT_COLLECT_TYPE, DataCollectType.FULL);
        jobProfile.set(JobConstants.JOB_TASK_BEGIN_WAIT_SECONDS, String.valueOf(0));
        jobProfile.set(JobConstants.JOB_FILE_LINE_END_PATTERN, "[0-9]");
        jobProfile.set(JOB_FILE_META_ENV_LIST, KUBERNETES);

        // mock data
        final MockSink sink = mockTextTask(jobProfile);
        StringBuffer sb = new StringBuffer();
        // one line multiple match
        sb.append("a1b2c3d5e" + System.lineSeparator());
        // one line no match
        sb.append("fghi" + System.lineSeparator());
        // empty gap
        sb.append("j6789klmn");

        TestUtils.write(file.getAbsolutePath(), sb);
        await().atMost(10, TimeUnit.SECONDS).until(() -> sink.getResult().size() == 8);
        List<String> results = sink.getResult().stream().map(message -> {
            String content = new String(message.getBody(), Charset.forName("UTF-8"));
            Map<String, String> logJson = GSON.fromJson(content, Map.class);
            return logJson.get(MetadataConstants.DATA_CONTENT);
        }).collect(Collectors.toList());
        List<String> excepted = Lists.newArrayList("a", "b", "c", "d",
                "e" + System.lineSeparator() + "fghi" + System.lineSeparator() + "j", "", "", "");
        Assert.assertEquals(excepted, results);
    }

    @Test
    public void testScaleData() throws IOException {
        File file = TMP_FOLDER.newFile();
        StringBuffer sb = new StringBuffer();
        String testData1 = IntStream.range(0, 15000)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(System.lineSeparator()));
        sb.append(testData1);
        sb.append(System.lineSeparator());
        TestUtils.write(file.getAbsolutePath(), sb);
        sb.setLength(0);
        JobProfile jobProfile = new JobProfile();
        jobProfile.set(JobConstants.JOB_INSTANCE_ID, "1");
        jobProfile.set(JobConstants.JOB_DIR_FILTER_PATTERNS, file.getAbsolutePath());
        jobProfile.set(JobConstants.JOB_TASK_BEGIN_WAIT_SECONDS, String.valueOf(0));
        jobProfile.set(JobConstants.JOB_FILE_CONTENT_COLLECT_TYPE, DataCollectType.FULL);
        // mock data
        final MockSink sink = mockTextTask(jobProfile);
        await().atMost(100, TimeUnit.SECONDS).until(() -> sink.getResult().size() == 15000);
    }
}
