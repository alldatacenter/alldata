/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.registry.plugin;

import io.datavines.common.utils.CommonPropertyUtils;
import io.datavines.registry.api.Event;
import io.datavines.registry.api.SubscribeListener;
import org.apache.curator.test.TestingServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class ZooKeeperRegistryTest {
    private static final Logger logger = LoggerFactory.getLogger(ZooKeeperRegistryTest.class);

    TestingServer server;

    ZooKeeperRegistry registry;

    @Before
    public void before() throws Exception {
        server = new TestingServer(true);
        Properties properties = CommonPropertyUtils.getProperties();
        registry = new ZooKeeperRegistry();
        properties.setProperty("server.port", "5600");
        registry.init(properties);
        registry.put("/sub", "", false);
    }

    @After
    public void after() throws IOException {
        registry.close();
        server.close();
    }

    @Test
    public void persistTest() {
        registry.put("/nodes/m1", "127.0.0.1:2181", false);
        registry.put("/nodes/m2", "127.0.0.1:3306", false);
        registry.put("/nodes/m1/m2", "127.0.0.1:2181", false);
        registry.put("/nodes/m2/m1", "127.0.0.1:3306", false);
        Assert.assertEquals(Arrays.asList("m2", "m1"), registry.children("/nodes"));
        Assert.assertTrue(registry.exists("/nodes/m1"));
        //registry.delete("/nodes/m2");
        Assert.assertFalse(registry.exists("/nodes/m2"));
    }

    @Test
    public void activeServerTest() {
        registry.getActiveServerList();
    }

    @Test
    public void subscribeTest() {
        registry.subscribe("/sub", new TestListener());
        registry.unSubscribe("/sub");
    }

    static class TestListener implements SubscribeListener {
        @Override
        public void notify(Event event) {
            logger.info("I'm test listener");
        }
    }

    @Test
    public void lockTest() throws InterruptedException {
        CountDownLatch preCountDownLatch = new CountDownLatch(1);
        CountDownLatch allCountDownLatch = new CountDownLatch(2);
        List<String> testData = new ArrayList<>();
        new Thread(() -> {
            registry.acquire("/lock", 10L);
            preCountDownLatch.countDown();
            logger.info(Thread.currentThread().getName() + " :I got the lock, but I don't want to work. I want to rest for a while");
            try {
                Thread.sleep(1000);
                logger.info(Thread.currentThread().getName() + " :I'm going to start working");
                testData.add("thread1");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                logger.info(Thread.currentThread().getName() + " :I have finished my work, now I release the lock");
                registry.release("/lock");
                allCountDownLatch.countDown();
            }
        }).start();
        preCountDownLatch.await();
        new Thread(() -> {
            try {
                logger.info(Thread.currentThread().getName() + " :I am trying to acquire the lock");
                registry.release("/lock");
                logger.info(Thread.currentThread().getName() + " :I got the lock and I started working");

                testData.add("thread2");
            } finally {
                registry.release("/lock");
                allCountDownLatch.countDown();
            }

        }).start();
        allCountDownLatch.await();
        Assert.assertEquals(testData, Arrays.asList("thread1", "thread2"));

    }

    @Test
    public void childrenTest() {
        registry.children("/nodes");
    }


    @Test
    public void getTest() {
        String key = "/datavines/servers";
        final List<String> children = registry.children(key);
        for (String child : children) {
            registry.get(key + "/" + child);
        }
    }

}
