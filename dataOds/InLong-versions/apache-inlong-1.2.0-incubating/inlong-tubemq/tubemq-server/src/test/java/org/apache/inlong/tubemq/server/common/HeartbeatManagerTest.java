/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.server.common;

import org.apache.inlong.tubemq.server.common.heartbeat.HeartbeatManager;
import org.apache.inlong.tubemq.server.common.heartbeat.TimeoutInfo;
import org.apache.inlong.tubemq.server.common.heartbeat.TimeoutListener;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeartbeatManagerTest {
    private static final Logger logger = LoggerFactory.getLogger(HeartbeatManager.class);
    private static HeartbeatManager heartbeatManager;

    @BeforeClass
    public static void setup() {
        heartbeatManager = new HeartbeatManager();
    }

    @AfterClass
    public static void tearDown() {
        heartbeatManager.clearAllHeartbeat();
    }

    @Test
    public void testBrokerTimeout() {
        heartbeatManager.regBrokerCheckBusiness(1000,
                new TimeoutListener() {
                    @Override
                    public void onTimeout(final String nodeId, TimeoutInfo nodeInfo) throws Exception {
                        logger.info(new StringBuilder(512).append("[Broker Timeout] ")
                                .append(nodeId).toString());
                    }
                });
        heartbeatManager.regBrokerNode("node1", String.valueOf(System.currentTimeMillis()));
        Assert.assertTrue(
                heartbeatManager.getBrokerRegMap().get("node1").getTimeoutTime() > System.currentTimeMillis());
    }

    @Test
    public void testConsumerTimeout() {
        heartbeatManager.regConsumerCheckBusiness(1000,
                new TimeoutListener() {
                    @Override
                    public void onTimeout(final String nodeId, TimeoutInfo nodeInfo) throws Exception {
                        logger.info(new StringBuilder(512).append("[Broker Timeout] ")
                                .append(nodeId).toString());
                    }
                });
        heartbeatManager.regConsumerNode("node1");
        Assert.assertTrue(heartbeatManager.getConsumerRegMap().get("node1").getTimeoutTime()
                > System.currentTimeMillis());
    }

    @Test
    public void testProducerTimeout() {
        heartbeatManager.regProducerCheckBusiness(1000,
                new TimeoutListener() {
                    @Override
                    public void onTimeout(final String nodeId, TimeoutInfo nodeInfo) throws Exception {
                        logger.info(new StringBuilder(512).append("[Broker Timeout] ")
                                .append(nodeId).toString());
                    }
                });
        heartbeatManager.regProducerNode("node1");
        Assert.assertTrue(heartbeatManager.getProducerRegMap().get("node1").getTimeoutTime()
                > System.currentTimeMillis());
    }
}