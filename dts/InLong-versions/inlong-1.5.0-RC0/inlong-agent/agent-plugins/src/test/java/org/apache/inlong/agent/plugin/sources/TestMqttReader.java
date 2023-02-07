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

import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.constant.CommonConstants;
import org.apache.inlong.agent.message.DefaultMessage;
import org.apache.inlong.agent.metrics.AgentMetricItem;
import org.apache.inlong.agent.metrics.AgentMetricItemSet;
import org.apache.inlong.agent.plugin.Message;
import org.apache.inlong.agent.plugin.sources.reader.MqttReader;
import org.apache.inlong.common.metric.MetricRegister;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.field;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

/**
 * Test cases for {@link MqttReader}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({MqttReader.class, MetricRegister.class})
@PowerMockIgnore({"javax.management.*"})
public class TestMqttReader {

    private MqttReader reader;

    @Mock
    private JobProfile jobProfile;

    @Mock
    private AgentMetricItemSet agentMetricItemSet;

    @Mock
    private AgentMetricItem agentMetricItem;

    @Mock
    private MqttClient mqttClient;

    @Mock
    private LinkedBlockingQueue<DefaultMessage> queue;

    @Mock
    private DefaultMessage message;

    private AtomicLong atomicLong;

    private AtomicLong atomicCountLong;

    private String topic;

    private static final String INSTANCE_ID = "instanceId";

    @Before
    public void setUp() throws Exception {
        final String serverURI = "tcp://broker.hivemq.com:1883";
        final String username = "test";
        final String password = "test";
        final String qos = "0";
        final String clientIdPrefix = "mqtt_client";
        final String groupId = "group01";
        final String streamId = "stream01";

        atomicLong = new AtomicLong(0L);
        atomicCountLong = new AtomicLong(0L);

        topic = "testtopic/mqtt/p1/ebr/delivered";

        when(jobProfile.get(eq(CommonConstants.PROXY_INLONG_GROUP_ID), anyString())).thenReturn(groupId);
        when(jobProfile.get(eq(CommonConstants.PROXY_INLONG_STREAM_ID), anyString())).thenReturn(streamId);
        when(jobProfile.get(eq(MqttReader.JOB_MQTT_USERNAME))).thenReturn(username);
        when(jobProfile.get(eq(MqttReader.JOB_MQTT_PASSWORD))).thenReturn(password);
        when(jobProfile.get(eq(MqttReader.JOB_MQTT_SERVER_URI))).thenReturn(serverURI);
        when(jobProfile.get(eq(MqttReader.JOB_MQTT_QOS))).thenReturn(qos);
        when(jobProfile.get(eq(MqttReader.JOB_MQTT_CLIENT_ID_PREFIX))).thenReturn(clientIdPrefix);
        when(jobProfile.getInstanceId()).thenReturn(INSTANCE_ID);
        when(jobProfile.getInt(eq(MqttReader.JOB_MQTT_QUEUE_SIZE), eq(1000))).thenReturn(1000);

        // mock MqttClient
        whenNew(MqttClient.class).withArguments(anyString(), anyString(), any(MemoryPersistence.class))
                .thenReturn(mqttClient);

        // mock queue
        whenNew(LinkedBlockingQueue.class).withArguments(anyInt()).thenReturn(queue);
        when(queue.poll()).thenReturn(message);

        // mock metrics
        whenNew(AgentMetricItemSet.class).withArguments(anyString()).thenReturn(agentMetricItemSet);
        when(agentMetricItemSet.findMetricItem(any())).thenReturn(agentMetricItem);
        field(AgentMetricItem.class, "pluginReadCount").set(agentMetricItem, atomicLong);
        field(AgentMetricItem.class, "pluginReadSuccessCount").set(agentMetricItem, atomicCountLong);

        // init method
        mockStatic(MetricRegister.class);
        (reader = new MqttReader(topic)).init(jobProfile);
    }

    /**
     * Test cases for {@link MqttReader#read()}.
     */
    @Test
    public void testRead() {
        Message actual = reader.read();
        assertEquals(message, actual);
    }

    /**
     * Test cases for {@link MqttReader#destroy()}.
     */
    @Test
    public void testDestroy() throws Exception {
        assertFalse(reader.isFinished());
        reader.destroy();
        verify(mqttClient).disconnect();
    }

    /**
     * Test cases for {@link MqttReader#finishRead()}.
     */
    @Test
    public void testFinishRead() {
        assertFalse(reader.isFinished());
        reader.finishRead();
        assertTrue(reader.isFinished());
    }

    /**
     * Test cases for {@link MqttReader#isSourceExist()}.
     */
    @Test
    public void testIsSourceExist() {
        assertTrue(reader.isSourceExist());
    }

    /**
     * Test cases for {@link MqttReader#getSnapshot()}.
     */
    @Test
    public void testGetSnapshot() {
        assertEquals(StringUtils.EMPTY, reader.getSnapshot());
    }

    /**
     * Test cases for {@link MqttReader#getReadSource()}.
     */
    @Test
    public void testGetReadSource() {
        assertEquals(INSTANCE_ID, reader.getReadSource());
    }

}
