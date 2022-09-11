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

package org.apache.inlong.agent.task;

import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.core.AgentBaseTestsHelper;
import org.apache.inlong.agent.core.AgentManager;
import org.apache.inlong.agent.core.task.Task;
import org.apache.inlong.agent.core.task.TaskWrapper;
import org.apache.inlong.agent.message.DefaultMessage;
import org.apache.inlong.agent.plugin.Channel;
import org.apache.inlong.agent.plugin.Message;
import org.apache.inlong.agent.plugin.MessageFilter;
import org.apache.inlong.agent.plugin.Reader;
import org.apache.inlong.agent.plugin.Sink;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

public class TestTaskWrapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestTaskWrapper.class);

    private static AgentManager manager;
    private static Task task;
    private static WriterImpl writer;
    private static ReaderImpl reader;
    private static AgentBaseTestsHelper helper;

    @BeforeClass
    public static void setup() {
        helper = new AgentBaseTestsHelper(TestTaskWrapper.class.getName()).setupAgentHome();
        manager = new AgentManager();
        reader = new ReaderImpl();
        writer = new WriterImpl();
        task = new Task("111", reader, writer, new MockChannel(), JobProfile.parseJsonStr(""));
    }

    @AfterClass
    public static void teardown() throws Exception {
        manager.stop();
        helper.teardownAgentHome();
    }

    @Test
    public void testTaskRunning() throws Exception {
        manager.getTaskManager().submitTask(task);
        String jobId = "111";
        TaskWrapper wrapper = manager.getTaskManager().getTaskWrapper(jobId);
        assert wrapper != null;
        if (!wrapper.isSuccess()) {
            LOGGER.info("waiting for success");
            TimeUnit.MILLISECONDS.sleep(100);
        }
        await().atMost(80, TimeUnit.SECONDS).until(()
                -> reader.getCount() == writer.getWriterCount() + 1);
        Assert.assertEquals("reader is not equals to writer",
                reader.getCount(), writer.getWriterCount() + 1);
    }

    public static class MockChannel implements Channel {

        private final LinkedBlockingQueue<Message> queue = new LinkedBlockingQueue<>();

        @Override
        public void push(Message message) {
            queue.offer(message);
        }

        @Override
        public boolean push(Message message, long timeout, TimeUnit unit) {
            return queue.offer(message);
        }

        @Override
        public Message pull(long timeout, TimeUnit unit) {
            return queue.poll();
        }

        @Override
        public void init(JobProfile jobConf) {

        }

        @Override
        public void destroy() {
            queue.clear();
        }
    }

    private static class ReaderImpl implements Reader {

        private int count = 0;

        @Override
        public Message read() {
            count += 1;
            return new DefaultMessage("".getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public boolean isFinished() {
            return count > 10;
        }

        @Override
        public String getReadSource() {
            return null;
        }

        @Override
        public void setReadTimeout(long mill) {

        }

        @Override
        public void setWaitMillisecs(long millis) {

        }

        @Override
        public String getSnapshot() {
            return null;
        }

        @Override
        public void finishRead() {

        }

        @Override
        public boolean isSourceExist() {
            return true;
        }

        public int getCount() {
            return count;
        }

        @Override
        public void init(JobProfile jobConf) {
        }

        @Override
        public void destroy() {

        }
    }

    private static class WriterImpl implements Sink {

        private int writerCount = 0;

        @Override
        public void write(Message message) {
            if (message != null) {
                writerCount += 1;
            }
        }

        @Override
        public void setSourceName(String sourceFileName) {

        }

        @Override
        public MessageFilter initMessageFilter(JobProfile jobConf) {
            return null;
        }

        public int getWriterCount() {
            return writerCount;
        }

        @Override
        public void init(JobProfile jobConf) {

        }

        @Override
        public void destroy() {

        }
    }
}
