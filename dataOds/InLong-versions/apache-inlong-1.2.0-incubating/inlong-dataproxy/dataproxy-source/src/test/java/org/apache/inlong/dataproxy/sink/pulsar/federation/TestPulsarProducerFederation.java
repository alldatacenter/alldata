/*
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

package org.apache.inlong.dataproxy.sink.pulsar.federation;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.inlong.common.metric.MetricRegister;
import org.apache.inlong.dataproxy.utils.MockUtils;
import org.apache.pulsar.client.api.ClientBuilder;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.ProducerBuilder;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.TypedMessageBuilder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertTrue;

/**
 * TestPulsarProducerFederation
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest({PulsarClient.class, ClientBuilder.class, MessageId.class,
        Producer.class, ProducerBuilder.class, TypedMessageBuilder.class, MetricRegister.class})
public class TestPulsarProducerFederation {

    public static final Logger LOG = LoggerFactory.getLogger(TestPulsarProducerFederation.class);
    public static Context context;
    public static Context sinkContext;

    /**
     * setup
     */
    @BeforeClass
    public static void setUp() {
        Map<String, String> result = new ConcurrentHashMap<>();
        try (InputStream inStream = TestPulsarFederationSink.class.getClassLoader().getResource(
                "dataproxy-pulsar.conf").openStream()) {
            MockUtils.mockMetricRegister();
            Properties props = new Properties();
            props.load(inStream);
            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                result.put((String) entry.getKey(), (String) entry.getValue());
            }
            context = new Context(result);
            sinkContext = new Context(context.getSubProperties("proxy_inlong5th_sz.sinks.pulsar-sink-more1."));
            MockUtils.mockPulsarClient();
        } catch (Exception e) {
            LOG.error("fail to load properties, file ={}, and e= {}", "dataproxy-pulsar.conf", e);
        }
    }

    /**
     * testResult
     */
    @Test
    public void testResult() throws Exception {
        String workerName = "workerName";
        PulsarFederationSinkContext pulsarContext = new PulsarFederationSinkContext(MockUtils.SINK_ID, sinkContext);
        PulsarProducerFederation federation = new PulsarProducerFederation(workerName, pulsarContext);
        federation.start();
        Event event = MockUtils.mockEvent();
        boolean result = federation.send(event);
        assertTrue(result);
    }

}
