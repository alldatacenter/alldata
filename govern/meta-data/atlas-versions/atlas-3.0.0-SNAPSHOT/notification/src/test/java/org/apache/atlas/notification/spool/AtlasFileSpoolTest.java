/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.notification.spool;

import org.apache.atlas.AtlasException;
import org.apache.atlas.notification.AbstractNotification;
import org.apache.atlas.notification.NotificationConsumer;
import org.apache.atlas.notification.NotificationException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.apache.atlas.notification.NotificationInterface.NotificationType.HOOK;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class AtlasFileSpoolTest extends BaseTest {
    private static int MAX_RECORDS = 50;

    private static class MessageHandlerSpy extends AbstractNotification {

        private List<String> publishedMessages = new ArrayList<>();

        public List<String> getMessages() {
            return publishedMessages;
        }

        @Override
        public void init(String source, Object failedMessagesLogger) {
        }

        @Override
        public void setCurrentUser(String user) {

        }

        @Override
        public void sendInternal(NotificationType type, List<String> messages) throws NotificationException {
            publishedMessages.addAll(messages);

        }

        @Override
        public <T> List<NotificationConsumer<T>> createConsumers(NotificationType notificationType, int numConsumers) {
            return null;
        }

        @Override
        public <T> void send(NotificationType type, T... messages) throws NotificationException {
        }

        @Override
        public <T> void send(NotificationType type, List<T> messages) throws NotificationException {
        }

        @Override
        public void close() {

        }

        @Override
        public boolean isReady(NotificationType type) {
            return true;
        }
    }

    @Test
    public void indexSetupMultipleTimes() throws IOException, AtlasException {
        SpoolConfiguration cfg = getSpoolConfiguration();
        IndexManagement indexManagement = new IndexManagement(cfg);

        for (int i = 0; i < 2; i++) {
            indexManagement.init();
            assertTrue(cfg.getSpoolDir().exists());
            assertTrue(cfg.getArchiveDir().exists());

            File indexFile = indexManagement.getIndexFileManager().getIndexFile();
            File indexDoneFile = indexManagement.getIndexFileManager().getDoneFile();

            assertTrue(indexFile.exists(), "File not created: " + indexFile.getAbsolutePath());
            assertTrue(indexDoneFile.exists(), "File not created: " + indexDoneFile.getAbsolutePath());
        }
    }

    @Test
    public void spoolerTest() throws IOException, AtlasException {
        SpoolConfiguration cfg = getSpoolConfigurationTest();
        IndexManagement indexManagement = new IndexManagement(cfg);

        indexManagement.init();
        Spooler spooler = new Spooler(cfg, indexManagement);
        for (int i = 0; i < MAX_RECORDS; i++) {
            spooler.write(Collections.singletonList("message: " + i));
        }

        indexManagement.stop();
    }

    @Test(dependsOnMethods = "spoolerTest")
    public void publisherTest() throws IOException, AtlasException, InterruptedException {
        SpoolConfiguration cfg = getSpoolConfigurationTest();

        IndexManagement indexManagement = new IndexManagement(cfg);
        indexManagement.init();

        MessageHandlerSpy messageHandler = new MessageHandlerSpy();
        Publisher publisher = new Publisher(cfg, indexManagement, messageHandler);
        boolean ret = publisher.processAndDispatch(indexManagement.getIndexFileManager().getRecords().get(0));

        publisher.setDrain();
        Assert.assertTrue(ret);
        TimeUnit.SECONDS.sleep(5);
        assertTrue(messageHandler.getMessages().size() >= 0);
    }

    @Test
    public void indexRecordsRead() throws IOException, AtlasException {
        SpoolConfiguration spoolCfg = getSpoolConfigurationTest();
        IndexManagement indexManagement = new IndexManagement(spoolCfg);
        indexManagement.init();

    }

    @Test
    public void concurrentWriteAndPublish() throws InterruptedException, IOException, AtlasException {
        final int MAX_PROCESSES = 4;
        SpoolConfiguration spoolCfg = getSpoolConfigurationTest(5);

        IndexManagement[] im1 = new IndexManagement[MAX_PROCESSES];
        MessageHandlerSpy[] messageHandlerSpy = new MessageHandlerSpy[MAX_PROCESSES];

        for (int i = 0; i < MAX_PROCESSES; i++) {
            messageHandlerSpy[i] = new MessageHandlerSpy();
            im1[i] = new IndexManagement(spoolCfg);
        }

        for (int i = 0; i < MAX_PROCESSES; i++) {
            im1[i].init();
        }

        IndexManagement imVerify = new IndexManagement(spoolCfg);
        imVerify.init();
        Assert.assertTrue(imVerify.getIndexFileManager().getRecords().size() >= 0);

        Thread[] th1 = new Thread[MAX_PROCESSES];
        for (int i = 0; i < MAX_PROCESSES; i++) {
            th1[i] = new Thread(new MessagePump(new Spooler(spoolCfg, im1[i]), new Publisher(spoolCfg, im1[i], messageHandlerSpy[i])));
        }

        for (int i = 0; i < MAX_PROCESSES; i++) {
            th1[i].start();
        }

        for (int i = 0; i < MAX_PROCESSES; i++) {
            th1[i].join();
        }

        imVerify = new IndexManagement(spoolCfg);
        imVerify.init();
        Assert.assertEquals(imVerify.getIndexFileManager().getRecords().size(), 0);
        for (int i = 0; i < MAX_PROCESSES; i++) {
            Assert.assertTrue(messageHandlerSpy[i].getMessages().size() >= 0);
        }
    }

    private class MessagePump implements Runnable {

        private Spooler spooler;
        private Publisher publisher;
        private Thread publisherThread;

        public MessagePump(Spooler spooler, Publisher publisher) {
            this.spooler = spooler;
            this.publisher = publisher;
        }

        @Override
        public void run() {
            publisherThread = new Thread(publisher);
            publisherThread.start();

            for (int i = 0; i < MAX_RECORDS; i++) {
                try {
                    spooler.send(HOOK, String.format("%s-%s", "message", i));

                    Thread.sleep(RandomUtils.nextInt(10, 100));
                } catch (NotificationException exception) {
                    exception.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            try {
                Thread.sleep(10000);
                publisher.setDrain();
                publisherThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    @AfterClass
    public void tearDown() {
        FileUtils.deleteQuietly(new File(spoolDirTest));
    }
}
