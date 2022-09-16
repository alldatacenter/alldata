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

import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.inlong.common.metric.MetricRegister;
import org.apache.inlong.dataproxy.config.loader.TestContextIdTopicConfigLoader;
import org.apache.inlong.dataproxy.utils.MockUtils;
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

/**
 * TestPulsarFederationSink
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest({MetricRegister.class})
public class TestPulsarFederationSink {

    public static final Logger LOG = LoggerFactory.getLogger(TestContextIdTopicConfigLoader.class);
    public static Context context;
    public static Context sinkContext;
    public static PulsarFederationSink sinkObj;

    /**
     * setup
     */
    @BeforeClass
    public static void setUp() {
        Map<String, String> result = new ConcurrentHashMap<>();
        try (InputStream inStream = TestPulsarFederationSink.class.getClassLoader().getResource(
                "dataproxy-pulsar.conf")
                .openStream()) {
            Properties props = new Properties();
            props.load(inStream);
            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                result.put((String) entry.getKey(), (String) entry.getValue());
            }
            context = new Context(result);
            sinkContext = new Context(context.getSubProperties("proxy_inlong5th_sz.sinks.pulsar-sink-more1."));
        } catch (Exception e) {
            LOG.error("fail to load properties, file ={}, and e= {}", "dataproxy-pulsar.conf", e);
        }
        sinkObj = new PulsarFederationSink();
        sinkObj.configure(sinkContext);
    }

    /**
     * testResult
     */
    @Test
    public void testResult() throws Exception {
        MockUtils.mockMetricRegister();
        // mock
        Channel channel = MockUtils.mockChannel();
        sinkObj.setChannel(channel);
        sinkObj.process();
    }

}
